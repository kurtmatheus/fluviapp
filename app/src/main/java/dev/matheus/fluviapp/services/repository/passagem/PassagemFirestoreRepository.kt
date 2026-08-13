package dev.matheus.fluviapp.services.repository.passagem

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import dev.matheus.fluviapp.domain.passagem.CarimboEmbarque
import dev.matheus.fluviapp.domain.passagem.Passagem
import dev.matheus.fluviapp.domain.passagem.ResultadoEmbarque
import dev.matheus.fluviapp.domain.passagem.StatusPassagem
import dev.matheus.fluviapp.domain.viagem.OcorrenciaViagem
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto
import dev.matheus.fluviapp.services.repository.firebase.documents.paraMapa
import dev.matheus.fluviapp.services.repository.firebase.documents.toPassagem
import dev.matheus.fluviapp.telemetry.RegistroEmissao
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Impl Firestore da porta [PassagemRepository] — a primeira entidade que **compõe do contrato só o codec**.
 *
 * As sete revitalizadas compõem a `ColecaoFirestore` inteira; a passagem não pode: ela não observa a coleção
 * ([ADR-0024] D9 — dado que cresce sem limite não vive num `StateFlow`, senão cada sessão baixaria tudo o que
 * já se emitiu para mostrar a venda de hoje) e **não deleta** (D11). O que ela toma do contrato compartilhado é
 * exatamente o que vale: `Map` → domínio com direito de recusa. E isso não é uma exceção inventada aqui — é o
 * que o código já fazia sem estar escrito: a passagem sempre foi a única entidade nunca espelhada por listener.
 */
