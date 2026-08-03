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

    @Test
    fun `cargo desconhecido nao inventa atuacao (fail-closed)`() {
        val comCargoEstranho = supervisor.copy(
            funcionario = supervisor.funcionario?.copy(cargo = "GERENTE_DE_PATIO"),
        )

        assertNull(comCargoEstranho.atuacao)
    }

    @Test
    fun `todo cargo declara uma atuacao operante — nenhum nasce sem painel`() {
        Funcionario.Cargo.entries.forEach { cargo ->
            assertTrue("cargo $cargo com atuação dormente", cargo.atuacao.operante)
        }
    }
}