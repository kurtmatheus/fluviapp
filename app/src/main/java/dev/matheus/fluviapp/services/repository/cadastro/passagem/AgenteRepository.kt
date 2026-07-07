package dev.matheus.fluviapp.services.repository.cadastro.passagem

import android.util.Log
import dev.matheus.fluviapp.database.dao.cadastro.passagem.AgenteDao
import dev.matheus.fluviapp.model.cadastro.passagem.Agente
import dev.matheus.fluviapp.model.cadastro.passagem.toDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.AgenteDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.toAgente
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgenteRepository @Inject constructor(
    private val dao: AgenteDao,
    private val firestore: FirebaseFirestore
) {

    fun sincronizar() {
        firestore.collection(COLLECTION_AGENTS)
            .addSnapshotListener { value, error ->
                value?.documents?.mapNotNull { document ->
                    document.toObject<AgenteDocumento>()?.toAgente(document.id)
                }?.forEach { agente ->
                    runBlocking {
                        dao.salvar(agente)
                    }
                }

                if (error != null) {
                    Log.e(TAG, "sincronizar: Exception: ${error.message}")
                    throw RuntimeException("Falha no Processamento: ${error.message}")
                }
            }
    }

    fun salvar(agente: Agente) {
        val document = obterDocumento(agente.id)
        try {
            val viagemDocumento = agente.toDocumento()
            document.set(viagemDocumento)
        } catch (e: Exception) {
            Log.e(TAG, "salvar: Exception: ${e.message}")
            throw RuntimeException("Falha no Processo: ${e.message}")
        }
    }

    private fun obterDocumento(id: String): DocumentReference {
        return if (id.isBlank()) {
            firestore.collection(COLLECTION_AGENTS).document()
        } else firestore.collection(COLLECTION_AGENTS).document(id)
    }

    suspend fun obterPorId(id: String) = dao.obterPorId(id).first()

    suspend fun obterTodasAgencias(): List<String> {
        val listaFiltrada = emptyList<String>().toMutableList()
        dao.obterTodasAgencias().first().forEach {
            if (!listaFiltrada.contains(it)) {
                listaFiltrada.add(it)
            }
        }
        return listaFiltrada
    }

    suspend fun obterTodosAgentes() = dao.obterTodos().first()

    suspend fun obterAgentesPorAgencia(agencia: String) = dao.obterTodosPorAgencia(agencia).first()

    companion object {
        private const val TAG = "agenteRepository"
        const val COLLECTION_AGENTS = "agents"
    }
}