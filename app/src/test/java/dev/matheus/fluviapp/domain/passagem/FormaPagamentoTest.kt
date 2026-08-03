package dev.matheus.fluviapp.domain.passagem

import dev.matheus.fluviapp.revitalizacao.ForaDoEscopo
import org.junit.experimental.categories.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Forma de pagamento como tipo de domínio (ADR-0020 D3). O que a linha de catálogo não conseguia carregar:
 * quando o valor entra no caixa e o que o fechamento conta na gaveta.
 */
@Category(ForaDoEscopo::class)
class FormaPagamentoTest {

    @Test
    fun `de converte o name canonico das quatro formas`() {
        assertEquals(FormaPagamento.DINHEIRO, FormaPagamento.de("DINHEIRO"))
        assertEquals(FormaPagamento.PIX, FormaPagamento.de("PIX"))
        assertEquals(FormaPagamento.DEBITO, FormaPagamento.de("DEBITO"))
        assertEquals(FormaPagamento.CREDITO, FormaPagamento.de("CREDITO"))
    }

    @Test
    fun `de tolera espaco e caixa`() {
        assertEquals(FormaPagamento.PIX, FormaPagamento.de(" pix "))
        assertEquals(FormaPagamento.DEBITO, FormaPagamento.de("Debito"))
    }

    @Test
    fun `de retorna null para desconhecido ou nulo (fail-closed)`() {
        assertNull(FormaPagamento.de(null))
        assertNull(FormaPagamento.de(""))
        // O item que um administrador cadastraria — e que não significaria nada sem código.
        assertNull(FormaPagamento.de("VOUCHER"))
    }

    @Test
    fun `so o credito nao liquida no ato`() {
        assertTrue(FormaPagamento.DINHEIRO.liquidacaoImediata)
        assertTrue(FormaPagamento.PIX.liquidacaoImediata)
        assertTrue(FormaPagamento.DEBITO.liquidacaoImediata)
        assertFalse(FormaPagamento.CREDITO.liquidacaoImediata)
    }

    @Test
    fun `so o dinheiro e especie`() {
        assertTrue(FormaPagamento.DINHEIRO.ehEspecie)
        listOf(FormaPagamento.PIX, FormaPagamento.DEBITO, FormaPagamento.CREDITO)
            .forEach { assertFalse(it.ehEspecie) }
    }
}
