package dev.matheus.fluviapp.domain.operacoes

import dev.matheus.fluviapp.fakes.FakeSessaoUsuario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
            funcionario = supervisor.funcionario?.copy(vinculo = null),
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
        val comEmpresa = supervisor.copy(empresaDoVinculo = "Navegação Norte")

        assertEquals("Navegação Norte", comEmpresa.agencia)
    }

    // --- A lente dos dois perfis ([ADR-0032] D5) ---

    /** O `ADM` que também é funcionário: dois perfis no mesmo uid, e a troca é do menu dele. */
    private val admComEmpresa = plataforma.copy(
        funcionario = supervisor.funcionario,
        empresaDoVinculo = "Navegação Norte",
    )

    /**
     * **A lente aparece num ponto só**: o vínculo existe e não está em vigor. É daí que atuação, cargo e
     * agência somem juntos — e é por isso que a troca não precisou tocar em cada um deles.
     */
    @Test
    fun `sob o perfil de plataforma, o vinculo existe e nao esta em vigor`() {
        assertEquals(Perfil.PLATAFORMA, admComEmpresa.perfilAtivo)
        assertNull(admComEmpresa.vinculoAtivo)
        assertNull(admComEmpresa.atuacao)
        assertNull(admComEmpresa.cargo)
        // O nome da empresa **fica**: é o que o menu usa para dizer "operar como Navegação Norte".
        assertEquals("Navegação Norte", admComEmpresa.empresaDoVinculo)
        // A agência do bilhete, não: ninguém emite em nome de uma agência pela qual não está olhando.
        assertEquals("", admComEmpresa.agencia)
    }

    @Test
    fun `sob o perfil de empresa, o mesmo contexto vira operacao`() {
        val naEmpresa = admComEmpresa.copy(perfilEscolhido = Perfil.EMPRESA)

        assertEquals(Perfil.EMPRESA, naEmpresa.perfilAtivo)
        assertEquals(Atuacao.AGENCIAMENTO, naEmpresa.atuacao)
        assertEquals(Funcionario.Cargo.SUPERVISOR.name, naEmpresa.cargo)
        assertEquals("Navegação Norte", naEmpresa.agencia)
        // E o papel **não muda**: a troca é lente, não redução de poder.
        assertEquals(Usuario.Papel.ADM.name, naEmpresa.papel)
    }

    @Test
    fun `a opcao de trocar so existe para quem tem os dois perfis`() {
        assertTrue(admComEmpresa.podeTrocarPerfil)
        assertFalse(plataforma.podeTrocarPerfil)
        assertFalse(supervisor.podeTrocarPerfil)
    }

    @Test
    fun `todo cargo declara uma atuacao operante — nenhum nasce sem painel`() {
        Funcionario.Cargo.entries.forEach { cargo ->
            assertTrue("cargo $cargo com atuação dormente", cargo.atuacao.operante)
        }
    }
}