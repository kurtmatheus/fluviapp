package dev.matheus.fluviapp.domain.passagem

import dev.matheus.fluviapp.revitalizacao.ForaDoEscopo
import org.junit.experimental.categories.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ciclo de vida da passagem como tipo de domínio (ADR-0012). Máquina de estados pura, JVM-testável.
 *
 * Ciclo: A_EMITIR → EMITIDA → EMBARCADA (terminal, embarque irreversível), com **CANCELADA** como o **segundo
 * terminal** (ADR-0018 D17, ADR-0024 D11) — alcançável de A_EMITIR e de EMITIDA, e **nunca de EMBARCADA**.
 *
 * **Um caso deste arquivo inverteu de sinal na F9.1**: ele afirmava que `de("CANCELADA")` devolvia `null`, o que
 * era verdade quando cancelar era *delete* físico. Está anotado abaixo, porque teste que defende o comportamento
 * antigo é pior do que teste nenhum.
 */
@Category(ForaDoEscopo::class)
class StatusPassagemTest {

    // --- de(): fronteira String -> enum, tolerante à grafia legada ---

    @Test
    fun `de converte o name canonico dos quatro estados`() {
        assertEquals(StatusPassagem.A_EMITIR, StatusPassagem.de("A_EMITIR"))
        assertEquals(StatusPassagem.EMITIDA, StatusPassagem.de("EMITIDA"))
        assertEquals(StatusPassagem.EMBARCADA, StatusPassagem.de("EMBARCADA"))
        assertEquals(StatusPassagem.CANCELADA, StatusPassagem.de("CANCELADA"))
    }

    @Test
    fun `de tolera a grafia legada com espaco e caixa`() {
        // "A EMITIR" (com espaço) era o valor gravado na criação antes do ADR-0012
        assertEquals(StatusPassagem.A_EMITIR, StatusPassagem.de("A EMITIR"))
        assertEquals(StatusPassagem.A_EMITIR, StatusPassagem.de("a emitir"))
        assertEquals(StatusPassagem.EMITIDA, StatusPassagem.de(" emitida "))
    }

    /**
     * **Caso invertido na F9.1.** Ele listava `"CANCELADA"` como desconhecido, e estava certo enquanto cancelar
     * era remoção física (ADR-0012: *"cancelar não é estado"*). Desde que o histórico virou prioridade
     * (ADR-0018 D17), quem não existe é `EXPIRADA` — o estado que segue no futuro, porque expirar é **regra
     * temporal**, não ação de operador.
     */
    @Test
    fun `de retorna null para desconhecido ou nulo (fail-closed)`() {
        assertNull(StatusPassagem.de(null))
        assertNull(StatusPassagem.de(""))
        assertNull(StatusPassagem.de("EXPIRADA"))
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
        assertEquals("CANCELADA", StatusPassagem.CANCELADA.rotulo())
    }

    // --- CANCELADA: o segundo terminal (ADR-0018 D17, ADR-0024 D11) ---

    @Test
    fun `cancela de A_EMITIR e de EMITIDA`() {
        assertTrue(StatusPassagem.A_EMITIR.podeTransicionarPara(StatusPassagem.CANCELADA))
        assertTrue(StatusPassagem.EMITIDA.podeTransicionarPara(StatusPassagem.CANCELADA))
    }

    /**
     * **Quem já embarcou não cancela a travessia.** O que existe depois do embarque é acerto financeiro, e acerto
     * é do módulo de faturamento — não um retrocesso de estado.
     */
    @Test
    fun `EMBARCADA nao cancela`() {
        assertFalse(StatusPassagem.EMBARCADA.podeTransicionarPara(StatusPassagem.CANCELADA))
    }

    @Test
    fun `CANCELADA e terminal - nao volta nem avanca`() {
        assertTrue(StatusPassagem.CANCELADA.ehTerminal())
        StatusPassagem.entries.forEach { destino ->
            assertFalse(
                "CANCELADA não deveria transicionar para $destino",
                StatusPassagem.CANCELADA.podeTransicionarPara(destino),
            )
        }
    }

    /** Cancelar não é desembarcar nem desemitir: os dois terminais são becos, e são becos diferentes. */
    @Test
    fun `os dois terminais nao se alcancam`() {
        assertFalse(StatusPassagem.CANCELADA.podeTransicionarPara(StatusPassagem.EMBARCADA))
        assertFalse(StatusPassagem.EMBARCADA.podeTransicionarPara(StatusPassagem.CANCELADA))
    }
}
