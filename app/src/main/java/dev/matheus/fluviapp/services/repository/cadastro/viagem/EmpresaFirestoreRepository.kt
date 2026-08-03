package dev.matheus.fluviapp.services.repository.cadastro.viagem

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import dev.matheus.fluviapp.di.module.SyncScope
import dev.matheus.fluviapp.domain.operacoes.Atuacao
import dev.matheus.fluviapp.domain.viagem.AtuacaoDaEmpresa
import dev.matheus.fluviapp.domain.viagem.Empresa
import dev.matheus.fluviapp.services.repository.firebase.FonteSnapshots
import dev.matheus.fluviapp.services.repository.firebase.documents.paraMapa
import dev.matheus.fluviapp.services.repository.firebase.documents.toEmpresa
import dev.matheus.fluviapp.services.repository.firebase.sincronizarColecao
import dev.matheus.fluviapp.telemetry.RegistroCadastro
import dev.matheus.fluviapp.telemetry.RegistroSincronizacao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Impl Firestore da porta [EmpresaRepository] — **a primeira coleção sem espelho Room** (ADR-0017 D1,
 * ADR-0020 F5).
 *
 * O que muda em relação ao molde anterior, e por quê:
 *
 * - **a fonte reativa é um [StateFlow] em memória**, alimentado pelo listener. O Room existia para
 *   guardar entre execuções, mas quem já faz isso é o **cache do SDK do Firestore** — manter os dois era
 *   pagar duas persistências para ter uma;
 * - **a leitura espera o primeiro snapshot** em vez de devolver lista vazia. Sem isso, trocar Room por
 *   memória transformaria *"ainda não chegou"* em *"não existe"* — e a diferença entre as duas coisas é
 *   justamente o que o [_recebeuSnapshot] guarda. O snapshot inicial vem do cache local, então a espera
 *   é curta e termina mesmo offline;
 * - **a escrita otimista sai** — não porque se abriu mão dela, mas porque o SDK já a faz: `set()` grava
 *   no cache e emite pelo listener antes de falar com o servidor. Era isso que o `dao.salvar()` estava
 *   duplicando.
 *
 * A fronteira é `Map` (ADR-0019 D2): `DocumentoBruto` → `Empresa` na leitura, `Empresa` → `Map` na
 * escrita. O `EmpresaDocumento` sai do caminho e fica só como documentação da forma.
 */
@Singleton
class EmpresaFirestoreRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val registroCadastro: RegistroCadastro,
    @SyncScope private val syncScope: CoroutineScope,
    private val registroSincronizacao: RegistroSincronizacao,
    private val fonteSnapshots: FonteSnapshots,
) : EmpresaRepository {

    private val _empresas = MutableStateFlow<List<Empresa>>(emptyList())

    /** `true` depois do primeiro snapshot — distingue "coleção vazia" de "ainda não chegou". */
    private val _recebeuSnapshot = MutableStateFlow(false)

    private var syncJob: Job? = null

    override fun observarTodas(): StateFlow<List<Empresa>> = _empresas.asStateFlow()

    override fun sincronizar() {
        if (syncJob?.isActive == true) return
        syncJob = sincronizarColecao(
            fonte = fonteSnapshots,
            colecao = COLLECTION_EMPRESAS,
            scope = syncScope,
            registro = registroSincronizacao,
            paraModelo = { it.toEmpresa() },
            salvarTodos = { empresas ->
                _empresas.value = empresas
                _recebeuSnapshot.value = true
            },
        )
    }

    override suspend fun salvar(empresa: Empresa): String {
        val documento = if (empresa.id.isBlank()) {
            firestore.collection(COLLECTION_EMPRESAS).document()
        } else {
            firestore.collection(COLLECTION_EMPRESAS).document(empresa.id)
        }
        val comId = empresa.copy(id = documento.id)

        // O `set()` grava no cache do SDK na hora e o listener reflete; o `await()` espera o ack do
        // servidor. Rejeição/offline vira WARNING (pendente-sync) e NÃO relança: o dado local já está
        // aplicado e o SDK reconcilia quando a rede voltar.
        try {
            documento.set(comId.paraMapa()).await()
            registroCadastro.salvou(ENTIDADE, comId.id)
        } catch (e: Exception) {
            registroCadastro.pendenteDeSync(ENTIDADE, comId.id, e)
        }
        return comId.id
    }

    override suspend fun obterAtuacoes(empresaId: String): List<AtuacaoDaEmpresa> {
        if (empresaId.isBlank()) return emptyList()
        return try {
            atuacoesDe(empresaId).get().await().documents.mapNotNull { doc ->
                // O id do documento É o nome da atuação (ADR-0016 §4). Valor que o código não conhece
                // é descartado — fail-closed: não se inventa atuação a partir de um id qualquer.
                Atuacao.de(doc.id)?.let { atuacao ->
                    AtuacaoDaEmpresa(
                        atuacao = atuacao,
                        navioIds = (doc.get(CAMPO_NAVIO_IDS) as? List<*>)
                            ?.filterIsInstance<String>()
                            .orEmpty()
                            .toSet(),
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "obterAtuacoes($empresaId): ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun salvarAtuacoes(empresaId: String, atuacoes: List<AtuacaoDaEmpresa>) {
        if (empresaId.isBlank()) return
        val colecao = atuacoesDe(empresaId)
        try {
            val existentes = colecao.get().await().documents.map { it.id }.toSet()
            val desejadas = atuacoes.associateBy { it.atuacao.name }

            // Uma escrita em lote: acrescentar e REMOVER no mesmo ato. Sem a remoção, deixar de exercer
            // uma atuação seria inexprimível — o documento antigo sobreviveria à edição.
            firestore.runBatch { lote ->
                desejadas.forEach { (id, atuacao) ->
                    lote.set(colecao.document(id), mapOf(CAMPO_NAVIO_IDS to atuacao.navioIds.toList()))
                }
                (existentes - desejadas.keys).forEach { lote.delete(colecao.document(it)) }
            }.await()
            registroCadastro.salvou(ENTIDADE_ATUACAO, empresaId)
        } catch (e: Exception) {
            registroCadastro.pendenteDeSync(ENTIDADE_ATUACAO, empresaId, e)
        }
    }

    private fun atuacoesDe(empresaId: String) =
        firestore.collection(COLLECTION_EMPRESAS).document(empresaId).collection(SUBCOLECAO_ATUACOES)

    override suspend fun obterTodas(): List<Empresa> {
        sincronizar()
        _recebeuSnapshot.first { it }
        return _empresas.value
    }

    override suspend fun obterPorId(id: String): Empresa? =
        obterTodas().firstOrNull { it.id == id }

    override suspend fun deletar(id: String) {
        try {
            firestore.collection(COLLECTION_EMPRESAS).document(id).delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "deletar($id): ${e.message}", e)
        }
    }

    private companion object {
        const val TAG = "empresaRepository"
        const val COLLECTION_EMPRESAS = "empresas"
        const val ENTIDADE = "empresa"
        const val ENTIDADE_ATUACAO = "empresa.atuacoes"
        const val SUBCOLECAO_ATUACOES = "atuacoes"
        const val CAMPO_NAVIO_IDS = "navioIds"
    }
}