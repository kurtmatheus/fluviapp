package dev.matheus.fluviapp.services.repository.operacoes

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import dev.matheus.fluviapp.di.module.SyncScope
import dev.matheus.fluviapp.domain.operacoes.Funcionario
import dev.matheus.fluviapp.services.repository.firebase.CodecFirestore
import dev.matheus.fluviapp.services.repository.firebase.ColecaoFirestore
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto
import dev.matheus.fluviapp.services.repository.firebase.FonteSnapshots
import dev.matheus.fluviapp.services.repository.firebase.documents.paraMapa
import dev.matheus.fluviapp.services.repository.firebase.documents.toFuncionario
import dev.matheus.fluviapp.services.repository.operacoes.FuncionarioRepository.Companion.COLLECTION_FUNCIONARIOS
import dev.matheus.fluviapp.telemetry.RegistroCadastro
import dev.matheus.fluviapp.telemetry.RegistroSincronizacao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** O codec do Funcionário: o pouco que é dele na fronteira (ADR-0019 D2). */
private object FuncionarioCodec : CodecFirestore<Funcionario> {
    override val colecao = COLLECTION_FUNCIONARIOS
    override val entidade = "funcionario"
    override fun deDocumento(bruto: DocumentoBruto) = bruto.toFuncionario()
    override fun paraMapa(modelo: Funcionario) = modelo.paraMapa()
    override fun id(modelo: Funcionario) = modelo.id
    override fun comId(modelo: Funcionario, id: String) = modelo.copy(id = id)
}

/**
 * Impl Firestore da porta [FuncionarioRepository] — **a quinta coleção sem Room**, e a última do caminho
 * vivo a perder o espelho (ADR-0017 D1).
 *
 * O CRUD comum é composto da [ColecaoFirestore]. O que sobra de próprio é o que **não** é CRUD:
 * [obterPorEmailDoServidor], que é uma consulta ao servidor no momento em que ainda não há listener
 * ligado — e é por isso que ela não pôde virar leitura da coleção em memória.
 *
 * As duas consultas por agência viram **filtro em memória**, e não uma query nova. A coleção é pequena e
 * já chega inteira pelo listener; criar uma query para recortá-la seria abrir um segundo caminho de
 * leitura para o mesmo dado. Elas morrem na F6.3, quando a agência virar o `empresaId` do vínculo.
 */
@Singleton
class FuncionarioFirestoreRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    registroCadastro: RegistroCadastro,
    @SyncScope syncScope: CoroutineScope,
    registroSincronizacao: RegistroSincronizacao,
    fonteSnapshots: FonteSnapshots,
) : FuncionarioRepository {

    private val colecao = ColecaoFirestore(
        codec = FuncionarioCodec,
        firestore = firestore,
        fonteSnapshots = fonteSnapshots,
        syncScope = syncScope,
        registroCadastro = registroCadastro,
        registroSincronizacao = registroSincronizacao,
    )

    override fun sincronizar() = colecao.sincronizar()

    override fun observarTodos(): StateFlow<List<Funcionario>> = colecao.observarTodos()

    override suspend fun salvar(funcionario: Funcionario): String = colecao.salvar(funcionario)

    override suspend fun obterPorId(id: String): Funcionario? = colecao.obterPorId(id)

    override suspend fun obterTodosFuncionarios(): List<Funcionario> = colecao.obterTodos()


    /**
     * Consulta o servidor por e-mail (ADR-0015 §2.1). **Não passa pela coleção em memória de propósito**:
     * no primeiro acesso o listener ainda não subiu — sincronizar exige estar logado, e aqui a pessoa
     * acabou de autenticar. Devolve `null` se não houver registro, que é o caso "autenticou mas não é da
     * casa" — distinto de "ainda não chegou", que aqui não existe porque a leitura é direta.
     */
    override suspend fun obterPorEmailDoServidor(email: String): Funcionario? = try {
        firestore.collection(COLLECTION_FUNCIONARIOS)
            .whereEqualTo("email", email.trim())
            .limit(1)
            .get().await()
            .documents.firstOrNull()
            ?.let { DocumentoBruto(id = it.id, dados = it.data.orEmpty()).toFuncionario() }
    } catch (e: Exception) {
        Log.e(TAG, "obterPorEmailDoServidor($email): ${e.message}", e)
        null
    }

    override suspend fun deletar(id: String) = colecao.deletar(id)

    private companion object {
        const val TAG = "funcionarioRepository"
    }
}