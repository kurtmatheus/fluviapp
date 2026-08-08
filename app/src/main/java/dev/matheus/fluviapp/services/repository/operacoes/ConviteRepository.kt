package dev.matheus.fluviapp.services.repository.operacoes

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import dev.matheus.fluviapp.di.module.SyncScope
import dev.matheus.fluviapp.domain.operacoes.Convite
import dev.matheus.fluviapp.services.repository.firebase.CodecFirestore
import dev.matheus.fluviapp.services.repository.firebase.ColecaoFirestore
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto
import dev.matheus.fluviapp.services.repository.firebase.FonteSnapshots
import dev.matheus.fluviapp.services.repository.firebase.documents.paraMapa
import dev.matheus.fluviapp.services.repository.firebase.documents.toConvite
import dev.matheus.fluviapp.telemetry.RegistroCadastro
import dev.matheus.fluviapp.telemetry.RegistroSincronizacao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Porta do repositório de convites (DIP) — a coleção que a plataforma escreve para dizer *quem pode
 * entrar* (estudo `usuario-e-funcionario.md`).
 */
interface ConviteRepository {
    suspend fun salvar(convite: Convite)
    suspend fun obterTodos(): List<Convite>

    /**
     * Busca **no servidor**, por e-mail — e é a única leitura desta porta que roda **antes** do perfil
     * existir. É o primeiro acesso: a pessoa acabou de autenticar, não tem `users/{uid}`, e é o convite
     * que diz com que papel ela entra.
     */
    suspend fun obterPorEmail(email: String): Convite?

    /** Marca o convite como usado — ele vira registro em vez de sumir. */
    suspend fun marcarComoUsado(email: String)

    companion object {
        const val COLLECTION_CONVITES = "convites"
    }
}

/** O codec do Convite: o pouco que é dele na fronteira (ADR-0019 D2). */
private object ConviteCodec : CodecFirestore<Convite> {
    override val colecao = ConviteRepository.COLLECTION_CONVITES
    override val entidade = "convite"
    override fun deDocumento(bruto: DocumentoBruto) = bruto.toConvite()
    override fun paraMapa(modelo: Convite) = modelo.paraMapa()

    /** O id **é** o e-mail — e é por isso que não existe convite duplicado para a mesma pessoa. */
    override fun id(modelo: Convite) = modelo.email
    override fun comId(modelo: Convite, id: String) = modelo.copy(email = id)
}

@Singleton
class ConviteFirestoreRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    registroCadastro: RegistroCadastro,
    @SyncScope syncScope: CoroutineScope,
    registroSincronizacao: RegistroSincronizacao,
    fonteSnapshots: FonteSnapshots,
) : ConviteRepository {

    private val colecao = ColecaoFirestore(
        codec = ConviteCodec,
        firestore = firestore,
        fonteSnapshots = fonteSnapshots,
        syncScope = syncScope,
        registroCadastro = registroCadastro,
        registroSincronizacao = registroSincronizacao,
    )

    override suspend fun salvar(convite: Convite) {
        colecao.salvar(convite)
    }

    override suspend fun obterTodos(): List<Convite> = colecao.obterTodos()

    /**
     * `get` direto no documento, **sem passar pela coleção em memória**: aqui o listener ainda não subiu
     * — e nem poderia, porque a leitura ampla de convites é do `ADM`, e quem chama isto é alguém que
     * acabou de autenticar e ainda não tem papel nenhum. A regra do servidor libera exatamente este
     * caso: o convite do próprio e-mail.
     */
    override suspend fun obterPorEmail(email: String): Convite? = try {
        val id = email.trim().lowercase()
        firestore.collection(ConviteRepository.COLLECTION_CONVITES).document(id)
            .get().await()
            .takeIf { it.exists() }
            ?.let { DocumentoBruto(id = it.id, dados = it.data.orEmpty()).toConvite() }
    } catch (e: Exception) {
        Log.e(TAG, "obterPorEmail($email): ${e.message}", e)
        null
    }

    override suspend fun marcarComoUsado(email: String) {
        obterPorEmail(email)?.let { colecao.salvar(it.copy(usado = true)) }
    }

    private companion object {
        const val TAG = "conviteRepository"
    }
}