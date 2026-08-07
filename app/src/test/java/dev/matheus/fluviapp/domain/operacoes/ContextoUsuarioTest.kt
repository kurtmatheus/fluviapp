package dev.matheus.fluviapp.domain.operacoes

import dev.matheus.fluviapp.fakes.FakeSessaoUsuario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Os dois contextos do logado (ADR-0015 §8.1) e — desde o ADR-0020 F4 — **a atuação**, que é o que faz o
 * painel derivar (ADR-0016 §2). Reusa as três personas do [FakeSessaoUsuario].
 */
class ContextoUsuarioTest {

    private val supervisor = requireNotNull(FakeSessaoUsuario.supervisor().contexto)
    private val agente = requireNotNull(FakeSessaoUsuario.agente().contexto)
    private val plataforma = requireNotNull(FakeSessaoUsuario.plataforma().contexto)

    @Test
    fun `o cargo determina a atuacao — supervisor e agente sao do agenciamento`() {
        assertEquals(Atuacao.AGENCIAMENTO, supervisor.atuacao)
        assertEquals(Atuacao.AGENCIAMENTO, agente.atuacao)
    }

    @Test
    fun `papel de plataforma nao tem atuacao — e isso e a informacao, nao a falta dela`() {
        assertNull(plataforma.atuacao)
        assertNull(plataforma.cargo)
    }

    /**
     * **O cargo em vigor é o do vínculo** (F6.5), e não mais o campo do funcionário: mexer no legado não
     * muda mais nada. O fail-closed do cargo desconhecido não sumiu — mudou de lugar: ele agora acontece
     * na fronteira, onde `Vinculo.de` recusa o cargo ilegível e a pessoa fica sem aquele vínculo.
     */
    @Test
    fun `o campo legado de cargo nao decide mais a atuacao`() {
        val comCargoLegadoEstranho = supervisor.copy(
            funcionario = supervisor.funcionario?.copy(cargo = "GERENTE_DE_PATIO"),
        )

        assertEquals(Funcionario.Cargo.SUPERVISOR.name, comCargoLegadoEstranho.cargo)
        assertEquals(Atuacao.AGENCIAMENTO, comCargoLegadoEstranho.atuacao)
    }

    /** Sem vínculo em vigor não há cargo nem atuação — nem agência a aplicar. */
    @Test
    fun `sem vinculo ativo, nao ha cargo nem agencia`() {
        val semVinculo = supervisor.copy(
            funcionario = supervisor.funcionario?.copy(vinculos = emptyList()),
        )

        assertNull(semVinculo.cargo)
        assertNull(semVinculo.atuacao)
        assertEquals("", semVinculo.agencia)
    }

    /**
     * A agência do bilhete é o **nome da empresa do vínculo ativo** (F6.5) — e some junto com o vínculo,
     * em vez de ficar sobrando de um contexto que não vale mais.
     */
    @Test
    fun `a agencia e o nome da empresa em vigor`() {
        val comEmpresa = supervisor.copy(empresaAtivaNome = "Navegação Norte")

        assertEquals("Navegação Norte", comEmpresa.agencia)
    }

    @Test
    fun `todo cargo declara uma atuacao operante — nenhum nasce sem painel`() {
        Funcionario.Cargo.entries.forEach { cargo ->
            assertTrue("cargo $cargo com atuação dormente", cargo.atuacao.operante)
        }
    }
}