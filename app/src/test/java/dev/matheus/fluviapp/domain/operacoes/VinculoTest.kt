package dev.matheus.fluviapp.domain.operacoes

import dev.matheus.fluviapp.domain.operacoes.Funcionario.Cargo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * O vínculo — a ligação entre a pessoa e a empresa em que ela atua (ADR-0016 §6, ADR-0022 D4).
 *
 * O que estes casos fixam não é o `data class`: é a **derivação da atuação** e o fail-closed da fronteira.
 * São as duas coisas que impedem um vínculo de conceder permissão que ninguém sabe qual é.
 */
class VinculoTest {

    @Test
    fun `a atuacao vem do cargo, e nao de um campo ao lado`() {
        assertEquals(Atuacao.AGENCIAMENTO, Vinculo("empresa-1", Cargo.SUPERVISOR).atuacao)
        assertEquals(Atuacao.AGENCIAMENTO, Vinculo("empresa-1", Cargo.AGENTE).atuacao)
    }

    /** É a garantia por construção: não existe vínculo cuja atuação contradiga o cargo. */
    @Test
    fun `todo cargo produz a atuacao que ele declara`() {
        Cargo.entries.forEach { cargo ->
            assertEquals(cargo.atuacao, Vinculo("empresa-1", cargo).atuacao)
        }
    }

    // --- Fronteira ---

    @Test
    fun `de monta o vinculo a partir das strings persistidas`() {
        val vinculo = Vinculo.de("empresa-1", "SUPERVISOR")

        assertEquals("empresa-1", vinculo?.empresaId)
        assertEquals(Cargo.SUPERVISOR, vinculo?.cargo)
    }

    /** Vínculo é o que concede permissão: cargo ilegível concederia permissão sem nome. */
    @Test
    fun `cargo desconhecido nao vira vinculo`() {
        assertNull(Vinculo.de("empresa-1", "CHEFAO"))
        assertNull(Vinculo.de("empresa-1", null))
        assertNull(Vinculo.de("empresa-1", ""))
    }

    @Test
    fun `sem empresa nao ha vinculo`() {
        assertNull(Vinculo.de("", "AGENTE"))
        assertNull(Vinculo.de(null, "AGENTE"))
        assertNull(Vinculo.de("   ", "AGENTE"))
    }

    // --- A lista ---

    private val naAgencia = Vinculo("empresa-1", Cargo.AGENTE)
    private val naOutra = Vinculo("empresa-2", Cargo.SUPERVISOR)

    @Test
    fun `empresaIds deriva a lista chata, sem repetir`() {
        val vinculos = listOf(naAgencia, naOutra, Vinculo("empresa-1", Cargo.SUPERVISOR))

        assertEquals(listOf("empresa-1", "empresa-2"), vinculos.empresaIds)
    }

    @Test
    fun `naEmpresa encontra o vinculo daquela empresa`() {
        val vinculos = listOf(naAgencia, naOutra)

        assertEquals(Cargo.SUPERVISOR, vinculos.naEmpresa("empresa-2")?.cargo)
        assertNull(vinculos.naEmpresa("empresa-9"))
    }

    /**
     * **Quem tem um vínculo só não escolhe nada; quem tem dois escolhe.** O `null` do meio não é falta de
     * informação — é a pergunta que a seleção de contexto vai fazer, e adivinhar por ela seria decidir em
     * nome de quem opera.
     */
    @Test
    fun `unicoOuNenhum resolve so o caso sem ambiguidade`() {
        assertEquals(naAgencia, listOf(naAgencia).unicoOuNenhum())
        assertNull(listOf(naAgencia, naOutra).unicoOuNenhum())
        assertNull(emptyList<Vinculo>().unicoOuNenhum())
    }
}