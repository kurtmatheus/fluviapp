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

    // --- O que saiu, e por quê ---
    //
    // Nove casos moravam aqui e cobriam `empresaIds`, `naEmpresa`, `unicoOuNenhum`,
    // `resolverVinculoAtivo` e `precisaEscolherVinculo` — as extensões sobre `List<Vinculo>`. Todas
    // respondiam à mesma pergunta, *qual dos vínculos vale agora*, e a [ADR-0032] D5 tirou a pergunta do
    // domínio: com uma empresa no máximo, o vínculo em vigor é o vínculo.
    //
    // Um deles vale registro: `escolha de empresa em que a pessoa nao atua mais nao vale` travava o pior
    // defeito possível daquele desenho — operar em nome de uma empresa cujo vínculo já se perdeu, porque
    // o id ficara gravado no aparelho. **Ele não deixou de ser verdade; deixou de ser possível**, e é
    // isso que a estrutura nova garante sem precisar de teste: não há id guardado a revalidar.
}