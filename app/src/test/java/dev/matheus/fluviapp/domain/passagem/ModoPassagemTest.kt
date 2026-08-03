package dev.matheus.fluviapp.domain.passagem

import dev.matheus.fluviapp.revitalizacao.ForaDoEscopo
import org.junit.experimental.categories.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * O modo da passagem como tipo de domínio (ADR-0018 D6, entrando pelo ADR-0020). O grosso dos casos é a
 * tolerância de `de()`: é ela que faz o dado gravado pelo catálogo continuar legível depois que ele sai.
 */
@Category(ForaDoEscopo::class)
class ModoPassagemTest {

    @Test
    fun `de converte o name canonico dos quatro modos`() {
        assertEquals(ModoPassagem.REDE, ModoPassagem.de("REDE"))
        assertEquals(ModoPassagem.SUITE, ModoPassagem.de("SUITE"))
        assertEquals(ModoPassagem.CAMAROTE, ModoPassagem.de("CAMAROTE"))
        assertEquals(ModoPassagem.VEICULO, ModoPassagem.de("VEICULO"))
    }

    @Test
    fun `de tolera caixa e espaco`() {
        assertEquals(ModoPassagem.REDE, ModoPassagem.de(" rede "))
        assertEquals(ModoPassagem.CAMAROTE, ModoPassagem.de("Camarote"))
    }

    @Test
    fun `de tolera acento — o catalogo gravava Suite com i acentuado`() {
        assertEquals(ModoPassagem.SUITE, ModoPassagem.de("Suíte"))
        assertEquals(ModoPassagem.VEICULO, ModoPassagem.de("Veículo"))
    }

    @Test
    fun `de le os rotulos inteiros que o catalogo semeava`() {
        // Estes são os valores REAIS que o SampleData gravava — e que o código nunca conseguiu casar.
        assertEquals(ModoPassagem.REDE, ModoPassagem.de("Rede"))
        assertEquals(ModoPassagem.SUITE, ModoPassagem.de("Suíte p/ 2 Pessoas"))
        assertEquals(ModoPassagem.SUITE, ModoPassagem.de("Suíte p/ 3 Pessoas"))
        assertEquals(ModoPassagem.CAMAROTE, ModoPassagem.de("Camarote"))
    }

    @Test
    fun `de retorna null para desconhecido ou nulo (fail-closed)`() {
        assertNull(ModoPassagem.de(null))
        assertNull(ModoPassagem.de(""))
        assertNull(ModoPassagem.de("   "))
        assertNull(ModoPassagem.de("PRIMEIRA CLASSE"))
    }

    @Test
    fun `acomodacoes traz os tres modos de passageiro, sem o veiculo`() {
        assertEquals(
            listOf(ModoPassagem.REDE, ModoPassagem.SUITE, ModoPassagem.CAMAROTE),
            ModoPassagem.acomodacoes(),
        )
    }

    @Test
    fun `so o veiculo nao e acomodacao`() {
        assertFalse(ModoPassagem.VEICULO.ehAcomodacao)
        ModoPassagem.acomodacoes().forEach { assertTrue(it.ehAcomodacao) }
    }

    @Test
    fun `a distincao suite de 2 e de 3 nao e modo — as duas colapsam em SUITE`() {
        assertEquals(ModoPassagem.de("Suíte p/ 2 Pessoas"), ModoPassagem.de("Suíte p/ 3 Pessoas"))
    }
}
