package dev.matheus.fluviapp.services.repository.cadastro.viagem

import dev.matheus.fluviapp.database.dao.cadastro.viagem.EmpresaDao
import dev.matheus.fluviapp.model.viagem.Empresa
import dev.matheus.fluviapp.model.viagem.toDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.EmpresaDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.toEmpresa
import dev.matheus.fluviapp.di.module.SyncScope
import dev.matheus.fluviapp.services.repository.firebase.sincronizarColecao
import dev.matheus.fluviapp.telemetry.RegistroCadastro
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Impl Firestore da porta [EmpresaRepository] — espelha no Room (ADR-0003), contrato do molde. */
@Singleton
class EmpresaFirestoreRepository @Inject constructor(
    private val dao: EmpresaDao,
    private val firestore: FirebaseFirestore,
    private val registroCadastro: RegistroCadastro,
    @SyncScope private val syncScope: CoroutineScope,
) : EmpresaRepository {

    private var syncJob: Job? = null

    override fun sincronizar() {
        if (syncJob?.isActive == true) return
        syncJob = firestore.sincronizarColecao(
            colecao = COLLECTION_EMPRESAS,
            tag = TAG,
            scope = syncScope,
            paraModelo = { it.toObject<EmpresaDocumento>()?.toEmpresa(it.id) },
            salvarTodos = { dao.salvarTodas(*it.toTypedArray()) },
        )
    }

    override suspend fun salvar(empresa: Empresa) {
        val documento = if (empresa.id.isBlank()) {
            firestore.collection(COLLECTION_EMPRESAS).document()
        } else {
            firestore.collection(COLLECTION_EMPRESAS).document(empresa.id)
        }
        val comId = empresa.copy(id = documento.id)

        // FALHA: Room não gravou — desfecho não recuperável, propaga pro VM tratar.
        try {
            dao.salvar(comId)
        } catch (e: Exception) {
            registroCadastro.falhou(ENTIDADE, e)
            throw RuntimeException("Falha ao salvar empresa: ${e.message}", e)
        }

        // Room já tem o dado (otimista). Aguarda o ack do Firestore: SUCESSO se confirmar,
        // WARNING (pendente-sync) se rejeitar/offline — não relança, o dado local reconcilia.
        try {
            documento.set(comId.toDocumento()).await()
            registroCadastro.salvou(ENTIDADE, comId.id)
        } catch (e: Exception) {
            registroCadastro.pendenteDeSync(ENTIDADE, comId.id, e)
        }
    }

    override suspend fun obterTodas() = dao.obterTodas().first()

    override suspend fun obterPorId(id: String) = dao.obterPorId(id).first()

    override suspend fun obterPorNome(nome: String) = dao.obterPorNome(nome).first()

    private companion object {
        const val TAG = "empresaRepository"
        const val COLLECTION_EMPRESAS = "empresas"
        const val ENTIDADE = "empresa"
    }
}
