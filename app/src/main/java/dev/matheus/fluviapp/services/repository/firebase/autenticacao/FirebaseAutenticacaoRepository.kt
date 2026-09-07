package dev.matheus.fluviapp.services.repository.firebase.autenticacao

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import dev.matheus.fluviapp.domain.operacoes.Usuario
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto
import dev.matheus.fluviapp.services.repository.firebase.documents.paraMapa
import dev.matheus.fluviapp.services.repository.firebase.documents.toPerfilAutenticado
import dev.matheus.fluviapp.services.repository.operacoes.FuncionarioRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Impl Firebase da [AutenticacaoRepository]. Único ponto que toca `Task`/exceções do Firebase;
 * converte para [ResultadoAutenticacao] na borda (via [motivoDe]).
 */
class FirebaseAutenticacaoRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : AutenticacaoRepository {

    override suspend fun autenticar(email: String, senha: String): ResultadoAutenticacao = try {
        firebaseAuth.signInWithEmailAndPassword(email, senha).await()
        ResultadoAutenticacao.Sucesso
    } catch (e: Exception) {
        ResultadoAutenticacao.Falha(motivoDe(e))
    }

    override suspend fun recuperarSenha(email: String): ResultadoAutenticacao = try {
        firebaseAuth.sendPasswordResetEmail(email).await()
        ResultadoAutenticacao.Sucesso
    } catch (e: Exception) {
        ResultadoAutenticacao.Falha(motivoDe(e))
    }

    override suspend fun alterarSenha(novaSenha: String): ResultadoAutenticacao = try {
        val user = firebaseAuth.currentUser ?: return ResultadoAutenticacao.Falha(MotivoFalhaAuth.USUARIO_INEXISTENTE)
        user.updatePassword(novaSenha).await()
        ResultadoAutenticacao.Sucesso
    } catch (e: Exception) {
        Log.e(TAG, "alterarSenha falhou: ${e.javaClass.simpleName}: ${e.message}", e)
        ResultadoAutenticacao.Falha(motivoDe(e))
    }

    /**
     * Resolve os dois contextos no login (ADR-0015 §8.3): lê `users/{uid}` e, se houver elo, segue para
     * `funcionarios/{funcionarioId}` — os mesmos dois saltos que as regras do servidor fazem. Sem elo
     * (papel puro de plataforma), cargo e nome ficam vazios, o que é estado válido: quem decide ali é o
     * papel. Falha na 2ª leitura NÃO derruba o login — degrada para "sem cargo", que é fail-closed.
     */
    override suspend fun perfilAutenticado(): ResultadoPerfil {
        val user = firebaseAuth.currentUser ?: return ResultadoPerfil.Ausente

        // Source.SERVER: sem ele, `get()` cai no cache quando falta rede, e cache vazio (app recém
        // instalado) devolve "documento inexistente" — indistinguível de "esta pessoa não tem perfil".
        // Aqui a falha de leitura vira Indisponivel, e quem chama decide o que dizer.
        val documento = try {
            firestore.collection(COLLECTION_USERS).document(user.uid)
                .get(Source.SERVER).await()
        } catch (e: Exception) {
            Log.e(TAG, "perfilAutenticado: users/${user.uid} ilegível: ${e.message}", e)
            return ResultadoPerfil.Indisponivel
        }

        // Map, e não `toObject` (ADR-0025): estas eram as **duas últimas leituras por reflexão** do app,
        // e saíram em 2026-09-07 junto com o R8. Reflexão é o que o encolhedor não enxerga — ele renomeia
        // o campo e o Firestore, que casa por nome, passa a devolver vazio sem erro nenhum. A alternativa
        // seria um `-keep` protegendo as duas data classes; preferiu-se **não ter o que proteger**.
        val dados = documento.data ?: return ResultadoPerfil.Ausente
        val perfil = DocumentoBruto(documento.id, dados)

        val funcionario = perfil.texto("funcionarioId").takeIf { it.isNotBlank() }?.let { id ->
            try {
                val bruto = firestore.collection(FuncionarioRepository.COLLECTION_FUNCIONARIOS)
                    .document(id).get(Source.SERVER).await()
                bruto.data?.let { DocumentoBruto(bruto.id, it) }
            } catch (e: Exception) {
                Log.e(TAG, "perfilAutenticado: funcionario $id ilegível: ${e.message}", e)
                null
            }
        }

        return ResultadoPerfil.Encontrado(perfil.toPerfilAutenticado(user.uid, funcionario))
    }

    /**
     * Escrita **aguardada**, ao contrário do resto (que é fire-and-forget offline-first): o primeiro
     * acesso manda a pessoa logar de novo em seguida, e um perfil que ainda não chegou ao servidor
     * faria o login seguinte parecer um novo primeiro acesso.
     */
    override suspend fun criarPerfil(email: String, username: String, papel: String, funcionarioId: String) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        firestore.collection(COLLECTION_USERS).document(uid)
            .set(
                Usuario(
                    id = uid,
                    email = email,
                    username = username,
                    papel = papel,
                    funcionarioId = funcionarioId,
                ).paraMapa()
            ).await()
    }

    override fun sair() {
        firebaseAuth.signOut()
    }

    companion object {
        private const val TAG = "FirebaseAuthRepo"

        /**
         * A coleção do perfil de sistema. Ela morava no `UsuarioRepository`, que era metade cache do Room
         * e metade esta constante; com o cache indo para o DataStore (ADR-0017 D4), o repositório deixou
         * de ter conteúdo e o nome da coleção veio para o **único lugar que a lê** — a mesma forma do
         * `FuncionarioRepository.COLLECTION_FUNCIONARIOS`.
         */
        const val COLLECTION_USERS = "users"
    }
}

/** Tradução da exceção do Firebase (borda) para o motivo de domínio. */
internal fun motivoDe(erro: Throwable): MotivoFalhaAuth = when (erro) {
    is FirebaseAuthInvalidCredentialsException -> MotivoFalhaAuth.CREDENCIAL_INVALIDA
    is FirebaseAuthInvalidUserException -> MotivoFalhaAuth.USUARIO_INEXISTENTE
    else -> MotivoFalhaAuth.DESCONHECIDO
}