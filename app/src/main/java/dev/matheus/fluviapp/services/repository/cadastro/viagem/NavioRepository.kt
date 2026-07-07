package dev.matheus.fluviapp.services.repository.cadastro.viagem

import android.util.Log
import dev.matheus.fluviapp.database.dao.cadastro.viagem.NavioDao
import dev.matheus.fluviapp.model.viagem.Navio
import dev.matheus.fluviapp.services.repository.firebase.documents.NavioDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.toNavio
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavioRepository @Inject constructor(
    private val dao: NavioDao,
    private val firestore: FirebaseFirestore
) {

    fun sincronizar() {
        firestore.collection(COLLECTION_NAVIOS)
            .addSnapshotListener { value, error ->
                value?.documents?.mapNotNull { document ->
                    document.toObject<NavioDocumento>()?.toNavio(document.id)
                }?.forEach { navio ->
                    runBlocking {
                        dao.salvar(navio)
                    }
                }

                if (error != null) {
                    Log.e(TAG, "sincronizar: Exception: ${error.message}")
                    throw RuntimeException("Falha no Processamento: ${error.message}")
                }
            }
    }

    suspend fun obterTodos() = dao.obterTodos().first()

    suspend fun obterPorId(id: String) = dao.obterPorId(id).first()

    suspend fun salvar(navio: Navio) = dao.salvar(navio)

    suspend fun obterPorNome(nome: String) = dao.obterPorNome(nome).first()

    companion object {
        private const val TAG = "navioRepository"
        private const val COLLECTION_NAVIOS = "navios"
    }
}
