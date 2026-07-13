package dev.matheus.fluviapp.telemetry

import javax.inject.Inject

/**
 * Semântica de observabilidade dos cadastros do molde (ADR-0007): traduz os desfechos de
 * persistência em chamadas de telemetria, com a taxonomia do ADR-0004 (sucesso / warning / falha).
 * Pura (só depende da porta [Telemetry]), logo unit-testável com um fake, sem Firebase/Firestore.
 *
 * Fronteira offline-first: [salvou] é o sucesso durável+confirmado (Room ok e Firestore deu ack).
 * Se o Room tem o dado mas o servidor rejeitou/está offline, é [pendenteDeSync] — degradado, não
 * erro: o dado local reconcilia depois. [falhou] é o desfecho que impede a gravação (Room falhou).
 *
 * Genérica por [entidade] (ex.: "empresa", "viagem", "agente") — a mesma semântica serve os módulos.
 */
class RegistroCadastro @Inject constructor(
    private val telemetry: Telemetry,
) {

    /** SUCESSO: Room ok e Firestore confirmou (ack do servidor). */
    fun salvou(entidade: String, id: String) {
        telemetry.rastro("$entidade #$id salvo (Room + Firestore)")
        telemetry.evento(EVENTO_SALVO, mapOf(PARAM_ENTIDADE to entidade, PARAM_ID to id))
    }

    /** WARNING: Room tem o dado, mas o Firestore rejeitou/está offline — degradado, não fatal. */
    fun pendenteDeSync(entidade: String, id: String, causa: Throwable) {
        telemetry.evento(
            EVENTO_PENDENTE_SYNC,
            mapOf(PARAM_ENTIDADE to entidade, PARAM_ID to id, PARAM_MOTIVO to (causa.message ?: DESCONHECIDO)),
        )
        telemetry.naoFatal(causa, mapOf(PARAM_ENTIDADE to entidade, PARAM_ID to id))
    }

    /** FALHA: erro que impede a gravação (Room falhou / não recuperável). */
    fun falhou(entidade: String, erro: Throwable) {
        telemetry.evento(
            EVENTO_FALHA,
            mapOf(PARAM_ENTIDADE to entidade, PARAM_MOTIVO to (erro.message ?: DESCONHECIDO)),
        )
        telemetry.naoFatal(erro, mapOf(PARAM_ENTIDADE to entidade))
    }

    companion object {
        const val EVENTO_SALVO = "cadastro_salvo"
        const val EVENTO_PENDENTE_SYNC = "cadastro_pendente_sync"
        const val EVENTO_FALHA = "cadastro_falha"

        const val PARAM_ENTIDADE = "entidade"
        const val PARAM_ID = "id"
        const val PARAM_MOTIVO = "motivo"
        const val DESCONHECIDO = "desconhecido"
    }
}
