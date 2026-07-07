package dev.matheus.fluviapp.services.repository.cadastro

import android.util.Log
import dev.matheus.fluviapp.database.dao.cadastro.ConstanteDao
import dev.matheus.fluviapp.model.cadastro.constantes.Constante
import dev.matheus.fluviapp.services.repository.firebase.documents.ConstanteDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.toConstante
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConstanteRepository @Inject constructor(
    private val dao: ConstanteDao,
    private val firestore: FirebaseFirestore
) {
    fun sincronizar() {
        firestore.collection(COLLECTION_CONSTANTS)
            .addSnapshotListener { value, error ->
                value?.documents?.mapNotNull { document ->
                    document.toObject<ConstanteDocumento>()?.toConstante(document.id)
                }?.forEach { constante ->
                    runBlocking {
                        dao.salvar(constante)
                    }
                }               

                if (error != null) {
                    Log.e(TAG, "sincronizar: Exception: ${error.message}")
                    throw RuntimeException("Falha no Processamento: ${error.message}")
                }
            }
    }

    suspend fun obterTodosPorCategoria(categoria: String): List<Constante> {
        return dao.obterTodosPorCategoria(categoria = categoria).first()
    }

    suspend fun obterTodas() = dao.obterTodos().first()

    companion object {
        private const val TAG = "constantesRepository"
        const val COLLECTION_CONSTANTS = "constants"
    }
}
