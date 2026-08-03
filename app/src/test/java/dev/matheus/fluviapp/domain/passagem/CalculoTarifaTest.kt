package dev.matheus.fluviapp.domain.passagem

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

/**
 * Regra pura de desconto (ADR-0013 §5): desconto = resíduo abaixo da tarifa devida, piso em zero.
 * Substitui o acúmulo ANTAC do antigo `CalculoDesconto` e a conta circular de `getValorTotal`.
 */
class CalculoTarifaTest {

    @Test
    fun `desconto e o quanto se cobrou abaixo da devida`() {
        // devida 300, cobrado 250 -> desconto 50
        assertEquals(
            BigDecimal("50.00"),
            descontoDerivado(BigDecimal("300.00"), BigDecimal("250.00")),
        )
    }

    @Test
    fun `cobrar a tarifa cheia nao gera desconto`() {
        assertEquals(
            BigDecimal("0.00"),
            descontoDerivado(BigDecimal("300.00"), BigDecimal("300.00")),
        )
    }

    @Test
    fun `cobrar acima da devida nao vira desconto negativo (piso em zero)`() {
        assertEquals(
            BigDecimal("0.00"),
            descontoDerivado(BigDecimal("300.00"), BigDecimal("350.00")),
        )
    }

    @Test
    fun `meia ja embutida na devida - desconto so mede abaixo dela`() {
        // devida da meia = 150; cobrado 120 -> desconto 30 (a meia NAO conta como desconto)
        val devidaMeia = TipoPassagem.MEIA.tarifaDevida(BigDecimal("300"))
        assertEquals(BigDecimal("30.00"), descontoDerivado(devidaMeia, BigDecimal("120.00")))
    }

    @Test
    fun `gratuidade tem devida zero - nunca ha desconto`() {
        val devidaGratuidade = TipoPassagem.GRATUIDADE.tarifaDevida(BigDecimal("300"))
        assertEquals(BigDecimal("0.00"), descontoDerivado(devidaGratuidade, BigDecimal.ZERO))
    }

    // --- Moto: piso à centena da cilindrada (ADR-0013) ---

    @Test
    fun `tarifa da moto e o piso a centena da cilindrada em reais`() {
        assertEquals(BigDecimal("100.00"), tarifaMotoBase(125))
        assertEquals(BigDecimal("200.00"), tarifaMotoBase(250))
        assertEquals(BigDecimal("300.00"), tarifaMotoBase(300))
        assertEquals(BigDecimal("600.00"), tarifaMotoBase(600))
    }

    @Test
    fun `moto abaixo de 100cc cai no piso zero`() {
        assertEquals(BigDecimal("0.00"), tarifaMotoBase(50))
    }
}