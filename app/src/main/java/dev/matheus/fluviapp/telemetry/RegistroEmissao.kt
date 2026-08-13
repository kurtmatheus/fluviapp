package dev.matheus.fluviapp.telemetry

import dev.matheus.fluviapp.exceptions.EmissaoException
import javax.inject.Inject

/**
 * Semântica de observabilidade da emissão (ADR-0004): traduz os desfechos do fluxo em
 * chamadas de telemetria — sucesso / warning / falha. Pura (só depende da porta), logo
 * unit-testável com um fake, sem Firebase nem Firestore.
 *
 * Fronteira offline-first: [aplicadaLocalmente] é o sucesso imediato. A ida ao
 * servidor é observada depois, assíncrona: [sincronizou] (ack) ou [pendenteDeSync] (rejeição).
 * Offline nenhum dos dois dispara — o dado fica local e reconcilia; isso é esperado, não erro.
 *
 * ### Por que o primeiro desfecho mudou de nome, e não de existência ([ADR-0025] D5)
 *
 * Ele se chamava `salvaLocal` e o KDoc dizia *"durável no Room"*. Sem Room, o nome perdeu referente — mas o
 * **desfecho não**: ele nunca mediu *qual banco gravou*; mede **o que o operador pode afirmar ao passageiro**
 * antes de a rede confirmar. O cache do SDK dá essa garantia como o Room dava; muda o lugar, não o fato.
 * Suprimi-lo apagaria a distinção que mais importa numa bilheteria de beira de rio: **aceito aqui × confirmado
 * no servidor**.
 */
class RegistroEmissao @Inject constructor(
    private val telemetry: Telemetry,
) {

    /** SUCESSO local: o `set` entrou no cache do SDK e **o bilhete já vale** — o SDK reconcilia. */
    fun aplicadaLocalmente(numero: String) {
        telemetry.rastro("Passagem #$numero aplicada localmente (cache do SDK)")
        telemetry.evento(EVENTO_SALVA, mapOf(PARAM_NUMERO to numero, PARAM_FASE to FASE_LOCAL))
    }

    /** SUCESSO remoto: Firestore confirmou (ack do servidor). */
    fun sincronizou(numero: String) {
        telemetry.evento(EVENTO_SINCRONIZADA, mapOf(PARAM_NUMERO to numero))
    }

    /** WARNING: o cache tem o dado, mas o servidor rejeitou a escrita ou está fora — degradado, não fatal. */
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