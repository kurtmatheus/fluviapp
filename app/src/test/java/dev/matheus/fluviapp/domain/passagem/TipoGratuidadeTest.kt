package dev.matheus.fluviapp.domain.passagem

import dev.matheus.fluviapp.revitalizacao.ForaDoEscopo
import org.junit.experimental.categories.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Subtipo de gratuidade como tipo de domínio (ADR-0013). As quatro gratuidades legais; `CORTESIA`
 * aposentada (não é subtipo). Fronteira String→enum tolerante à grafia legada.
 */
@Category(ForaDoEscopo::class)
class TipoGratuidadeTest {

    @Test
    fun `de converte o name canonico dos quatro subtipos`() {
        assertEquals(TipoGratuidade.IDOSO, TipoGratuidade.de("IDOSO"))
        assertEquals(TipoGratuidade.PCD, TipoGratuidade.de("PCD"))
        assertEquals(TipoGratuidade.CRIANCA_ATE_5, TipoGratuidade.de("CRIANCA_ATE_5"))
        assertEquals(TipoGratuidade.PASSE_FEDERAL, TipoGratuidade.de("PASSE_FEDERAL"))
    }

    @Test
    fun `de tolera espaco e caixa`() {
        assertEquals(TipoGratuidade.PASSE_FEDERAL, TipoGratuidade.de("passe federal"))
        assertEquals(TipoGratuidade.CRIANCA_ATE_5, TipoGratuidade.de(" Crianca Ate 5 "))
    }

    @Test
    fun `de retorna null para desconhecido, nulo ou a CORTESIA aposentada (fail-closed)`() {
        assertNull(TipoGratuidade.de(null))
        assertNull(TipoGratuidade.de(""))
        assertNull(TipoGratuidade.de("CORTESIA"))
    }

    @Test
    fun `rotulo formata para exibicao`() {
        assertEquals("Idoso", TipoGratuidade.IDOSO.rotulo())
        assertEquals("PcD", TipoGratuidade.PCD.rotulo())
        assertEquals("Criança até 5 anos", TipoGratuidade.CRIANCA_ATE_5.rotulo())
        assertEquals("Passe Federal", TipoGratuidade.PASSE_FEDERAL.rotulo())
    }
}
