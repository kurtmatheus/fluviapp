package dev.matheus.fluviapp.services.repository.cadastro.viagem

import android.util.Log
import dev.matheus.fluviapp.database.dao.cadastro.viagem.EmpresaDao
import dev.matheus.fluviapp.model.viagem.Empresa
import dev.matheus.fluviapp.model.viagem.toDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.EmpresaDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.toEmpresa
import dev.matheus.fluviapp.services.repository.firebase.sincronizarColecao
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmpresaRepository @Inject constructor(
    private val dao: EmpresaDao,
    private val firestore: FirebaseFirestore
) {
    fun sincronizar() = firestore.sincronizarColecao(
        colecao = COLLECTION_EMPRESAS,
        tag = TAG,
        paraModelo = { it.toObject<EmpresaDocumento>()?.toEmpresa(it.id) },
        salvarLocal = { dao.salvar(it) },
    )

    /** Contrato normalizado (molde cadastro-modulos §7): id em branco → auto-id; Room otimista + Firestore. */
    suspend fun salvar(empresa: Empresa) {
        val documento = if (empresa.id.isBlank()) {
            firestore.collection(COLLECTION_EMPRESAS).document()
        } else {
            firestore.collection(COLLECTION_EMPRESAS).document(empresa.id)
        }
        val comId = empresa.copy(id = documento.id)
        dao.salvar(comId)
        try {
            documento.set(comId.toDocumento())
        } catch (e: Exception) {
            Log.e(TAG, "salvar: ${e.message}", e)
            throw RuntimeException("Falha ao salvar empresa: ${e.message}", e)
        }
    }

    suspend fun obterTodas() = dao.obterTodas().first()

    suspend fun obterPorId(id: String) = dao.obterPorId(id).first()

    suspend fun obterPorNome(nome: String) = dao.obterPorNome(nome).first()

    companion object {
        private const val TAG = "empresaRepository"
        private const val COLLECTION_EMPRESAS = "empresas"
    }
}
