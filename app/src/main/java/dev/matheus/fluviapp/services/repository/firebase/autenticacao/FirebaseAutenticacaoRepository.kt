package dev.matheus.fluviapp.services.repository.firebase.autenticacao

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import dev.matheus.fluviapp.model.operacoes.Agencia
import dev.matheus.fluviapp.services.repository.firebase.documents.UsuarioDocumento
import dev.matheus.fluviapp.services.repository.operacoes.UsuarioRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Impl Firebase da [AutenticacaoRepository]. Único ponto que toca `Task`/exceções do Firebase;
 * converte para [ResultadoAutenticacao] na borda (via [motivoDe]). Escrita de perfil é
 * fire-and-forget (offline-first, como as demais).
 */
class FirebaseAutenticacaoRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : AutenticacaoRepository {

    override suspend fun autenticar(email: String, senha: String): ResultadoAutenticacao = try {
        firebaseAuth.signInWithEmailAndPassword(email, senha).await()
        ResultadoAutenticacao.Sucesso(firebaseAuth.currentUser?.isEmailVerified == true)
    } catch (e: Exception) {
        ResultadoAutenticacao.Falha(motivoDe(e))
    }

    override suspend fun cadastrar(email: String, senha: String): ResultadoAutenticacao = try {
        firebaseAuth.createUserWithEmailAndPassword(email, senha).await()
        firebaseAuth.currentUser?.sendEmailVerification()?.await()
        ResultadoAutenticacao.Sucesso(emailVerificado = false)
    } catch (e: Exception) {
        ResultadoAutenticacao.Falha(motivoDe(e))
    }

    override suspend fun reenviarVerificacao(email: String, senha: String): ResultadoAutenticacao = try {
        firebaseAuth.signInWithEmailAndPassword(email, senha).await()
        firebaseAuth.currentUser?.sendEmailVerification()?.await()
        firebaseAuth.signOut()
        ResultadoAutenticacao.Sucesso(emailVerificado = false)
    } catch (e: Exception) {
        ResultadoAutenticacao.Falha(motivoDe(e))
    }

    override suspend fun recuperarSenha(email: String): ResultadoAutenticacao = try {
        firebaseAuth.sendPasswordResetEmail(email).await()
        ResultadoAutenticacao.Sucesso(emailVerificado = false)
    } catch (e: Exception) {
        ResultadoAutenticacao.Falha(motivoDe(e))
    }

    override suspend fun autenticarComGoogle(idToken: String): ResultadoAutenticacao = try {
        firebaseAuth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()

        // 1º login Google → auto-provisiona o perfil; só cria se ausente p/ não sobrescrever cargo.
        firebaseAuth.currentUser?.let { user ->
            val docRef = firestore.collection(UsuarioRepository.COLLECTION_USERS).document(user.uid)
            if (!docRef.get().await().exists()) {
                docRef.set(
                    UsuarioDocumento(
                        email = user.email.orEmpty(),
                        nome = user.displayName ?: user.email.orEmpty(),
                        cargo = CARGO_PADRAO_AUTOCADASTRO,
                    )
                ).await()
            }
        }
        // Conta Google já vem verificada.
        ResultadoAutenticacao.Sucesso(emailVerificado = true)
    } catch (e: Exception) {
        // Log da exceção CRUA: motivoDe() colapsa a maioria em DESCONHECIDO e perde a causa
        // (provider Google desabilitado, DEVELOPER_ERROR/10 por web client id, rede, etc.).
        Log.e(TAG, "autenticarComGoogle falhou: ${e.javaClass.simpleName}: ${e.message}", e)
        ResultadoAutenticacao.Falha(motivoDe(e))
    }

    override suspend fun perfilAutenticado(): PerfilAutenticado? {
        val user = firebaseAuth.currentUser ?: return null
        val doc = firestore.collection(UsuarioRepository.COLLECTION_USERS).document(user.uid)
            .get().await().toObject(UsuarioDocumento::class.java) ?: return null
        return PerfilAutenticado(
            id = user.uid,
            email = doc.email,
            nome = doc.nome,
            cargo = doc.cargo,
            // Mesma normalização da fronteira do documento: sem agência → AUTONOMO (ADR-0015 §2).
            agencia = Agencia.deOuPadrao(doc.agencia).name,
            lotacao = doc.lotacao,
        )
    }

    override suspend fun criarPerfil(email: String, nome: String, cargo: String) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        firestore.collection(UsuarioRepository.COLLECTION_USERS).document(uid)
            .set(UsuarioDocumento(email = email, nome = nome, cargo = cargo))
    }

    override fun sair() {
        firebaseAuth.signOut()
    }

    private companion object {
        private const val TAG = "FirebaseAuthRepo"
        // Menor privilégio p/ auto-cadastro (mesmo default do cadastro por e-mail/senha).
        const val CARGO_PADRAO_AUTOCADASTRO = "AGENTE"
    }
}

/** Tradução da exceção do Firebase (borda) para o motivo de domínio. */
internal fun motivoDe(erro: Throwable): MotivoFalhaAuth = when (erro) {
    is FirebaseAuthInvalidCredentialsException -> MotivoFalhaAuth.CREDENCIAL_INVALIDA
    is FirebaseAuthInvalidUserException -> MotivoFalhaAuth.USUARIO_INEXISTENTE
    is FirebaseAuthUserCollisionException -> MotivoFalhaAuth.EMAIL_JA_CADASTRADO
    else -> MotivoFalhaAuth.DESCONHECIDO
}