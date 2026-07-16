package dev.matheus.fluviapp.telemetry

import javax.inject.Inject

/**
 * Semântica de observabilidade da sincronização Firestore→Room (estudo sincronizacao-firestore-room.md,
 * §10). Pura — só depende da porta [Telemetry], logo unit-testável com um fake, sem Firebase.
 *
 * Torna "a sync funcionou?" observável em vez de anedótico:
 * - [iniciado] só dispara quando o listener é de fato anexado (passou a guarda de idempotência), então
 *   um único `iniciado` por coleção **prova que não há duplo-attach**.
 * - [snapshotRecebido] carrega `doCache` (de `SnapshotMetadata.isFromCache`) — responde se o espelho
 *   veio do servidor ou do cache local.
 * - [gravado]/[parado] fecham o ciclo (batch no Room / logout removendo a registration).
 * - [erro] é WARNING não-fatal (o Firestore reconecta sozinho).
 */
class RegistroSincronizacao @Inject constructor(
    private val telemetry: Telemetry,
) {

    /** Listener anexado (por coleção). Único por coleção ⇒ sem duplo-attach. */
    fun iniciado(colecao: String) {
        telemetry.rastro("sync $colecao: listener anexado")
        telemetry.evento(EVENTO_INICIADO, mapOf(PARAM_COLECAO to colecao))
    }

    /** Snapshot recebido: quantos docs e de onde (cache × servidor). */
    fun snapshotRecebido(colecao: String, docs: Int, doCache: Boolean) {
        val origem = if (doCache) ORIGEM_CACHE else ORIGEM_SERVIDOR
        telemetry.rastro("sync $colecao: snapshot ($docs docs, $origem)")
        telemetry.evento(
            EVENTO_SNAPSHOT,
            mapOf(PARAM_COLECAO to colecao, PARAM_DOCS to docs.toString(), PARAM_ORIGEM to origem),
        )
    }

    /** Batch gravado no Room. */
    fun gravado(colecao: String, quantidade: Int) {
        telemetry.rastro("sync $colecao: $quantidade gravados no Room")
    }

    /** Listener removido (cancelamento do escopo de sessão — logout). */
    fun parado(colecao: String) {
        telemetry.rastro("sync $colecao: listener removido")
        telemetry.evento(EVENTO_PARADO, mapOf(PARAM_COLECAO to colecao))
    }

    /** WARNING: erro do listener; não fatal, o Firestore reconecta. */
    fun erro(colecao: String, causa: Throwable) {
        telemetry.evento(
            EVENTO_ERRO,
            mapOf(PARAM_COLECAO to colecao, PARAM_MOTIVO to (causa.message ?: DESCONHECIDO)),
        )
        telemetry.naoFatal(causa, mapOf(PARAM_COLECAO to colecao))
    }

    companion object {
        const val EVENTO_INICIADO = "sync_iniciado"
        const val EVENTO_SNAPSHOT = "sync_snapshot"
        const val EVENTO_PARADO = "sync_parado"
        const val EVENTO_ERRO = "sync_erro"

        const val PARAM_COLECAO = "colecao"
        const val PARAM_DOCS = "docs"
        const val PARAM_ORIGEM = "origem"
        const val PARAM_MOTIVO = "motivo"

        const val ORIGEM_CACHE = "cache"
        const val ORIGEM_SERVIDOR = "servidor"
        const val DESCONHECIDO = "desconhecido"
    }
}
