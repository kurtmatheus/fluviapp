package dev.matheus.fluviapp.services.repository.cadastro.viagem

import dev.matheus.fluviapp.database.dao.cadastro.viagem.NavioDao
import dev.matheus.fluviapp.model.viagem.Navio
import dev.matheus.fluviapp.model.viagem.toDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.NavioDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.toNavio
import dev.matheus.fluviapp.di.module.SyncScope
import dev.matheus.fluviapp.services.repository.firebase.sincronizarColecao
import dev.matheus.fluviapp.telemetry.RegistroCadastro
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Impl Firestore da porta [NavioRepository] — espelha no Room (ADR-0003), contrato do molde (ADR-0006). */
@Singleton
class NavioFirestoreRepository @Inject constructor(
    private val dao: NavioDao,
    private val firestore: FirebaseFirestore,
    private val registroCadastro: RegistroCadastro,
    @SyncScope private val syncScope: CoroutineScope,
) : NavioRepository {

    private var syncJob: Job? = null

    override fun sincronizar() {
        if (syncJob?.isActive == true) return
        syncJob = firestore.sincronizarColecao(
            colecao = COLLECTION_NAVIOS,
            tag = TAG,
            scope = syncScope,
            paraModelo = { it.toObject<NavioDocumento>()?.toNavio(it.id) },
            salvarTodos = { dao.salvarTodos(*it.toTypedArray()) },
        )
    }

    override suspend fun obterTodos() = dao.obterTodos().first()

    override suspend fun obterPorId(id: String) = dao.obterPorId(id).first()

    override suspend fun salvar(navio: Navio) {
        val documento = if (navio.id.isBlank()) {
            firestore.collection(COLLECTION_NAVIOS).document()
        } else {
            firestore.collection(COLLECTION_NAVIOS).document(navio.id)
        }
        val comId = navio.copy(id = documento.id)

        // FALHA: Room não gravou — desfecho não recuperável, propaga pro VM tratar.
        try {
            dao.salvar(comId)
        } catch (e: Exception) {
            registroCadastro.falhou(ENTIDADE, e)
            throw RuntimeException("Falha ao salvar navio: ${e.message}", e)
        }

        // Room já tem o dado (otimista). Aguarda o ack do Firestore: SUCESSO se confirmar,
        // WARNING (pendente-sync) se rejeitar/offline — não relança, o dado local reconcilia.
        try {
            documento.set(comId.toDocumento()).await()
            registroCadastro.salvou(ENTIDADE, comId.id)
        } catch (e: Exception) {
            registroCadastro.pendenteDeSync(ENTIDADE, comId.id, e)
        }
    }

    override suspend fun obterPorNome(nome: String) = dao.obterPorNome(nome).first()

    private companion object {
        const val TAG = "navioRepository"
        const val COLLECTION_NAVIOS = "navios"
        const val ENTIDADE = "navio"
    }
}
