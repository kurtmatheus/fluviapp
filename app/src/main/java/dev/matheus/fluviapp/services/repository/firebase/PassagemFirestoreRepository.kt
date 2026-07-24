package dev.matheus.fluviapp.services.repository.firebase

import android.util.Log
import dev.matheus.fluviapp.database.dao.ContadorDao
import dev.matheus.fluviapp.database.dao.passagem.PassagemDao
import dev.matheus.fluviapp.exceptions.EmissaoException
import dev.matheus.fluviapp.extensions.isTextoNaoNulo
import dev.matheus.fluviapp.extensions.toPassagemDocumento
import dev.matheus.fluviapp.model.ContadorBilhete
import dev.matheus.fluviapp.model.operacoes.Usuario
import dev.matheus.fluviapp.model.passagem.Passagem
import dev.matheus.fluviapp.model.passagem.ResultadoEmbarque
import dev.matheus.fluviapp.model.passagem.StatusPassagem
import dev.matheus.fluviapp.services.repository.firebase.documents.PassagemDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.toContadorBilhete
import dev.matheus.fluviapp.services.repository.firebase.documents.toContadorDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.toPassagem
import dev.matheus.fluviapp.telemetry.RegistroEmissao
import dev.matheus.fluviapp.telemetry.RegistroSincronizacao
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.toObject
import dev.matheus.fluviapp.di.module.SyncScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PassagemFirestoreRepository @Inject constructor(
    private val dao: PassagemDao,
    private val contadorDao: ContadorDao,
    private val firestore: FirebaseFirestore,
    private val registroEmissao: RegistroEmissao,
    @SyncScope private val syncScope: CoroutineScope,
    private val registroSincronizacao: RegistroSincronizacao,
    private val fonteSnapshots: FonteSnapshots,
) {

    private var contadorJob: Job? = null

    // Contador de bilhete como Flow gerenciado (estudo sync, D2/D3): idempotente (mata o duplo-attach
    // Login+Main), grava sem runBlocking e NÃO lança dentro do callback (o `throw` antigo derrubava o
    // app). awaitClose remove a registration quando o escopo de sessão é cancelado (logout).
    fun sincronizarNumeroBilheteEmTempoReal() {
        if (contadorJob?.isActive == true) return
        registroSincronizacao.iniciado(COLECAO_CONTADOR)
        contadorJob = fonteSnapshots.observarDocumento(COLLECTION_PASSAGENS, DOCUMENT_CONTADOR)
            .onEach { resultado ->
                when (resultado) {
                    is ResultadoDocumento.Dados -> {
                        registroSincronizacao.snapshotRecebido(
                            COLECAO_CONTADOR,
                            docs = if (resultado.documento != null) 1 else 0,
                            doCache = resultado.doCache,
                        )
                        resultado.documento?.toContadorDocumento()?.toContadorBilhete()?.let {
                            contadorDao.atualizarContagem(ContadorBilhete(contagem = it.contagem))
                            registroSincronizacao.gravado(COLECAO_CONTADOR, 1)
                        }
                    }

                    is ResultadoDocumento.Falha -> registroSincronizacao.erro(COLECAO_CONTADOR, resultado.causa)
                }
            }
            .onCompletion { registroSincronizacao.parado(COLECAO_CONTADOR) }
            .launchIn(syncScope)
    }

    suspend fun salvar(id: String, passagem: Passagem): String {
        val documento = retornaDocumentReference(id)
        val passagemComIdNumeroBilhete = passagem.copy(id = documento.id)
        val numero = passagemComIdNumeroBilhete.numero

        // volátil/cacheada -> sólida local (Room). Sucesso durável imediato.
        val passagemDocumento = try {
            dao.salvar(passagemComIdNumeroBilhete)
            passagemComIdNumeroBilhete.toPassagemDocumento()
        } catch (e: Exception) {
            Log.e(TAG, "salvar: Exception: ${e.message}")
            registroEmissao.falhou(EmissaoException.FalhaAoPersistir(e), numero)
            throw RuntimeException("Falha no Processo: ${e.message}")
        }
        registroEmissao.salvaLocal(numero)

        // Transmissão observada de forma não-bloqueante: offline apenas enfileira (nenhum
        // listener dispara); ack -> sincronizou; rejeição do servidor -> pendenteDeSync (warning).
        documento.set(passagemDocumento)
            .addOnSuccessListener { registroEmissao.sincronizou(numero) }
            .addOnFailureListener { e -> registroEmissao.pendenteDeSync(numero, e) }

        try {
            adicionarContador(id, numero)
        } catch (e: Exception) {
            Log.e(TAG, "salvar/contador: Exception: ${e.message}")
            registroEmissao.falhou(EmissaoException.NumeroIndisponivel(e.message ?: "contador"), numero)
            throw RuntimeException("Falha no Processo: ${e.message}")
        }
        return documento.id
    }

    private fun adicionarContador(id: String, numero: String) {
        if (!id.isTextoNaoNulo()) {
            runBlocking {
                atualizarContador(numero.toInt())
            }
        }
    }

    private fun retornaDocumentReference(id: String): DocumentReference {
        return if (id.isTextoNaoNulo()) {
            firestore.collection(COLLECTION_PASSAGENS).document(id)
        } else firestore.collection(COLLECTION_PASSAGENS).document()
    }

    suspend fun obterPorId(id: String) = dao.obterPorId(id).first()

    fun deletar(id: String) {
        try {
            firestore.collection(COLLECTION_PASSAGENS)
                .document(id)
                .delete()
        } catch (e: Exception) {
            Log.e(TAG, "deletar: Exception: ${e.message}")
            throw RuntimeException("Falha no Processo: ${e.message}")
        }
    }

    fun obterTodasPorData(data: String): Task<QuerySnapshot> {
        return firestore.collection(COLLECTION_PASSAGENS)
            .whereEqualTo(FIELD_DATA_VIAGEM, data)
            .get()
    }

    /**
     * Conta as passagens de uma viagem com a gratuidade da categoria dada (ADR-0013 §8, cota por viagem).
     * Firestore-driven: lê a coleção ao vivo. Consulta por `viagemId` (índice simples, sem composto) e
     * filtra a categoria em memória. `excetoId` exclui o próprio bilhete (edição não conta a si mesmo).
     * Ressalva de concorrência conhecida (não é atômico com a emissão).
     */
    suspend fun contarGratuidadePorViagem(viagemId: String, gratuidade: String, excetoId: String): Int {
        val snapshot = firestore.collection(COLLECTION_PASSAGENS)
            .whereEqualTo(FIELD_VIAGEM_ID, viagemId)
            .get()
            .await()
        return snapshot.documents.count {
            it.id != excetoId && it.getString(FIELD_GRATUIDADE) == gratuidade
        }
    }

    fun getListaNome(): List<String> {
        return emptyList()
    }

    suspend fun obterContagem() = contadorDao.obterContagem().first()

    fun obterTodasPorDataStatus(data: String, status: String, nomeFuncionario: String): Task<QuerySnapshot> {
        // Canoniza o filtro (ADR-0012): o dropdown traz o rótulo ("A EMITIR"), mas o status é gravado
        // pelo .name do tipo ("A_EMITIR"). Sem isso a query não casaria com o armazenamento canônico.
        val statusCanonico = StatusPassagem.de(status)?.name ?: status
        val queryStatusData = firestore.collection(COLLECTION_PASSAGENS)
            .whereEqualTo(FIELD_DATA_VIAGEM, data)
            .whereEqualTo(FIELD_STATUS, statusCanonico)
        return if (nomeFuncionario != Usuario.GERAL) {
            queryStatusData
                .whereEqualTo(FIELD_NOME_FUNC, nomeFuncionario)
                .get()
                .addOnSuccessListener { snapshot ->
                    snapshot.documents.mapNotNull { document ->
                        document.toObject<PassagemDocumento>()?.toPassagem(document.id)
                    }.forEach {
                        runBlocking { dao.salvar(it) }
                    }
                }
        } else {
            queryStatusData
                .get()
                .addOnSuccessListener { snapshot ->
                snapshot.documents.mapNotNull { document ->
                    document.toObject<PassagemDocumento>()?.toPassagem(document.id)
                }.forEach {
                    runBlocking { dao.salvar(it) }
                }
            }
        }
    }

    /**
     * Transição de status como máquina de estados (ADR-0012). Fail-closed: só aplica arestas legais
     * (`A_EMITIR→EMITIDA→EMBARCADA`); idempotente (reimpressão do já EMITIDA é no-op); transição ilegal
     * é ignorada com log, sem quebrar o fluxo do chamador. Espelha no Room também (SSOT, ADR-0009) —
     * o antigo `atualizarSituacao` só tocava o Firestore.
     */
    suspend fun transicionar(idPassagem: String, novo: StatusPassagem) {
        val passagem = obterPorId(idPassagem)
        val atual = StatusPassagem.de(passagem.status)
        if (atual == novo) return
        if (atual == null || !atual.podeTransicionarPara(novo)) {
            Log.w(TAG, "transicao ilegal ignorada: $atual -> $novo (id=$idPassagem)")
            return
        }
        dao.salvar(passagem.copy(status = novo.name))
        firestore.collection(COLLECTION_PASSAGENS)
            .document(idPassagem)
            .update(mapOf(FIELD_STATUS to novo.name))
    }

    /**
     * Lê a passagem AO VIVO do Firestore pelo id (ADR-0012): o QR é ponteiro, então o embarque valida
     * contra o servidor (fonte de verdade), não contra o espelho Room — que pode não ter o bilhete
     * emitido em outro device. `null` se o doc não existir.
     */
    suspend fun obterDoServidorPorId(id: String): Passagem? {
        val snapshot = firestore.collection(COLLECTION_PASSAGENS).document(id).get().await()
        return snapshot.toObject<PassagemDocumento>()?.toPassagem(snapshot.id)
    }

    /**
     * Confirma o embarque (ADR-0012): lê ao vivo, valida a aresta EMITIDA→EMBARCADA (FSM, fail-closed
     * e idempotente) e carimba o operador que validou o QR (id + nome snapshot + quando). Grava o
     * Firestore (fronteira) e espelha o Room best-effort. Retorna o caso p/ a UI reagir.
     */
    suspend fun confirmarEmbarque(idPassagem: String, operadorId: String, operadorNome: String): ResultadoEmbarque {
        val passagem = obterDoServidorPorId(idPassagem) ?: return ResultadoEmbarque.NaoEncontrada
        val atual = StatusPassagem.de(passagem.status)
        return when {
            atual == StatusPassagem.EMBARCADA ->
                ResultadoEmbarque.JaEmbarcada(passagem.embarcadaPor, passagem.embarcadaEm)
            atual == null || !atual.podeTransicionarPara(StatusPassagem.EMBARCADA) ->
                ResultadoEmbarque.NaoEmitida
            else -> {
                val agora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date())
                val embarcada = passagem.copy(
                    status = StatusPassagem.EMBARCADA.name,
                    embarcadaPorId = operadorId,
                    embarcadaPor = operadorNome,
                    embarcadaEm = agora,
                )
                firestore.collection(COLLECTION_PASSAGENS).document(idPassagem).update(
                    mapOf(
                        FIELD_STATUS to embarcada.status,
                        FIELD_EMBARCADA_POR_ID to operadorId,
                        FIELD_EMBARCADA_POR to operadorNome,
                        FIELD_EMBARCADA_EM to agora,
                    )
                ).await()
                runCatching { dao.salvar(embarcada) } // espelho local best-effort (pode não existir)
                ResultadoEmbarque.Confirmada(embarcada)
            }
        }
    }

    private fun atualizarContador(numero: Int) {
        firestore.collection(COLLECTION_PASSAGENS)
            .document(DOCUMENT_CONTADOR)
            .update(mapOf(FIELD_NUMERO to numero))
    }

    companion object {
        private const val TAG = "passagemFirestoreRepository"
        private const val COLLECTION_PASSAGENS = "passagens"
        private const val DOCUMENT_CONTADOR = "contador"
        private const val COLECAO_CONTADOR = "contador_bilhete"
        private const val FIELD_NUMERO = "numeroBilhete"
        private const val FIELD_DATA_VIAGEM = "dataViagem"
        private const val FIELD_VIAGEM_ID = "viagemId"
        private const val FIELD_GRATUIDADE = "gratuidade"
        private const val FIELD_STATUS = "status"
        private const val FIELD_NOME_FUNC = "funcionarioResponsavel"
        private const val FIELD_EMBARCADA_POR_ID = "embarcadaPorId"
        private const val FIELD_EMBARCADA_POR = "embarcadaPor"
        private const val FIELD_EMBARCADA_EM = "embarcadaEm"
    }
}