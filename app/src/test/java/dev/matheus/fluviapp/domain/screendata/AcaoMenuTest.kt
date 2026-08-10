package dev.matheus.fluviapp.domain.screendata

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A estrutura do menu como domínio (ADR-0020 F3). Antes ela vivia dentro do `NavGraphBuilder`, no
 * `acoesDe(secao)` — e por isso não havia como testá-la sem levantar navegação junto.
 */
class AcaoMenuTest {

    /**
     * O invariante voltou a valer para **todas** as seções na F8.2: a exceção da Viagem, aberta na F8.0
     * enquanto ela estava em obras, fechou junto com o cadastro dela. Quem a fechou foi o teste-estopim
     * que ficava ao lado — ele apontava o buraco e sumiu quando o buraco sumiu.
     */
    @Test
    fun `toda secao oferece ao menos uma acao — nenhuma abre vazia`() {
        SecaoMenu.entries.forEach { secao ->
            assertTrue("$secao sem ações", AcaoMenu.de(secao).isNotEmpty())
        }
    }

    @Test
    fun `cada acao pertence a exatamente uma secao`() {
        AcaoMenu.entries.forEach { acao ->
            assertEquals(listOf(acao), AcaoMenu.de(acao.secao).filter { it == acao })
        }
    }

    @Test
    fun `a passagem oferece pesquisar e contagem`() {
        assertEquals(
            listOf(AcaoMenu.PASSAGEM_PESQUISAR, AcaoMenu.PASSAGEM_CONTAGEM),
            AcaoMenu.de(SecaoMenu.PASSAGEM),
        )
    }

    @Test
    fun `os cadastros oferecem novo e pesquisar, nessa ordem`() {
        listOf(
            SecaoMenu.VIAGEM,
            SecaoMenu.EQUIPE,
            SecaoMenu.EMPRESA,
            SecaoMenu.EMBARCACAO,
            SecaoMenu.LOCALIDADE,
            SecaoMenu.PORTO,
        ).forEach { secao ->
            assertEquals("$secao deveria ter duas ações", 2, AcaoMenu.de(secao).size)
        }
    }

    @Test
    fun `acoesPorSecao monta o menu so das secoes visiveis`() {
        val menu = acoesPorSecao(listOf(SecaoMenu.PASSAGEM, SecaoMenu.EQUIPE))

        assertEquals(setOf(SecaoMenu.PASSAGEM, SecaoMenu.EQUIPE), menu.keys)
        assertEquals(AcaoMenu.de(SecaoMenu.PASSAGEM), menu[SecaoMenu.PASSAGEM])
    }

    @Test
    fun `menu sem secao visivel e menu vazio, nao menu completo`() {
        assertTrue(acoesPorSecao(emptyList()).isEmpty())
    }

    @Test
    fun `titulo e icone estao preenchidos em todas as acoes`() {
        AcaoMenu.entries.forEach { acao ->
            assertTrue("$acao sem título", acao.titulo != 0)
            assertTrue("$acao sem ícone", acao.icone != 0)
        }
    }
}