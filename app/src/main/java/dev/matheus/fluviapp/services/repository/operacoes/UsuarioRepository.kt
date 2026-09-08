package dev.matheus.fluviapp.services.repository.operacoes

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import dev.matheus.fluviapp.di.module.SyncScope
import dev.matheus.fluviapp.domain.operacoes.Usuario
import dev.matheus.fluviapp.services.repository.firebase.CodecFirestore
import dev.matheus.fluviapp.services.repository.firebase.ColecaoFirestore
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto
import dev.matheus.fluviapp.services.repository.firebase.FonteSnapshots
import dev.matheus.fluviapp.services.repository.firebase.documents.paraMapa
import dev.matheus.fluviapp.services.repository.firebase.documents.toUsuario
import dev.matheus.fluviapp.telemetry.RegistroCadastro
import dev.matheus.fluviapp.telemetry.RegistroSincronizacao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Porta do repositório de **perfis de acesso** (`users/{uid}`) — quem já entrou, e em que estado
 * ([ADR-0032] D6).
 *
 * Ela nasce porque a seção Usuários deixou de ser somente-leitura (supera o ADR-0021 D2). Até aqui o app
 * lia `users/{uid}` num lugar só, o **login**, e por um caminho próprio: o
 * `AutenticacaoRepository.perfilAutenticado()` lê o perfil de *quem está entrando* e junta o segundo salto
 * (`funcionarios/{id}`). São perguntas diferentes — *quem sou eu* e *quem tem acesso* —, e é por isso que
 * são duas portas em vez de um método a mais na primeira.
 *
 * **Não existe criar nem deletar aqui.** Perfil nasce no primeiro acesso, pela mão do próprio dono
 * (ADR-0015 §2.1), e não se apaga — `allow delete: if false`. O que a plataforma faz é **governar o
 * estado** do que existe.
 */
interface UsuarioRepository {
    fun sincronizar()

    /** Todos os perfis — é a lista da seção Usuários. Leitura é de todo autenticado; a escrita, do `ADM`. */
    suspend fun obterTodos(): List<Usuario>

    /**
     * Liga e desliga o acesso, e define (ou tira) o prazo — as duas chaves que a regra abre ao `ADM`.
     *
     * `expiraEm` nulo vira **zero** no documento, porque é o que a regra sabe comparar (Q1). Os dois vão
     * juntos, numa escrita só: são as duas metades da mesma pergunta (*este acesso vale?*), e separá-las
     * criaria um instante em que o documento diz uma coisa e a tela outra.
     */
    suspend fun definirAcesso(id: String, ativo: Boolean, expiraEm: Long?)

    companion object {
        const val COLLECTION_USERS = "users"

        const val CAMPO_ATIVO = "ativo"
        const val CAMPO_EXPIRA_EM = "expiraEm"
    }
}

/** O codec do perfil de acesso (ADR-0019 D2) — o id **é** o `uid` do Auth. */
private object UsuarioCodec : CodecFirestore<Usuario> {
    override val colecao = UsuarioRepository.COLLECTION_USERS
    override val entidade = "usuario"
    override fun deDocumento(bruto: DocumentoBruto) = bruto.toUsuario()
    override fun paraMapa(modelo: Usuario) = modelo.paraMapa()
    override fun id(modelo: Usuario) = modelo.id
    override fun comId(modelo: Usuario, id: String) = modelo.copy(id = id)
}

@Singleton
class UsuarioFirestoreRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    registroCadastro: RegistroCadastro,
    @SyncScope syncScope: CoroutineScope,
    registroSincronizacao: RegistroSincronizacao,
    fonteSnapshots: FonteSnapshots,
) : UsuarioRepository {

    private val colecao = ColecaoFirestore(
        codec = UsuarioCodec,
        firestore = firestore,
        fonteSnapshots = fonteSnapshots,
        syncScope = syncScope,
        registroCadastro = registroCadastro,
        registroSincronizacao = registroSincronizacao,
    )

    override fun sincronizar() = colecao.sincronizar()

    override suspend fun obterTodos(): List<Usuario> = colecao.obterTodos()

    /**
     * **`update` de dois campos, e não `salvar`**, que é o `set` do documento inteiro.
     *
     * A razão é a regra: ela admite ao `ADM` apenas uma **lista fechada de chaves** (Q1), e a compara pelo
     * `diff` do que a escrita muda. Um `set` a partir de uma cópia local atrasada — alguém trocou o
     * `username` depois do último snapshot — mudaria também esse campo, e a escrita inteira seria negada.
     * Fail-closed, mas por um motivo que quem desativou não tem como adivinhar.
     *
     * Aguardada, ao contrário dos cadastros: a tela recarrega a lista em seguida, e ler antes de a escrita
     * chegar mostraria a situação anterior como se o gesto não tivesse acontecido.
     */
    override suspend fun definirAcesso(id: String, ativo: Boolean, expiraEm: Long?) {
        try {
            firestore.collection(UsuarioRepository.COLLECTION_USERS).document(id)
                .update(
                    mapOf(
                        UsuarioRepository.CAMPO_ATIVO to ativo,
                        UsuarioRepository.CAMPO_EXPIRA_EM to (expiraEm ?: 0L),
                    )
                )
                .await()
        } catch (e: Exception) {
            // A falha não é silenciosa ([ADR-0032] D4) e **não** é tratada como pendência de sync: gestão
            // de acesso que "vai chegar depois" é pior do que gestão de acesso que falhou — quem desativou
            // precisa saber que não desativou.
            Log.e(TAG, "definirAcesso($id): ${e.message}", e)
            throw e
        }
    }

    private companion object {
        const val TAG = "usuarioRepository"
    }
}
