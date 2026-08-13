package dev.matheus.fluviapp.services.repository.pool

import android.util.Log
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import dev.matheus.fluviapp.services.repository.firebase.CodecFirestore
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto
import dev.matheus.fluviapp.telemetry.RegistroCadastro
import kotlinx.coroutines.tasks.await

/**
 * **O que um pool tem de diferente de uma coleção** ([ADR-0018] D2/D3, [ADR-0025] D6) — escrito uma vez.
 *
 * As sete entidades revitalizadas compõem a `ColecaoFirestore`: coleção pequena, observada inteira num
 * `StateFlow`. `clientes` e `veiculos` **não podem** — crescem sem limite, e é a mesma razão pela qual a
 * passagem não se observa ([ADR-0024] D9). O que muda, além disso, são três coisas que nenhuma das sete
 * precisou:
 *
 * 1. **criar-ou-assinar**, que funciona por tentativa e queda (ver [criarOuAssinar]);
 * 2. **leitura por ids em lote**, que é o segundo regime da junção ([ADR-0025] D3) e precisa particionar;
 * 3. **consulta recortada por assinatura**, que substitui o `getListaNome()` que devolvia lista vazia.
 *
 * Escrever isso duas vezes seria criar duas chances de errar o mesmo detalhe — o argumento que criou a
 * `ColecaoFirestore`, aplicado ao caso que ela não atende.
 *
 * @param chaveNaturalDe a identidade do pool, que é **também o id do documento**. Ver [criarOuAssinar].
 * @param comAssinatura o modelo com a agência acrescentada às assinaturas — só usado na criação.
 * @param campoDeOrdenacao por onde a listagem da agência sai ordenada (`nome`, `placa`).
 */
