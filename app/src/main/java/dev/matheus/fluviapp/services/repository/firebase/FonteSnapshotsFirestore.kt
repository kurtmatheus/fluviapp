package dev.matheus.fluviapp.services.repository.firebase

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Impl Firestore da porta [FonteSnapshots]. Converte cada snapshot em [DocumentoBruto] (id + `data`),
 * sem vazar tipos Firebase para fora. `callbackFlow` + `awaitClose` removem a `ListenerRegistration`
 * no cancelamento; erro vira `Falha` sem fechar o Flow (deixa o Firestore reconectar).
 */
@Singleton
class FonteSnapshotsFirestore @Inject constructor(
    private val firestore: FirebaseFirestore,
) : FonteSnapshots {

    override fun observar(colecao: String): Flow<ResultadoColecao> = callbackFlow {
        val registration = firestore.collection(colecao).addSnapshotListener { value, error ->
            if (error != null) {
                trySend(ResultadoColecao.Falha(error))
                return@addSnapshotListener
            }
            value?.let { snapshot ->
                val documentos = snapshot.documents.map { DocumentoBruto(it.id, it.data.orEmpty()) }
                trySend(ResultadoColecao.Dados(documentos, snapshot.metadata.isFromCache))
            }
        }
        awaitClose { registration.remove() }
    }

    override fun observarDocumento(colecao: String, documento: String): Flow<ResultadoDocumento> = callbackFlow {
        val registration = firestore.collection(colecao).document(documento)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    trySend(ResultadoDocumento.Falha(error))
                    return@addSnapshotListener
                }
                val bruto = value?.takeIf { it.exists() }?.let { DocumentoBruto(it.id, it.data.orEmpty()) }
                trySend(ResultadoDocumento.Dados(bruto, value?.metadata?.isFromCache ?: false))
            }
        awaitClose { registration.remove() }
    }
}
