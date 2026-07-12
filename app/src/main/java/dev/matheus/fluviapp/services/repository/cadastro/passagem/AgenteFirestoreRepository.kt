package dev.matheus.fluviapp.services.repository.cadastro.passagem

import android.util.Log
import dev.matheus.fluviapp.database.dao.cadastro.passagem.AgenteDao
import dev.matheus.fluviapp.model.cadastro.passagem.Agente
import dev.matheus.fluviapp.model.cadastro.passagem.toDocumento
import dev.matheus.fluviapp.services.repository.cadastro.passagem.AgenteRepository.Companion.COLLECTION_AGENTS
import dev.matheus.fluviapp.services.repository.firebase.documents.AgenteDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.toAgente
import dev.matheus.fluviapp.services.repository.firebase.sincronizarColecao
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Impl Firestore da porta [AgenteRepository] — espelha no Room (ADR-0003), contrato do molde. */
@Singleton
class AgenteFirestoreRepository @Inject constructor(
    private val dao: AgenteDao,
    private val firestore: FirebaseFirestore
) : AgenteRepository {

    override fun sincronizar() = firestore.sincronizarColecao(
        colecao = COLLECTION_AGENTS,
        tag = TAG,
        paraModelo = { it.toObject<AgenteDocumento>()?.toAgente(it.id) },
        salvarLocal = { dao.salvar(it) },
    )

    override suspend fun salvar(agente: Agente) {
        val documento = if (agente.id.isBlank()) {
            firestore.collection(COLLECTION_AGENTS).document()
        } else {
            firestore.collection(COLLECTION_AGENTS).document(agente.id)
        }
        val comId = agente.copy(id = documento.id)
        dao.salvar(comId)
        try {
            documento.set(comId.toDocumento())
        } catch (e: Exception) {
            Log.e(TAG, "salvar: ${e.message}", e)
            throw RuntimeException("Falha ao salvar agente: ${e.message}", e)
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
    }
}
