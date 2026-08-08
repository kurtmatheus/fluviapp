package dev.matheus.fluviapp.services.repository.cadastro.viagem

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import dev.matheus.fluviapp.di.module.SyncScope
import dev.matheus.fluviapp.domain.operacoes.Atuacao
import dev.matheus.fluviapp.domain.viagem.AtuacaoDaEmpresa
import dev.matheus.fluviapp.domain.viagem.Empresa
import dev.matheus.fluviapp.services.repository.firebase.CodecFirestore
import dev.matheus.fluviapp.services.repository.firebase.ColecaoFirestore
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto
import dev.matheus.fluviapp.services.repository.firebase.FonteSnapshots
import dev.matheus.fluviapp.services.repository.firebase.documents.paraMapa
import dev.matheus.fluviapp.services.repository.firebase.documents.toEmpresa
import dev.matheus.fluviapp.telemetry.RegistroCadastro
import dev.matheus.fluviapp.telemetry.RegistroSincronizacao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
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
/**
 * O codec da Empresa: o pouco que é dela na fronteira. Todo o resto do CRUD é da [ColecaoFirestore].
 */
private object EmpresaCodec : CodecFirestore<Empresa> {
    override val colecao = "empresas"
    override val entidade = "empresa"
    override fun deDocumento(bruto: DocumentoBruto) = bruto.toEmpresa()
    override fun paraMapa(modelo: Empresa) = modelo.paraMapa()
    override fun id(modelo: Empresa) = modelo.id
    override fun comId(modelo: Empresa, id: String) = modelo.copy(id = id)
}

@Singleton
class EmpresaFirestoreRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val registroCadastro: RegistroCadastro,
    @SyncScope syncScope: CoroutineScope,
    registroSincronizacao: RegistroSincronizacao,
    fonteSnapshots: FonteSnapshots,
) : EmpresaRepository {

    // O CRUD da coleção é composto, não herdado: quem os ViewModels conhecem é a porta acima, e é ela
    // que os fakes substituem. A subcoleção de atuações fica aqui, porque é da Empresa, não da coleção.
    private val colecao = ColecaoFirestore(
        codec = EmpresaCodec,
        firestore = firestore,
        fonteSnapshots = fonteSnapshots,
        syncScope = syncScope,
        registroCadastro = registroCadastro,
        registroSincronizacao = registroSincronizacao,
    )

    override fun observarTodas(): StateFlow<List<Empresa>> = colecao.observarTodos()

    override fun sincronizar() = colecao.sincronizar()

    override suspend fun salvar(empresa: Empresa): String = colecao.salvar(empresa)

    override suspend fun obterAtuacoes(empresaId: String): List<AtuacaoDaEmpresa> {
        if (empresaId.isBlank()) return emptyList()
        return try {
            atuacoesDe(empresaId).get().await().documents.mapNotNull { doc ->
                // O id do documento É o nome da atuação (ADR-0016 §4). Valor que o código não conhece
                // é descartado — fail-closed: não se inventa atuação a partir de um id qualquer.
                Atuacao.de(doc.id)?.let { atuacao ->
                    AtuacaoDaEmpresa(
                        atuacao = atuacao,
                        embarcacaoIds = (doc.get(CAMPO_EMBARCACAO_IDS) as? List<*>)
                            ?.filterIsInstance<String>()
                            .orEmpty()
                            .toSet(),
                        // A segunda dimensão da concessão (F7). Ausente = conjunto vazio = **nada
                        // concedido**, que é o fail-closed correto para uma allow-list de segurança:
                        // documento gravado antes deste campo não passa a conceder o mundo inteiro.
                        portoIds = (doc.get(CAMPO_PORTO_IDS) as? List<*>)
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
                    lote.set(
                        colecao.document(id),
                        mapOf(
                            CAMPO_EMBARCACAO_IDS to atuacao.embarcacaoIds.toList(),
                            CAMPO_PORTO_IDS to atuacao.portoIds.toList(),
                        ),
                    )
                }
                (existentes - desejadas.keys).forEach { lote.delete(colecao.document(it)) }
            }.await()
            registroCadastro.salvou(ENTIDADE_ATUACAO, empresaId)
        } catch (e: Exception) {
            registroCadastro.pendenteDeSync(ENTIDADE_ATUACAO, empresaId, e)
        }
    }

    private fun atuacoesDe(empresaId: String) =
        firestore.collection(EmpresaCodec.colecao).document(empresaId).collection(SUBCOLECAO_ATUACOES)

    override suspend fun obterTodas(): List<Empresa> = colecao.obterTodos()

    override suspend fun obterPorId(id: String): Empresa? = colecao.obterPorId(id)

    override suspend fun deletar(id: String) = colecao.deletar(id)

    private companion object {
        const val TAG = "empresaRepository"
        const val ENTIDADE_ATUACAO = "empresa.atuacoes"
        const val SUBCOLECAO_ATUACOES = "atuacoes"
        const val CAMPO_EMBARCACAO_IDS = "embarcacaoIds"
        const val CAMPO_PORTO_IDS = "portoIds"
    }
}