class PoolFirestore<T>(
    private val codec: CodecFirestore<T>,
    private val firestore: FirebaseFirestore,
    private val registroCadastro: RegistroCadastro,
    private val chaveNaturalDe: (T) -> String,
    private val comAssinatura: (T, String) -> T,
    private val campoDeOrdenacao: String,
) {

    /**
     * **Tenta criar; se o servidor recusar, assina.** Devolve o id — que é a chave natural.
     *
     * ### Por que não se procura antes
     *
     * Porque **quem emite não lê o que ainda não assinou** ([ADR-0018] D3): a leitura do pool é recortada por
     * `agenciaIds`, então uma busca prévia responderia *"não existe"* para uma pessoa que existe — e o pool
     * ganharia uma entrada por agência, desfazendo a decisão de que uma pessoa é **um** documento.
     *
     * Quem sabe se a entrada existe é o **servidor**, e é a regra que responde: `create` é permitido quando
     * `resource == null`, e `update` só admite o `arrayUnion` da assinatura. Então a escrita completa num
     * documento que já existe volta como `PERMISSION_DENIED` — e essa recusa **não é um erro**, é a resposta:
     * *já existe, então assine*. São **duas escritas no pior caso**, e esta é a única operação do app que
     * funciona assim.
     *
     * ### O que isso cobra
     *
     * **Rede.** Uma escrita otimista não serve aqui, porque a decisão entre criar e assinar depende do que só
     * o servidor enxerga; offline, a `Task` do `set` fica pendente até haver conexão. É o oposto da emissão da
     * passagem, que é otimista de propósito — e a diferença tem razão: o bilhete é um fato local que o SDK
     * reconcilia, enquanto o pool é uma identidade **compartilhada**, e reconciliar identidade depois é como
     * duplicatas nascem.
     */
    suspend fun criarOuAssinar(modelo: T, agenciaId: String): String {
        val chave = chaveNaturalDe(modelo)
        val documento = firestore.collection(codec.colecao).document(chave)
        val comId = codec.comId(comAssinatura(modelo, agenciaId), chave)

        try {
            documento.set(codec.paraMapa(comId)).await()
            registroCadastro.salvou(codec.entidade, chave)
        } catch (e: FirebaseFirestoreException) {
            if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                registroCadastro.pendenteDeSync(codec.entidade, chave, e)
                return chave
            }
            assinar(documento.path, chave, agenciaId)
        } catch (e: Exception) {
            registroCadastro.pendenteDeSync(codec.entidade, chave, e)
        }
        return chave
    }

    /**
     * A queda do [criarOuAssinar]: acrescenta a agência às assinaturas de quem já existe.
     *
     * `arrayUnion` porque assinar duas vezes é o mesmo que assinar uma — e porque é a **única** alteração que
     * a regra concede a quem não é da plataforma. Corrigir conteúdo é curadoria, e ela mora no painel.
     */
    private suspend fun assinar(caminho: String, chave: String, agenciaId: String) {
        try {
            firestore.document(caminho).update(CAMPO_ASSINATURAS, FieldValue.arrayUnion(agenciaId)).await()
            registroCadastro.salvou(codec.entidade, chave)
        } catch (e: Exception) {
            registroCadastro.pendenteDeSync(codec.entidade, chave, e)
        }
    }

    suspend fun obterPorId(id: String): T? =
        runCatching { firestore.collection(codec.colecao).document(id).get().await() }
            .getOrNull()
            ?.let { snapshot -> snapshot.data?.let { codec.deDocumento(DocumentoBruto(snapshot.id, it)) } }

    /**
     * Leitura por ids, **em lote** — o segundo regime da junção ([ADR-0025] D3).
     *
     * Particiona porque o `whereIn` do Firestore aceita **30 valores por consulta**; é limite de plataforma,
     * não escolha. Ids repetidos são reduzidos antes (uma passagem de duas pessoas pode citar o mesmo
     * responsável duas vezes), e o que não existir simplesmente não volta — quem chama junta pelo id.
     */
    suspend fun obterPorIds(ids: List<String>): List<T> {
        val distintos = ids.filter { it.isNotBlank() }.distinct()
        if (distintos.isEmpty()) return emptyList()

        return distintos.chunked(LIMITE_WHERE_IN).flatMap { pedaco ->
            runCatching {
                firestore.collection(codec.colecao)
                    .whereIn(FieldPath.documentId(), pedaco)
                    .get()
                    .await()
            }
                .onFailure { Log.e(TAG, "obterPorIds(${codec.colecao}): ${it.message}", it) }
                .getOrNull()
                ?.documents
                ?.mapNotNull { doc -> doc.data?.let { codec.deDocumento(DocumentoBruto(doc.id, it)) } }
                .orEmpty()
        }
    }

    /**
     * O que **esta agência** já atendeu — a consulta que substitui o `getListaNome()` que devolvia lista
     * vazia, e que é o motivo de a assinatura existir: *"assim a agência não pega o `listaNome` de todo
     * mundo, pode onerar"*.
     *
     * `array-contains` combinado com ordenação **exige índice composto**, e ele entra na mesma fatia — não
     * depois. Sem o índice, a consulta falha em produção com um erro que só aparece quando alguém abre a tela.
     */
    suspend fun consultarDaAgencia(agenciaId: String): List<T> {
        if (agenciaId.isBlank()) return emptyList()

        return runCatching {
            firestore.collection(codec.colecao)
                .whereArrayContains(CAMPO_ASSINATURAS, agenciaId)
                .orderBy(campoDeOrdenacao)
                .get()
                .await()
        }
            .onFailure { Log.e(TAG, "consultarDaAgencia(${codec.colecao}): ${it.message}", it) }
            .getOrNull()
            ?.documents
            ?.mapNotNull { doc -> doc.data?.let { codec.deDocumento(DocumentoBruto(doc.id, it)) } }
            .orEmpty()
    }

    private companion object {
        const val TAG = "poolFirestore"
        const val CAMPO_ASSINATURAS = "agenciaIds"

        /** Limite do `whereIn` no Firestore. Plataforma, não escolha — por isso [obterPorIds] particiona. */
        const val LIMITE_WHERE_IN = 30
    }
}