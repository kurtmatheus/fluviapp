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

    // A tarifa da moto por faixa de cilindrada saiu em 2026-09-03 (ADR-0031), e com ela estes dois casos.
    //
    // Ela era a regra provisória do ADR-0013 e já não tinha chamador em produção desde que *preço é I/O*: a
    // emissão não calcula valor, o operador informa o praticado. O que restava era o vínculo conceitual
    // entre classe de veículo e dinheiro, e ele caiu quando a cilindrada passou a derivar da natureza.
}
