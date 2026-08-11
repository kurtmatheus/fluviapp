package dev.matheus.fluviapp.domain.passagem

import dev.matheus.fluviapp.revitalizacao.ForaDoEscopo
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.experimental.categories.Category
import java.math.BigDecimal

/**
 * Lançamento de pagamento (ADR-0018 D11): o **fato** de que um valor entrou por uma forma, no lugar das quatro
 * colunas fixas cuja única leitura era a soma.
 *
 * O que se cobra aqui é o que a lista consegue e as colunas não conseguiam: **dois lançamentos da mesma forma**
 * (dois cartões, dois pagadores no PIX) sem colapsar num número só.
 *
 * `@Category(ForaDoEscopo)` — tipo sem portador vivo (ver `AcomodacaoTest`).
 */
@Category(ForaDoEscopo::class)
class LancamentoTest {

    private fun lancamento(id: String, forma: FormaPagamento, valor: String) =
        Lancamento(id = id, forma = forma, valor = BigDecimal(valor))

    @Test
    fun `total soma os lancamentos`() {
        val lancamentos = listOf(
            lancamento("l1", FormaPagamento.PIX, "50.00"),
            lancamento("l2", FormaPagamento.DINHEIRO, "10.50"),
        )

        assertEquals(BigDecimal("60.50"), lancamentos.total)
    }

    @Test
    fun `total de lista vazia e zero`() {
        assertEquals(BigDecimal.ZERO, emptyList<Lancamento>().total)
    }

    /** O caso que as quatro colunas não sabiam registrar: a mesma forma, duas vezes. */
    @Test
    fun `dois lancamentos da mesma forma nao colapsam`() {
        val lancamentos = listOf(
            lancamento("l1", FormaPagamento.CREDITO, "30.00"),
            lancamento("l2", FormaPagamento.CREDITO, "20.00"),
        )

        assertEquals(2, lancamentos.size)
        assertEquals(BigDecimal("50.00"), lancamentos.total)
        assertEquals(BigDecimal("50.00"), lancamentos.totalPorForma()[FormaPagamento.CREDITO])
    }

    @Test
    fun `totalPorForma agrupa por forma`() {
        val lancamentos = listOf(
            lancamento("l1", FormaPagamento.PIX, "40.00"),
            lancamento("l2", FormaPagamento.DINHEIRO, "5.00"),
            lancamento("l3", FormaPagamento.PIX, "10.00"),
        )

        val porForma = lancamentos.totalPorForma()

        assertEquals(BigDecimal("50.00"), porForma[FormaPagamento.PIX])
        assertEquals(BigDecimal("5.00"), porForma[FormaPagamento.DINHEIRO])
        assertEquals(2, porForma.size)
    }

    /**
     * `BigDecimal` no domínio (ADR-0013 §6): centavos somam exato. Com `Double`, `0.1 + 0.2` não é `0.3`, e é
     * por isso que o dinheiro só vira número de ponto flutuante **na fronteira**.
     */
    @Test
    fun `centavos somam exato`() {
        val lancamentos = listOf(
            lancamento("l1", FormaPagamento.PIX, "0.10"),
            lancamento("l2", FormaPagamento.DINHEIRO, "0.20"),
        )

        assertEquals(BigDecimal("0.30"), lancamentos.total)
    }
}