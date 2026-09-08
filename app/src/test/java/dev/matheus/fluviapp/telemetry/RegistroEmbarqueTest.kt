package dev.matheus.fluviapp.telemetry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * O registrador do **embarque** ([ADR-0032] D4), que até 2026-09-07 não existia: confirmar embarque não
 * emitia evento nenhum, e o único rastro era o carimbo dentro do documento.
 *
 * A diferença prática entre os dois: o carimbo responde *quem validou aquele bilhete*; o evento responde
 * *quantos embarques houve hoje* — pergunta que, sem ele, só se respondia varrendo as passagens.
 */
class RegistroEmbarqueTest {

    private val telemetry = FakeTelemetry()
    private val registro = RegistroEmbarque(telemetry)

    @Test
    fun `confirmar registra evento e deixa rastro`() {
        registro.confirmado(numero = "12")

        assertEquals(listOf(RegistroEmbarque.EVENTO_CONFIRMADO), telemetry.nomesDeEventos())
        assertEquals("12", telemetry.eventos.single().params[RegistroEmbarque.PARAM_NUMERO])
        assertTrue(telemetry.rastros.single().contains("#12"))
    }

    /**
     * **Recusa não é erro**, e por isso não vira `naoFatal`: bilhete já usado é o antifraude funcionando,
     * e bilhete não emitido é gente chegando à doca com o que ainda não pagou. As duas são operação
     * normal, e o que interessa é a contagem por motivo.
     */
    @Test
    fun `recusar registra o motivo, e nao um nao-fatal`() {
        registro.recusado("ja_embarcada")

        assertEquals(listOf(RegistroEmbarque.EVENTO_RECUSADO), telemetry.nomesDeEventos())
        assertEquals("ja_embarcada", telemetry.eventos.single().params[RegistroEmbarque.PARAM_MOTIVO])
        assertTrue(telemetry.naoFatais.isEmpty())
    }

    /**
     * **O evento sai com as coordenadas sem que o registrador saiba delas.** É o ponto do desenho: quem
     * emite não carrega o contexto — ele é ambiente, empurrado uma vez pela sessão.
     */
    @Test
    fun `o evento carrega as coordenadas de quem opera`() {
        telemetry.definirCoordenadas(mapOf(PARAM_PAPEL to "OPERADOR", PARAM_AGENCIA to "empresa-1"))

        registro.confirmado(numero = "12")

        val params = telemetry.eventos.single().params
        assertEquals("OPERADOR", params[PARAM_PAPEL])
        assertEquals("empresa-1", params[PARAM_AGENCIA])
        assertEquals("12", params[RegistroEmbarque.PARAM_NUMERO])
    }
}
