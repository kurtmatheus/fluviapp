package dev.matheus.fluviapp.domain.passagem

import dev.matheus.fluviapp.revitalizacao.ForaDoEscopo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.categories.Category

/**
 * A acomodação como tipo de domínio (ADR-0023 D3), no lugar do eixo único `ModoPassagem` — que tinha o veículo
 * dentro porque a categoria ainda não era raiz.
 *
 * O que se cobra aqui é a **regra que subiu para o tipo**: quais tipos tarifários cada acomodação admite, e
 * quantos clientes cabem num bilhete. Fora da rede, *meia* e *gratuidade* não existem.
 *
 * `@Category(ForaDoEscopo)` pela régua que o `TipoEmbarcacaoTest` escreveu: **tipo sem portador vivo fica fora**.
 * O portador é a `Passagem` selada, e a seção só é alcançável na F9.6 — é lá que estes casos voltam ao escopo.
 */
@Category(ForaDoEscopo::class)
class AcomodacaoTest {

    @Test
    fun `rede admite os tres tipos tarifarios`() {
        assertTrue(Acomodacao.REDE.admite(TipoPassagem.INTEIRA))
        assertTrue(Acomodacao.REDE.admite(TipoPassagem.MEIA))
        assertTrue(Acomodacao.REDE.admite(TipoPassagem.GRATUIDADE))
    }

    @Test
    fun `suite e camarote sao sempre inteira`() {
        listOf(Acomodacao.SUITE, Acomodacao.CAMAROTE).forEach { acomodacao ->
            assertTrue(acomodacao.admite(TipoPassagem.INTEIRA))
            assertFalse(acomodacao.admite(TipoPassagem.MEIA))
            assertFalse(acomodacao.admite(TipoPassagem.GRATUIDADE))
        }
    }

    /** Fail-closed: sem tipo escolhido não se admite nada — a ausência não vira inteira por conveniência. */
    @Test
    fun `tipo nulo nao e admitido por nenhuma acomodacao`() {
        Acomodacao.entries.forEach { assertFalse(it.admite(null)) }
    }

    /** É o que decide se a tela mostra o seletor: onde só cabe inteira, não há escolha a oferecer. */
    @Test
    fun `so a rede tem escolha de tipo`() {
        assertTrue(Acomodacao.REDE.temEscolhaDeTipo)
        assertFalse(Acomodacao.SUITE.temEscolhaDeTipo)
        assertFalse(Acomodacao.CAMAROTE.temEscolhaDeTipo)
    }

    @Test
    fun `rede e um cliente por bilhete - suite e camarote ate tres`() {
        assertEquals(1, Acomodacao.REDE.ocupacaoMaxima)
        assertEquals(3, Acomodacao.SUITE.ocupacaoMaxima)
        assertEquals(3, Acomodacao.CAMAROTE.ocupacaoMaxima)
    }

    @Test
    fun `de le o name canonico e tolera grafia legada`() {
        assertEquals(Acomodacao.REDE, Acomodacao.de("REDE"))
        assertEquals(Acomodacao.SUITE, Acomodacao.de("  suite "))
        assertEquals(Acomodacao.CAMAROTE, Acomodacao.de("Camarote"))
    }

    @Test
    fun `de recusa desconhecido e nulo`() {
        assertNull(Acomodacao.de("POLTRONA"))
        assertNull(Acomodacao.de(""))
        assertNull(Acomodacao.de(null))
    }

    /**
     * `porRotulo` lê o que a **pessoa** escolheu; `de` lê o que o **Firestore** gravou. A suíte tem acento no
     * rótulo e não no `name` — é o caso que confunde as duas fronteiras se elas forem a mesma função.
     */
    @Test
    fun `porRotulo le o texto da tela, com acento`() {
        assertEquals(Acomodacao.SUITE, Acomodacao.porRotulo("Suíte"))
        assertEquals(Acomodacao.SUITE, Acomodacao.porRotulo("suíte"))
        assertNull(Acomodacao.porRotulo("SUITE_"))
    }
}