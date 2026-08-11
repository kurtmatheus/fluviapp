package dev.matheus.fluviapp.domain.passagem

import dev.matheus.fluviapp.revitalizacao.ForaDoEscopo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.categories.Category

/**
 * A categoria como eixo raiz do agregado (ADR-0023 D1) e discriminador do documento (ADR-0024 D1).
 *
 * Dois casos aqui defendem decisões, e não comportamento: que **`CARGA` não existe ainda** — porque valor de enum
 * sem portador é o que o ADR-0020 D4 corrigiu no tipo de embarcação — e que a fronteira é **fail-closed**, para
 * que documento com categoria ilegível seja recusado em vez de virar passageiro por padrão.
 *
 * `@Category(ForaDoEscopo)` — tipo sem portador vivo (ver `AcomodacaoTest`).
 */
@Category(ForaDoEscopo::class)
class CategoriaPassagemTest {

    @Test
    fun `de converte o name canonico das duas categorias`() {
        assertEquals(CategoriaPassagem.PASSAGEIRO, CategoriaPassagem.de("PASSAGEIRO"))
        assertEquals(CategoriaPassagem.VEICULO, CategoriaPassagem.de("VEICULO"))
    }

    @Test
    fun `de tolera espaco e caixa`() {
        assertEquals(CategoriaPassagem.PASSAGEIRO, CategoriaPassagem.de(" passageiro "))
        assertEquals(CategoriaPassagem.VEICULO, CategoriaPassagem.de("Veiculo"))
    }

    /** Fail-closed: sem categoria não há passagem. Não se escolhe um padrão. */
    @Test
    fun `de recusa desconhecido, vazio e nulo`() {
        assertNull(CategoriaPassagem.de(null))
        assertNull(CategoriaPassagem.de(""))
        assertNull(CategoriaPassagem.de("   "))
        assertNull(CategoriaPassagem.de("OUTRA"))
    }

    /**
     * **A carga é prevista, não declarada.** Enquanto ela não tiver portador, um valor aqui seria tipo sem
     * ninguém que o carregue. A prontidão para ela é o formato — o `when` exaustivo que passa a acusar cada
     * lugar a decidir —, não uma linha reservada.
     */
    @Test
    fun `carga ainda nao existe como categoria`() {
        assertNull(CategoriaPassagem.de("CARGA"))
        assertEquals(2, CategoriaPassagem.entries.size)
    }

    @Test
    fun `toda categoria tem rotulo`() {
        CategoriaPassagem.entries.forEach { assertTrue(it.rotulo.isNotBlank()) }
    }

    /** O `name` é o que a fronteira grava; o rótulo é o que a tela mostra — e eles não se confundem. */
    @Test
    fun `o rotulo do veiculo tem acento e o name nao`() {
        assertEquals("Veículo", CategoriaPassagem.VEICULO.rotulo)
        assertFalse(CategoriaPassagem.VEICULO.name.contains("í"))
    }
}