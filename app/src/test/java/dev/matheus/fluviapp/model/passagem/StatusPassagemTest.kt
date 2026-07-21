package dev.matheus.fluviapp.model.passagem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ciclo de vida da passagem como tipo de domínio (ADR-0012). Máquina de estados pura, JVM-testável.
 *
 * Ciclo: A_EMITIR → EMITIDA → EMBARCADA (terminal, embarque irreversível).
 */
class StatusPassagemTest {

    // --- de(): fronteira String -> enum, tolerante à grafia legada ---

    @Test
    fun `de converte o name canonico dos tres estados`() {
        assertEquals(StatusPassagem.A_EMITIR, StatusPassagem.de("A_EMITIR"))
        assertEquals(StatusPassagem.EMITIDA, StatusPassagem.de("EMITIDA"))
        assertEquals(StatusPassagem.EMBARCADA, StatusPassagem.de("EMBARCADA"))
    }

    @Test
    fun `de tolera a grafia legada com espaco e caixa`() {
        // "A EMITIR" (com espaço) era o valor gravado na criação antes do ADR-0012
        assertEquals(StatusPassagem.A_EMITIR, StatusPassagem.de("A EMITIR"))
        assertEquals(StatusPassagem.A_EMITIR, StatusPassagem.de("a emitir"))
        assertEquals(StatusPassagem.EMITIDA, StatusPassagem.de(" emitida "))
    }

    @Test
    fun `de retorna null para desconhecido ou nulo (fail-closed)`() {
        assertNull(StatusPassagem.de(null))
        assertNull(StatusPassagem.de(""))
        assertNull(StatusPassagem.de("CANCELADA"))
    }

    // --- Máquina de estados ---

    @Test
    fun `arestas legais do ciclo`() {
        assertTrue(StatusPassagem.A_EMITIR.podeTransicionarPara(StatusPassagem.EMITIDA))
        assertTrue(StatusPassagem.EMITIDA.podeTransicionarPara(StatusPassagem.EMBARCADA))
    }

    @Test
    fun `nao pula etapa - a emitir nao vai direto para embarcada`() {
        assertFalse(StatusPassagem.A_EMITIR.podeTransicionarPara(StatusPassagem.EMBARCADA))
    }

    @Test
    fun `embarque e irreversivel - EMBARCADA nao transiciona para nada`() {
        assertTrue(StatusPassagem.EMBARCADA.ehTerminal())
        StatusPassagem.entries.forEach { destino ->
            assertFalse(
                "EMBARCADA não deveria transicionar para $destino",
                StatusPassagem.EMBARCADA.podeTransicionarPara(destino)
            )
        }
    }

    @Test
    fun `nao retrocede - EMITIDA nao volta para A_EMITIR`() {
        assertFalse(StatusPassagem.EMITIDA.podeTransicionarPara(StatusPassagem.A_EMITIR))
    }

    @Test
    fun `nenhum estado transiciona para si mesmo`() {
        StatusPassagem.entries.forEach {
            assertFalse("$it não deveria transicionar para si", it.podeTransicionarPara(it))
        }
    }

    // --- rotulo(): exibição ---

    @Test
    fun `rotulo troca underscore por espaco`() {
        assertEquals("A EMITIR", StatusPassagem.A_EMITIR.rotulo())
        assertEquals("EMITIDA", StatusPassagem.EMITIDA.rotulo())
        assertEquals("EMBARCADA", StatusPassagem.EMBARCADA.rotulo())
    }
}