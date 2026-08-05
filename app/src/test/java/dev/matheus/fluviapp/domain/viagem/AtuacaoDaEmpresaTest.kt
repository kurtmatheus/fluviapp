package dev.matheus.fluviapp.domain.viagem

import dev.matheus.fluviapp.domain.operacoes.Atuacao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * O par `(parte, atuação)` e a **concessão** (ADR-0016 §4/§7). O que estes casos travam é o fail-closed:
 * o que não foi concedido não se vende, e frota nova nasce não-concedida.
 */
class AtuacaoDaEmpresaTest {

    private val agenciamento = AtuacaoDaEmpresa(
        atuacao = Atuacao.AGENCIAMENTO,
        embarcacaoIds = setOf("embarcacao-1", "embarcacao-2"),
    )
    private val transporte = AtuacaoDaEmpresa(atuacao = Atuacao.TRANSPORTE)

    // --- concessão ---

    @Test
    fun `embarcacao concedido pode ser vendido`() {
        assertTrue(agenciamento.concedeu("embarcacao-1"))
        assertTrue(agenciamento.concedeu("embarcacao-2"))
    }

    @Test
    fun `embarcacao nao concedido nao pode — frota nova nasce fora`() {
        assertFalse(agenciamento.concedeu("embarcacao-3"))
    }

    @Test
    fun `id ausente e negado, e nao tratado como sem filtro`() {
        assertFalse(agenciamento.concedeu(null))
        assertFalse(agenciamento.concedeu(""))
        assertFalse(agenciamento.concedeu("   "))
    }

    @Test
    fun `atuacao sem concessao nao concede nada`() {
        assertFalse(transporte.concedeu("embarcacao-1"))
    }

    // --- o conjunto de atuações da parte ---

    @Test
    fun `uma parte exerce varias atuacoes ao mesmo tempo`() {
        val atuacoes = listOf(agenciamento, transporte)

        assertTrue(atuacoes.exerce(Atuacao.AGENCIAMENTO))
        assertTrue(atuacoes.exerce(Atuacao.TRANSPORTE))
        assertFalse(atuacoes.exerce(Atuacao.PORTUARIA_OPERACAO))
    }

    @Test
    fun `de devolve a atuacao pedida, ou null quando a parte nao a exerce`() {
        val atuacoes = listOf(agenciamento, transporte)

        assertEquals(agenciamento, atuacoes.de(Atuacao.AGENCIAMENTO))
        assertNull(atuacoes.de(Atuacao.PORTUARIA_ARRENDAMENTO))
    }

    @Test
    fun `parte sem atuacao nenhuma nao exerce nada`() {
        val nenhuma = emptyList<AtuacaoDaEmpresa>()

        assertFalse(nenhuma.exerce(Atuacao.AGENCIAMENTO))
        assertNull(nenhuma.de(Atuacao.AGENCIAMENTO))
    }

    @Test
    fun `as dormentes ficam fora das operantes`() {
        val atuacoes = listOf(
            agenciamento,
            transporte,
            AtuacaoDaEmpresa(Atuacao.PORTUARIA_OPERACAO),
        )

        assertEquals(listOf(agenciamento, transporte), atuacoes.operantes())
    }
}