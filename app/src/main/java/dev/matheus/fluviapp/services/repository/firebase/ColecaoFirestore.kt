package dev.matheus.fluviapp.services.repository.firebase

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import dev.matheus.fluviapp.telemetry.RegistroCadastro
import dev.matheus.fluviapp.telemetry.RegistroSincronizacao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

/**
 * O que **cada entidade** precisa declarar para viver numa coleção do Firestore — e só isso.
 *
 * Tudo o mais é igual entre coleções, e é o que a [ColecaoFirestore] passa a guardar num lugar só.
 */
interface CodecFirestore<T> {
    /** Nome da coleção no Firestore. */
    val colecao: String

    /** Como a entidade aparece na telemetria de cadastro. */
    val entidade: String

    /**
     * `Map` → domínio na leitura (ADR-0019 D2). **`null` = este documento não é uma entidade minha** e
     * fica de fora da coleção.
     *
     * A recusa é do codec porque só ele conhece o invariante: a Embarcação, por exemplo, não existe sem
     * tipo, e um documento que não o declara não vira embarcação de tipo padrão — não vira nada. Quem não
     * tem invariante nenhum devolve sempre o modelo (a Empresa, hoje) e nem percebe a nulidade.
     *
     * Uma recusa **não derruba a coleção**: o snapshot é mapeado com `mapNotNull`, e a diferença entre
     * `snapshotRecebido` e `gravado` na telemetria conta quantos documentos foram recusados.
     */
    fun deDocumento(bruto: DocumentoBruto): T?

    /** Domínio → `Map` na escrita. O `id` **não entra**: ele é o nome do documento, não um campo dele. */
    fun paraMapa(modelo: T): Map<String, Any?>

    fun id(modelo: T): String

    /** Devolve a entidade com o id que a criação gerou. */
    fun comId(modelo: T, id: String): T
}

/**
 * O CRUD de uma coleção do Firestore, escrito **uma vez** (ADR-0017 D1, ADR-0019 D2).
 *
 * ### Por que existe
 *
 * O primeiro repositório sem Room (Empresa) trouxe quatro comportamentos que não são óbvios e que seriam
 * **copiados** a cada entidade revitalizada:
 *
 * 1. a fonte reativa é um [StateFlow] em memória alimentado pelo listener — o cache do SDK já persiste
 *    entre execuções, e manter Room ao lado era pagar duas persistências para ter uma;
 * 2. a leitura **espera o primeiro snapshot** em vez de devolver lista vazia. Sem isso, trocar Room por
 *    memória transformaria *"ainda não chegou"* em *"não existe"* — a mesma confusão que, no login,
 *    fazia o app acusar a pessoa de não ser da casa quando o problema era rede;
 * 3. a escrita é otimista **porque o SDK já a faz**: `set()` grava no cache e emite pelo listener antes
 *    de falar com o servidor. Rejeição vira `pendenteDeSync` e **não relança** — o dado local já está
 *    aplicado e o SDK reconcilia;
 * 4. id gerado × id existente decide entre `document()` e `document(id)`.
 *
 * Copiar isso por entidade é criar uma chance de errar por entidade. Aqui é uma implementação e um teste.
 *
 * ### O que NÃO entra aqui
 *
 * O que é próprio de cada entidade: subcoleções (as `atuacoes` da Empresa), consultas específicas
 * (`obterPorNome`, `obterPorEmailDoServidor`) e qualquer regra de negócio. O repositório concreto
 * **compõe** esta classe em vez de herdá-la, e continua implementando a própria porta — é a porta que os
 * ViewModels conhecem, e é ela que os fakes substituem nos testes.
 */
class ColecaoFirestore<T>(
    private val codec: CodecFirestore<T>,
    private val firestore: FirebaseFirestore,
    private val fonteSnapshots: FonteSnapshots,
    private val syncScope: CoroutineScope,
    private val registroCadastro: RegistroCadastro,
    private val registroSincronizacao: RegistroSincronizacao,
) {

    private val _itens = MutableStateFlow<List<T>>(emptyList())

    /** `true` depois do primeiro snapshot — distingue "coleção vazia" de "ainda não chegou". */
    private val _recebeuSnapshot = MutableStateFlow(false)

    private var syncJob: Job? = null

    fun observarTodos(): StateFlow<List<T>> = _itens.asStateFlow()

    /** Idempotente: chamar duas vezes não anexa dois listeners. */
    fun sincronizar() {
        if (syncJob?.isActive == true) return
        syncJob = sincronizarColecao(
            fonte = fonteSnapshots,
            colecao = codec.colecao,
            scope = syncScope,
            registro = registroSincronizacao,
            paraModelo = { codec.deDocumento(it) },
            salvarTodos = { itens ->
                _itens.value = itens
                _recebeuSnapshot.value = true
            },
        )
    }

    /** Espera o primeiro snapshot: vazio aqui significa *vazio*, não *ainda não chegou*. */
    suspend fun obterTodos(): List<T> {
        sincronizar()
        _recebeuSnapshot.first { it }
        return _itens.value
    }

    suspend fun obterPorId(id: String): T? = obterTodos().firstOrNull { codec.id(it) == id }

    /** Salva e devolve **o id** — necessário porque a criação o gera, e subcoleções penduram nele. */
    suspend fun salvar(modelo: T): String {
        val documento = if (codec.id(modelo).isBlank()) {
            firestore.collection(codec.colecao).document()
        } else {
            firestore.collection(codec.colecao).document(codec.id(modelo))
        }
        val comId = codec.comId(modelo, documento.id)

        try {
            documento.set(codec.paraMapa(comId)).await()
            registroCadastro.salvou(codec.entidade, documento.id)
        } catch (e: Exception) {
            registroCadastro.pendenteDeSync(codec.entidade, documento.id, e)
        }
        return documento.id
    }

    suspend fun deletar(id: String) {
        try {
            firestore.collection(codec.colecao).document(id).delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "deletar(${codec.colecao}/$id): ${e.message}", e)
        }
    }

    private companion object {
        const val TAG = "colecaoFirestore"
    }
}