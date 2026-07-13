package dev.matheus.fluviapp.telemetry

import dev.matheus.fluviapp.exceptions.EmissaoException
import javax.inject.Inject

/**
 * Semântica de observabilidade da emissão (ADR-0004): traduz os desfechos do fluxo em
 * chamadas de telemetria — sucesso / warning / falha. Pura (só depende da porta), logo
 * unit-testável com um fake, sem Firebase nem Firestore.
 *
 * Fronteira offline-first: [salvaLocal] é o sucesso durável imediato (Room). A ida ao
 * servidor é observada depois, assíncrona: [sincronizou] (ack) ou [pendenteDeSync] (rejeição).
 * Offline nenhum dos dois dispara — o dado fica local e reconcilia; isso é esperado, não erro.
 */
class RegistroEmissao @Inject constructor(
    private val telemetry: Telemetry,
) {

    /** SUCESSO local: passagem durável no Room. */
    fun salvaLocal(numero: String) {
        telemetry.rastro("Passagem #$numero salva localmente (Room)")
        telemetry.evento(EVENTO_SALVA, mapOf(PARAM_NUMERO to numero, PARAM_FASE to FASE_LOCAL))
    }

    /** SUCESSO remoto: Firestore confirmou (ack do servidor). */
    fun sincronizou(numero: String) {
        telemetry.evento(EVENTO_SINCRONIZADA, mapOf(PARAM_NUMERO to numero))
    }

    /** WARNING: Room tem o dado, mas o servidor rejeitou a escrita — degradado, não fatal. */
    fun pendenteDeSync(numero: String, causa: Throwable) {
        telemetry.evento(
            EVENTO_PENDENTE_SYNC,
            mapOf(PARAM_NUMERO to numero, PARAM_MOTIVO to (causa.message ?: DESCONHECIDO)),
        )
        telemetry.naoFatal(EmissaoException.FalhaNaTransmissao(causa), mapOf(PARAM_NUMERO to numero))
    }

    /** FALHA: desfecho que impede a emissão. */
    fun falhou(erro: EmissaoException, numero: String) {
        telemetry.evento("${erro.evento}$SUFIXO_FALHA", mapOf(PARAM_NUMERO to numero))
        telemetry.naoFatal(erro, mapOf(PARAM_NUMERO to numero))
    }

    companion object {
        const val EVENTO_SALVA = "passagem_salva"
        const val EVENTO_SINCRONIZADA = "passagem_sincronizada"
        const val EVENTO_PENDENTE_SYNC = "passagem_pendente_sync"
        const val SUFIXO_FALHA = "_falha"

        const val PARAM_NUMERO = "numero"
        const val PARAM_MOTIVO = "motivo"
        const val PARAM_FASE = "fase"
        const val FASE_LOCAL = "local"
        const val DESCONHECIDO = "desconhecido"
    }
}