package dev.matheus.fluviapp.domain.operacoes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Atuação como tipo de domínio (ADR-0020 D5). O valor é fechado; o **fato** de uma empresa exercer uma
 * atuação continua sendo cadastrado no painel.
 */
class AtuacaoTest {

    @Test
    fun `de converte o name canonico das quatro atuacoes`() {
        assertEquals(Atuacao.AGENCIAMENTO, Atuacao.de("AGENCIAMENTO"))
        assertEquals(Atuacao.TRANSPORTE, Atuacao.de("TRANSPORTE"))
        assertEquals(Atuacao.PORTUARIA_OPERACAO, Atuacao.de("PORTUARIA_OPERACAO"))
        assertEquals(Atuacao.PORTUARIA_ARRENDAMENTO, Atuacao.de("PORTUARIA_ARRENDAMENTO"))
    }

    @Test
    fun `de tolera espaco e caixa — o id do documento e o proprio name`() {
        assertEquals(Atuacao.AGENCIAMENTO, Atuacao.de(" agenciamento "))
        assertEquals(Atuacao.PORTUARIA_OPERACAO, Atuacao.de("portuaria operacao"))
    }

    @Test
    fun `de retorna null para desconhecida ou nula (fail-closed)`() {
        assertNull(Atuacao.de(null))
        assertNull(Atuacao.de(""))
        assertNull(Atuacao.de("ARMAZENAGEM"))
    }

    @Test
    fun `as portuarias nascem dormentes`() {
        assertTrue(Atuacao.AGENCIAMENTO.operante)
        assertTrue(Atuacao.TRANSPORTE.operante)
        assertFalse(Atuacao.PORTUARIA_OPERACAO.operante)
        assertFalse(Atuacao.PORTUARIA_ARRENDAMENTO.operante)
    }

    @Test
    fun `operantes devolve so as que produzem operacao hoje`() {
        assertEquals(listOf(Atuacao.AGENCIAMENTO, Atuacao.TRANSPORTE), Atuacao.operantes())
    }
}