package dev.matheus.fluviapp.domain.passagem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

/**
 * Tipo tarifário como tipo de domínio (ADR-0013). Fronteira String→enum + a regra pura de tarifa devida
 * (inteira = base, meia = metade round-up scale 2, gratuidade = zero).
 */
class TipoPassagemTest {

    // --- de(): fronteira String -> enum ---

    @Test
    fun `de converte o name canonico dos tres tipos`() {
        assertEquals(TipoPassagem.INTEIRA, TipoPassagem.de("INTEIRA"))
        assertEquals(TipoPassagem.MEIA, TipoPassagem.de("MEIA"))
        assertEquals(TipoPassagem.GRATUIDADE, TipoPassagem.de("GRATUIDADE"))
    }

    @Test
    fun `de tolera espaco e caixa`() {
        assertEquals(TipoPassagem.INTEIRA, TipoPassagem.de("inteira"))
        assertEquals(TipoPassagem.GRATUIDADE, TipoPassagem.de(" Gratuidade "))
    }

    @Test
    fun `de retorna null para desconhecido ou nulo (fail-closed)`() {
        assertNull(TipoPassagem.de(null))
        assertNull(TipoPassagem.de(""))
        assertNull(TipoPassagem.de("CORTESIA"))
    }

    // --- tarifaDevida(): regra pura por categoria ---

    @Test
    fun `inteira deve a tarifa base cheia, em scale 2`() {
        assertEquals(BigDecimal("300.00"), TipoPassagem.INTEIRA.tarifaDevida(BigDecimal("300")))
    }

    @Test
    fun `meia e metade da base, scale 2`() {
        assertEquals(BigDecimal("150.00"), TipoPassagem.MEIA.tarifaDevida(BigDecimal("300")))
    }

    @Test
    fun `meia arredonda o centavo para cima (RoundingMode UP)`() {
        // 300.01 / 2 = 150.005 -> UP scale 2 -> 150.01
        assertEquals(BigDecimal("150.01"), TipoPassagem.MEIA.tarifaDevida(BigDecimal("300.01")))
        // 0.01 / 2 = 0.005 -> UP -> 0.01
        assertEquals(BigDecimal("0.01"), TipoPassagem.MEIA.tarifaDevida(BigDecimal("0.01")))
    }

    @Test
    fun `gratuidade zera a tarifa, scale 2`() {
        assertEquals(BigDecimal("0.00"), TipoPassagem.GRATUIDADE.tarifaDevida(BigDecimal("300")))
    }

    // --- rotulo(): exibição ---

    @Test
    fun `rotulo formata para exibicao`() {
        assertEquals("Inteira", TipoPassagem.INTEIRA.rotulo())
        assertEquals("Meia", TipoPassagem.MEIA.rotulo())
        assertEquals("Gratuidade", TipoPassagem.GRATUIDADE.rotulo())
    }
}