package dev.matheus.fluviapp.telemetry

import javax.inject.Inject

/**
 * Semântica de observabilidade do **embarque** ([ADR-0032] D4) — a validação do bilhete na doca.
 *
 * Nasce em 2026-09-07 para fechar um buraco que o estudo mediu: **confirmar embarque não emitia evento
 * nenhum**. O rastro existia, mas só **dentro do documento** (`CarimboEmbarque.porId`), o que responde
 * *quem validou aquele bilhete* e não responde *quantos embarques houve hoje* — para saber isso era
 * preciso varrer as passagens.
 *
 * ### Por que registrador próprio, e não um método no [RegistroEmissao]
 *
 * Porque são atos diferentes sobre o mesmo agregado, e a distinção aparece em quem os pratica: emitir é
 * da bilheteria, embarcar é da doca — o [ADR-0032] D1 registra que *"qualquer papel conhecido valida o
 * QR, mesmo sem ter vendido"*. Misturá-los faria a contagem de um poluir a do outro.
 *
 * ### O que ele **não** carrega
 *
 * O uid de quem carimbou. Quem fez está no documento; aqui o operador entra como **coordenada** (agência,
 * papel, cargo), empurrada por [CoordenadasDaSessao] e igual para todo evento. *O evento conta, o
 * documento prova.*
 */
class RegistroEmbarque @Inject constructor(
    private val telemetry: Telemetry,
) {

    /** O bilhete foi validado e a passagem transicionou para `EMBARCADA`. */
    fun confirmado(numero: String) {
        telemetry.rastro("Embarque confirmado para a passagem #$numero")
        telemetry.evento(EVENTO_CONFIRMADO, mapOf(PARAM_NUMERO to numero))
    }

    /**
     * O bilhete foi lido e **não** embarcou, com o motivo tipado.
     *
     * Recusa não é erro: bilhete já usado é o antifraude funcionando, e bilhete não emitido é gente
     * chegando à doca com o que ainda não pagou. O que interessa é **quantas** e **de que tipo** — por
     * isso o motivo é parâmetro, e não um evento por desfecho.
     */
    fun recusado(motivo: String) {
        telemetry.evento(EVENTO_RECUSADO, mapOf(PARAM_MOTIVO to motivo))
    }

    /**
     * **A escrita falhou** — distinto de [recusado], e a distinção é a que importa na doca.
     *
     * Recusar é o sistema funcionando: o bilhete não valia. Falhar é o sistema não funcionando: o bilhete
     * valia e o embarque **não aconteceu**, e quem está na fila não tem como saber a diferença. Por isso
     * este é o único dos três que vira não-fatal.
     */
    fun falhou(erro: Throwable) {
        telemetry.evento(EVENTO_FALHA, mapOf(PARAM_MOTIVO to (erro.message ?: DESCONHECIDO)))
        telemetry.naoFatal(erro)
    }

    companion object {
        const val EVENTO_CONFIRMADO = "embarque_confirmado"
        const val EVENTO_RECUSADO = "embarque_recusado"
        const val EVENTO_FALHA = "embarque_falha"
        const val DESCONHECIDO = "desconhecido"

        const val PARAM_NUMERO = "numero"
        const val PARAM_MOTIVO = "motivo"
    }
}
