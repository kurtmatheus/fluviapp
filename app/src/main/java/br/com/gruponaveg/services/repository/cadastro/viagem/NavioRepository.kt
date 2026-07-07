package br.com.gruponaveg.services.repository.cadastro.viagem

import android.util.Log
import br.com.gruponaveg.database.dao.cadastro.viagem.NavioDao
import br.com.gruponaveg.model.viagem.Navio
import br.com.gruponaveg.services.repository.firebase.documents.NavioDocumento
import br.com.gruponaveg.services.repository.firebase.documents.toNavio
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
