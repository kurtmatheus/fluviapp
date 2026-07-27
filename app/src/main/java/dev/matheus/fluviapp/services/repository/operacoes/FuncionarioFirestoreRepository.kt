package dev.matheus.fluviapp.services.repository.operacoes

import android.util.Log
import dev.matheus.fluviapp.database.dao.operacoes.FuncionarioDao
import dev.matheus.fluviapp.model.operacoes.Funcionario
import dev.matheus.fluviapp.model.operacoes.toDocumento
import dev.matheus.fluviapp.services.repository.operacoes.FuncionarioRepository.Companion.COLLECTION_FUNCIONARIOS
import dev.matheus.fluviapp.services.repository.firebase.FonteSnapshots
import dev.matheus.fluviapp.services.repository.firebase.documents.toFuncionario
import dev.matheus.fluviapp.services.repository.firebase.documents.toFuncionarioDocumento
import dev.matheus.fluviapp.di.module.SyncScope
import dev.matheus.fluviapp.services.repository.firebase.sincronizarColecao
import dev.matheus.fluviapp.telemetry.RegistroCadastro
import dev.matheus.fluviapp.telemetry.RegistroSincronizacao
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Impl Firestore da porta [FuncionarioRepository] — espelha no Room (ADR-0003), contrato do molde. */
@Singleton
class FuncionarioFirestoreRepository @Inject constructor(
    private val dao: FuncionarioDao,
    private val firestore: FirebaseFirestore,
    private val registroCadastro: RegistroCadastro,
    @SyncScope private val syncScope: CoroutineScope,
    private val registroSincronizacao: RegistroSincronizacao,
    private val fonteSnapshots: FonteSnapshots,
) : FuncionarioRepository {

    private var syncJob: Job? = null

    override fun sincronizar() {
        if (syncJob?.isActive == true) return
        syncJob = sincronizarColecao(
            fonte = fonteSnapshots,
            colecao = COLLECTION_FUNCIONARIOS,
            scope = syncScope,
            registro = registroSincronizacao,
            paraModelo = { it.toFuncionarioDocumento().toFuncionario(it.id) },
            salvarTodos = { dao.salvarTodos(*it.toTypedArray()) },
        )
    }

    override suspend fun salvar(funcionario: Funcionario) {
        val documento = if (funcionario.id.isBlank()) {
            firestore.collection(COLLECTION_FUNCIONARIOS).document()
        } else {
            firestore.collection(COLLECTION_FUNCIONARIOS).document(funcionario.id)
        }
        val comId = funcionario.copy(id = documento.id)

        // FALHA: Room não gravou — desfecho não recuperável, propaga pro VM tratar.
        try {
            dao.salvar(comId)
        } catch (e: Exception) {
            registroCadastro.falhou(ENTIDADE, e)
            throw RuntimeException("Falha ao salvar funcionario: ${e.message}", e)
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

    override suspend fun obterPorId(id: String) = dao.obterPorId(id).first()

    override suspend fun obterTodasAgencias(): List<String> =
        dao.obterTodasAgencias().first().distinct()

    override suspend fun obterTodosFuncionarios() = dao.obterTodos().first()

    override suspend fun obterFuncionariosPorAgencia(agencia: String) =
        dao.obterTodosPorAgencia(agencia).first()

    // Deleta local (otimista) + doc do Firestore. O listener de sessão (ADR-0009) reconcilia o Room.
    // Falha no servidor não derruba a UI — só loga (o doc remanescente re-sincroniza depois).
    override suspend fun deletar(id: String) {
        dao.deletar(id)
        try {
            firestore.collection(COLLECTION_FUNCIONARIOS).document(id).delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "deletar($id): ${e.message}", e)
        }
    }

    private companion object {
        const val TAG = "funcionarioRepository"
        const val ENTIDADE = "funcionario"
    }
}
