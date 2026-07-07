package br.com.gruponaveg.services.repository.cadastro.viagem

import android.util.Log
import br.com.gruponaveg.database.dao.cadastro.viagem.EmpresaDao
import br.com.gruponaveg.services.repository.firebase.documents.EmpresaDocumento
import br.com.gruponaveg.services.repository.firebase.documents.toEmpresa
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmpresaRepository @Inject constructor(
    private val dao: EmpresaDao,
    private val firestore: FirebaseFirestore
) {
    fun sincronizar() {
        firestore.collection(COLLECTION_EMPRESAS)
            .addSnapshotListener { value, error ->
                value?.documents?.mapNotNull { document ->
                    document.toObject<EmpresaDocumento>()?.toEmpresa(document.id)
                }?.forEach { empresa ->
                    runBlocking {
                        dao.salvar(empresa)
                    }
                }

                if (error != null) {
                    Log.e(TAG, "sincronizar: Exception: ${error.message}")
                    throw RuntimeException("Falha no Processamento: ${error.message}")
                }
            }
    }

    suspend fun obterTodas() = dao.obterTodas().first()

    suspend fun obterPorId(idEmpresa: Int) = dao.obterPorId(idEmpresa).first()

    suspend fun obterPorNome(empresa: String) = dao.obterPorNome(empresa).first()

    companion object {
        private const val TAG = "empresaRepository"
        private const val COLLECTION_EMPRESAS = "empresas"
    }
}