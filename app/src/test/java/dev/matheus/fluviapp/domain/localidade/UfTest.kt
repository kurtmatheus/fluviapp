package dev.matheus.fluviapp.domain.localidade

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** UF como tipo de domínio (ADR-0020 D6): 27 unidades, fechadas por constituição. */
class UfTest {

    @Test
    fun `sao exatamente 27 unidades federativas`() {
        assertEquals(27, Uf.entries.size)
    }

    @Test
    fun `de aceita a sigla, em qualquer caixa`() {
        assertEquals(Uf.PA, Uf.de("PA"))
        assertEquals(Uf.PA, Uf.de("pa"))
        assertEquals(Uf.SP, Uf.de(" sp "))
    }

    @Test
    fun `de aceita o nome por extenso — e assim que o dado nasceu no catalogo`() {
        assertEquals(Uf.PA, Uf.de("Pará"))
        assertEquals(Uf.PA, Uf.de("pará"))
        assertEquals(Uf.RJ, Uf.de("Rio de Janeiro"))
    }

    @Test
    fun `de retorna null para desconhecida ou nula (fail-closed)`() {
        assertNull(Uf.de(null))
        assertNull(Uf.de(""))
        assertNull(Uf.de("XX"))
        // Sem tabela genérica, não há como cadastrar "Pará" duas vezes — a unicidade que o
        // ADR-0016 §5 precisou inventar deixa de ser necessária.
        assertNull(Uf.de("Para do Norte"))
    }

    @Test
    fun `a sigla e o valor canonico e o rotulo traz o nome junto`() {
        assertEquals("PA", Uf.PA.sigla)
        assertEquals("Pará", Uf.PA.nome)
        assertEquals("Pará (PA)", Uf.PA.rotulo())
    }

    @Test
    fun `nenhuma sigla se repete`() {
        assertEquals(Uf.entries.size, Uf.entries.map { it.sigla }.toSet().size)
    }
}