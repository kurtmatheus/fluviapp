package dev.matheus.fluviapp.services.repository.cadastro.passagem

import dev.matheus.fluviapp.database.dao.cadastro.passagem.AgenteDao
import dev.matheus.fluviapp.model.cadastro.passagem.Agente
import dev.matheus.fluviapp.model.cadastro.passagem.toDocumento
import dev.matheus.fluviapp.services.repository.cadastro.passagem.AgenteRepository.Companion.COLLECTION_AGENTS
import dev.matheus.fluviapp.services.repository.firebase.documents.AgenteDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.toAgente
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

/** Impl Firestore da porta [AgenteRepository] — espelha no Room (ADR-0003), contrato do molde. */
@Singleton
class AgenteFirestoreRepository @Inject constructor(
    private val dao: AgenteDao,
    private val firestore: FirebaseFirestore,
    private val registroCadastro: RegistroCadastro,
    @SyncScope private val syncScope: CoroutineScope,
) : AgenteRepository {

    private var syncJob: Job? = null

    override fun sincronizar() {
        if (syncJob?.isActive == true) return
        syncJob = firestore.sincronizarColecao(
            colecao = COLLECTION_AGENTS,
            tag = TAG,
            scope = syncScope,
            paraModelo = { it.toObject<AgenteDocumento>()?.toAgente(it.id) },
            salvarTodos = { dao.salvarTodos(*it.toTypedArray()) },
        )
    }

    override suspend fun salvar(agente: Agente) {
        val documento = if (agente.id.isBlank()) {
            firestore.collection(COLLECTION_AGENTS).document()
        } else {
            firestore.collection(COLLECTION_AGENTS).document(agente.id)
        }
        val comId = agente.copy(id = documento.id)

        // FALHA: Room não gravou — desfecho não recuperável, propaga pro VM tratar.
        try {
            dao.salvar(comId)
        } catch (e: Exception) {
            registroCadastro.falhou(ENTIDADE, e)
            throw RuntimeException("Falha ao salvar agente: ${e.message}", e)
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

    override suspend fun obterPorId(id: String) = dao.obterPorId(id).first()

    override suspend fun obterTodasAgencias(): List<String> =
        dao.obterTodasAgencias().first().distinct()

    override suspend fun obterTodosAgentes() = dao.obterTodos().first()

    override suspend fun obterAgentesPorAgencia(agencia: String) =
        dao.obterTodosPorAgencia(agencia).first()

    private companion object {
        const val TAG = "agenteRepository"
        const val ENTIDADE = "agente"
    }
}
