package dev.matheus.fluviapp.services.repository.cadastro.passagem

import android.util.Log
import dev.matheus.fluviapp.database.dao.cadastro.passagem.AgenteDao
import dev.matheus.fluviapp.model.cadastro.passagem.Agente
import dev.matheus.fluviapp.model.cadastro.passagem.toDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.AgenteDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.toAgente
import dev.matheus.fluviapp.services.repository.firebase.sincronizarColecao
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgenteRepository @Inject constructor(
    private val dao: AgenteDao,
    private val firestore: FirebaseFirestore
) {

    fun sincronizar() = firestore.sincronizarColecao(
        colecao = COLLECTION_AGENTS,
        tag = TAG,
        paraModelo = { it.toObject<AgenteDocumento>()?.toAgente(it.id) },
        salvarLocal = { dao.salvar(it) },
    )

    /** Contrato normalizado (molde cadastro-modulos §7): id em branco → auto-id; Room otimista + Firestore. */
    suspend fun salvar(agente: Agente) {
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

    suspend fun obterPorId(id: String) = dao.obterPorId(id).first()

    suspend fun obterTodasAgencias(): List<String> =
        dao.obterTodasAgencias().first().distinct()

    suspend fun obterTodosAgentes() = dao.obterTodos().first()

    suspend fun obterAgentesPorAgencia(agencia: String) = dao.obterTodosPorAgencia(agencia).first()

    companion object {
        private const val TAG = "agenteRepository"
        const val COLLECTION_AGENTS = "agents"
    }
}