@Singleton
class PassagemFirestoreRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val registroEmissao: RegistroEmissao,
) : PassagemRepository {

    /**
     * Cria o documento e devolve o id, com os **três desfechos** da telemetria ([ADR-0025] D5).
     *
     * A ida ao servidor **não é esperada**: o `set` entra no cache do SDK e o bilhete já vale — é isso que
     * `aplicadaLocalmente` afirma. Offline, nenhum dos dois listeners dispara e o SDK reconcilia depois; isso é
     * esperado, não erro.
     */
    override suspend fun emitir(passagem: Passagem): String {
        val documento = firestore.collection(COLECAO).document()
        val numero = passagem.numero

        documento.set(passagem.paraMapa())
            .addOnSuccessListener { registroEmissao.sincronizou(numero) }
            .addOnFailureListener { erro -> registroEmissao.pendenteDeSync(numero, erro) }

        registroEmissao.aplicadaLocalmente(numero)
        return documento.id
    }

    override suspend fun obterPorId(id: String): Passagem? =
        runCatching { firestore.collection(COLECAO).document(id).get().await() }
            .getOrNull()
            ?.toPassagemDoDominio()

    /** `Source.SERVER`: o QR pode chegar num aparelho que nunca viu o bilhete, e o cache mentiria por omissão. */
    override suspend fun obterDoServidorPorId(id: String): Passagem? =
        runCatching { firestore.collection(COLECAO).document(id).get(Source.SERVER).await() }
            .getOrNull()
            ?.toPassagemDoDominio()

    /**
     * A consulta recortada. O plano é traduzido por função pura ([CriterioPassagem.traduzir]); aqui só se
     * aplica cada filtro à `Query` — que é a única linha deste arquivo que conhece o Firebase.
     *
     * [PlanoDeConsulta.SemResultado] **não vira consulta**: quem não tem empresa nenhuma não pergunta nada.
     */
    override suspend fun consultar(criterio: CriterioPassagem): List<Passagem> {
        val plano = criterio.traduzir()
        if (plano !is PlanoDeConsulta.Filtrada) return emptyList()

        val consulta = plano.filtros.fold(firestore.collection(COLECAO) as Query) { query, filtro ->
            when (filtro) {
                is FiltroPassagem.Igual -> query.whereEqualTo(filtro.campo, filtro.valor)
                is FiltroPassagem.ContemNoArray -> query.whereArrayContains(filtro.campo, filtro.valor)
                is FiltroPassagem.NaFaixa -> query
                    .whereGreaterThanOrEqualTo(filtro.campo, filtro.de)
                    .whereLessThanOrEqualTo(filtro.campo, filtro.ate)
            }
        }

        return runCatching { consulta.get().await() }
            .onFailure { Log.e(TAG, "consultar: ${it.message}", it) }
            .getOrNull()
            ?.documents
            ?.mapNotNull { it.toPassagemDoDominio() }
            .orEmpty()
    }

    /**
     * Transição como máquina de estados, fail-closed e idempotente: só aplica arestas legais, e repetir a
     * transição já aplicada é no-op. Transição ilegal é **recusada com log**, sem quebrar o chamador.
     *
     * Lê do **servidor** antes de decidir: a guarda existe para impedir estado ilegal, e um cache atrasado a
     * faria decidir sobre um status que já mudou noutro aparelho.
     */
    override suspend fun transicionar(id: String, novo: StatusPassagem) {
        val passagem = obterDoServidorPorId(id) ?: return
        val atual = passagem.metadados.status
        if (atual == novo) return
        if (!atual.podeTransicionarPara(novo)) {
            Log.w(TAG, "transicao ilegal ignorada: $atual -> $novo (id=$id)")
            return
        }

        firestore.collection(COLECAO).document(id)
            .update(mapOf(CAMPO_STATUS to novo.name, CAMPO_ALTERADO_EM to agoraIso()))
            .await()
    }

    /**
     * Confirma o embarque (ADR-0012): lê ao vivo, valida a aresta `EMITIDA → EMBARCADA` e carimba **o uid** de
     * quem leu o QR — é ele que a regra do servidor confere contra `request.auth.uid`, e é o que torna forjar
     * autoria impossível.
     *
     * O carimbo vai como **sub-objeto inteiro**: `porId` e `em` juntos, num instante **ISO** que ordena — o que
     * torna respondível a pergunta *"quem embarcou entre tal e tal hora"*, que o formato antigo
     * (`dd/MM/yyyy HH:mm`) não permitia responder.
     */
    override suspend fun confirmarEmbarque(id: String, operadorId: String): ResultadoEmbarque {
        val passagem = obterDoServidorPorId(id) ?: return ResultadoEmbarque.NaoEncontrada
        val atual = passagem.metadados.status

        if (atual == StatusPassagem.EMBARCADA) {
            val carimbo = passagem.metadados.embarque ?: CarimboEmbarque(porId = "", em = "")
            return ResultadoEmbarque.JaEmbarcada(carimbo)
        }
        if (!atual.podeTransicionarPara(StatusPassagem.EMBARCADA)) return ResultadoEmbarque.NaoEmitida

        val agora = agoraIso()
        val carimbo = CarimboEmbarque(porId = operadorId, em = agora)

        return runCatching {
            firestore.collection(COLECAO).document(id).update(
                mapOf(
                    CAMPO_STATUS to StatusPassagem.EMBARCADA.name,
                    CAMPO_EMBARQUE to mapOf("porId" to carimbo.porId, "em" to carimbo.em),
                    CAMPO_ALTERADO_EM to agora,
                ),
            ).await()

            val embarcada = passagem.comEmbarque(carimbo)
            ResultadoEmbarque.Confirmada(embarcada) as ResultadoEmbarque
        }.getOrElse { erro ->
            Log.e(TAG, "confirmarEmbarque($id): ${erro.message}", erro)
            ResultadoEmbarque.NaoEncontrada
        }
    }

    /**
     * Reserva o próximo número da ocorrência em `viagens/{viagemId}/ocorrencias/{data}` ([ADR-0024] D6).
     *
     * ### Por que transação, e não `update(increment)` solto
     *
     * O `increment` é atômico mas **não devolve o valor**, e quem emite precisa do número para imprimi-lo. Ler
     * depois de incrementar reabriria exatamente a corrida que se quer fechar. A transação faz as duas coisas
     * numa operação e **lê do servidor** — que é o que corrige o defeito de hoje: o número saía do cache local,
     * era gravado depois, e havia um `runBlocking` no meio. Dois caixas vendendo ao mesmo tempo recebiam o
     * mesmo número.
     *
     * ### O que isso custa, e é honesto declarar
     *
     * **Reservar número exige rede.** A escrita da passagem continua otimista (o `set` do [emitir] funciona
     * offline e reconcilia), mas a numeração não pode ser: um número inventado localmente colidiria com o de
     * outra bilheteria, e um bilhete com número repetido é pior do que um bilhete que esperou. Como a emissão
     * offline é a razão de o desfecho `aplicadaLocalmente` existir, **o que fazer quando não há rede na hora de
     * numerar é decisão do fluxo de emissão** — e ele é a F9.4, não esta fatia.
     *
     * O documento **não persiste a ocorrência**: ele guarda o último número e nada mais. A ausência dele
     * significa que **ninguém vendeu** — não que a saída não exista. Quem responde *"esta saída existe?"*
     * continua sendo o cálculo sobre `(diaSemana, hora)`.
     */
    override suspend fun reservarNumero(ocorrencia: OcorrenciaViagem): Int {
        val documento = firestore.collection(COLECAO_VIAGENS)
            .document(ocorrencia.viagemId)
            .collection(SUBCOLECAO_OCORRENCIAS)
            .document(ocorrencia.dataIso)

        return firestore.runTransaction { transacao ->
            val atual = (transacao.get(documento).get(CAMPO_ULTIMO_NUMERO) as? Number)?.toInt() ?: 0
            val proximo = atual + 1
            transacao.set(documento, mapOf(CAMPO_ULTIMO_NUMERO to proximo))
            proximo
        }.await()
    }

    private fun DocumentSnapshot.toPassagemDoDominio(): Passagem? {
        val dados = data ?: return null
        return DocumentoBruto(id = id, dados = dados).toPassagem()
    }

    /** O carimbo aplicado ao agregado — sem `copy` no tipo selado, que não o tem. */
    private fun Passagem.comEmbarque(carimbo: CarimboEmbarque): Passagem = when (this) {
        is dev.matheus.fluviapp.domain.passagem.PassagemDePassageiro ->
            copy(metadados = metadados.copy(status = StatusPassagem.EMBARCADA, embarque = carimbo))

        is dev.matheus.fluviapp.domain.passagem.PassagemDeVeiculo ->
            copy(metadados = metadados.copy(status = StatusPassagem.EMBARCADA, embarque = carimbo))
    }

    private fun agoraIso(): String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

    private companion object {
        const val TAG = "passagemFirestoreRepository"
        const val COLECAO = "passagens"
        const val COLECAO_VIAGENS = "viagens"
        const val SUBCOLECAO_OCORRENCIAS = "ocorrencias"
        const val CAMPO_ULTIMO_NUMERO = "ultimoNumero"
        const val CAMPO_EMBARQUE = "embarque"
        const val CAMPO_ALTERADO_EM = "alteradoEm"
    }
}