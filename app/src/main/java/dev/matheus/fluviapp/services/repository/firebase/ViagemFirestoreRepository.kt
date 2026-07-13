package dev.matheus.fluviapp.services.repository.firebase

import android.util.Log
import dev.matheus.fluviapp.database.dao.cadastro.viagem.ViagemDao
import dev.matheus.fluviapp.extensions.formatarCodigoViagemNavioFB
import dev.matheus.fluviapp.model.viagem.Viagem
import dev.matheus.fluviapp.model.viagem.toDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.ViagemDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.toViagem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Impl Firestore da porta [ViagemRepository] — espelha no Room (ADR-0003), contrato do molde (ADR-0006). */
@Singleton
class ViagemFirestoreRepository @Inject constructor(
    private val dao: ViagemDao,
    private val firestore: FirebaseFirestore
) : ViagemRepository {

    override fun sincronizar() = firestore.sincronizarColecao(
        colecao = COLLECTION_VIAGENS,
        tag = TAG,
        paraModelo = { it.toObject<ViagemDocumento>()?.toViagem(it.id) },
        salvarLocal = { dao.salvar(it) },
    )

    override suspend fun salvar(viagem: Viagem) {
        val documento = if (viagem.id.isBlank()) {
            firestore.collection(COLLECTION_VIAGENS).document()
        } else {
            firestore.collection(COLLECTION_VIAGENS).document(viagem.id)
        }
        // codigo é derivado na persistência (a partir do navio); id vem do doc.
        val comId = viagem.copy(id = documento.id)
        val completo = comId.copy(codigo = comId.formatarCodigoViagemNavioFB())
        dao.salvar(completo)
        try {
            documento.set(completo.toDocumento())
        } catch (e: Exception) {
            Log.e(TAG, "salvar: ${e.message}", e)
            throw RuntimeException("Falha ao salvar viagem: ${e.message}", e)
        }
    }

    override suspend fun obterPorId(id: String) = dao.obterPorId(id).first()

    override suspend fun obterPorCodigo(codigo: String) = dao.obterPorCodigo(codigo).first()

    override suspend fun obterTodas() = dao.obterTodas().first()

    override suspend fun deletar(id: String) {
        val viagem = obterPorId(id)
        dao.deletar(viagem)
        try {
            firestore.collection(COLLECTION_VIAGENS).document(id).delete()
        } catch (e: Exception) {
            Log.e(TAG, "deletar: ${e.message}", e)
            throw RuntimeException("Falha ao deletar viagem: ${e.message}", e)
        }
    }

    private companion object {
        const val TAG = "viagemFirestoreRepository"
        const val COLLECTION_VIAGENS = "viagens"
    }
}
