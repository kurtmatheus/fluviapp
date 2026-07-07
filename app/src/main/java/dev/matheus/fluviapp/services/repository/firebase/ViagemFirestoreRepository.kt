package dev.matheus.fluviapp.services.repository.firebase

import android.util.Log
import dev.matheus.fluviapp.database.dao.cadastro.viagem.ViagemDao
import dev.matheus.fluviapp.extensions.formatarCodigoViagemNavioFB
import dev.matheus.fluviapp.model.viagem.Viagem
import dev.matheus.fluviapp.model.viagem.toDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.ViagemDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.toViagem
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ViagemFirestoreRepository @Inject constructor(
    private val dao: ViagemDao,
    private val firestore: FirebaseFirestore
) {

    fun sincronizar() {
        firestore.collection(COLLECTION_VIAGENS)
            .addSnapshotListener { value, error ->
                value?.documents?.mapNotNull { document ->
                    document.toObject<ViagemDocumento>()?.toViagem(document.id)
                }?.forEach { viagem ->
                    runBlocking {
                        dao.salvar(viagem)
                    }
                }

                if (error != null) {
                    Log.e(TAG, "salvarTodas: Exception: ${error.message}")
                    throw RuntimeException("Falha na Requisicao: ${error.message}")
                }
            }
    }

    suspend fun salvar(id: String?, navio: String, empresa: String, origem: String, destino: String) {
        val documento = retornaDocumentReference(id)
        Viagem(
            id = documento.id,
            codigo = "",
            navio = navio,
            empresa = empresa,
            origem = origem,
            destino = destino
        ).apply {
            val viagemComCodigo = copy(codigo = formatarCodigoViagemNavioFB())
            dao.salvar(viagemComCodigo)
            try {
                val viagemDocumento = viagemComCodigo.toDocumento()
                documento.set(viagemDocumento)
            } catch (e: Exception) {
                Log.e(TAG, "salvar: Exception: ${e.message}")
                throw RuntimeException("Falha no Processo: ${e.message}")
            }
        }
    }

    private fun retornaDocumentReference(id: String?): DocumentReference {
        return id?.let {
            firestore.collection(COLLECTION_VIAGENS).document(it)
        } ?: firestore.collection(COLLECTION_VIAGENS).document()
    }

    suspend fun obterPorId(id: String) = dao.obterPorId(id).first()

    suspend fun obterPorCodigo(codigo: String) = dao.obterPorCodigo(codigo).first()

    suspend fun obterTodas() = dao.obterTodas().first()

    suspend fun deletar(id: String) {
        val viagem = obterPorId(id)
        dao.deletar(viagem)
        try {
            firestore.collection(COLLECTION_VIAGENS)
                .document(id)
                .delete()
        } catch (e: Exception) {
            Log.e(TAG, "deletar: Exception: ${e.message}")
            throw RuntimeException("Falha no Processo: ${e.message}")
        }
    }


    companion object {
        private const val TAG = "viagemFirestoreRepository"
        const val COLLECTION_VIAGENS = "viagens"
    }
}