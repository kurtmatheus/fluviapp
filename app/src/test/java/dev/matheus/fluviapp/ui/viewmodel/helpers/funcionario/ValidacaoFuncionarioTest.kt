package dev.matheus.fluviapp.ui.viewmodel.helpers.funcionario

import dev.matheus.fluviapp.domain.operacoes.Funcionario.Cargo
import dev.matheus.fluviapp.domain.operacoes.Vinculo
import dev.matheus.fluviapp.ui.states.FormFuncionarioUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidacaoFuncionarioTest {

    private fun estadoValido() = FormFuncionarioUiState(
        nome = "Ana",
        email = "ana@fluviapp.com.br",
        vinculos = listOf(Vinculo("empresa-1", Cargo.AGENTE)),
    )

    @Test
    fun `campos em branco sao invalidos`() {
        val erros = validarFuncionario(FormFuncionarioUiState())

        assertTrue(erros.nome)
        assertTrue(erros.email)
        assertTrue(erros.vinculos)
        assertFalse(erros.valido)
    }

    @Test
    fun `estado completo e valido`() {
        assertTrue(validarFuncionario(estadoValido()).valido)
    }

    /**
     * O vínculo é obrigatório no cadastro mesmo sendo opcional no documento: quem cadastra sabe para
     * qual empresa está contratando, e uma pessoa sem vínculo não enxerga seção nenhuma nem emite nada.
     */
    @Test
    fun `nome e e-mail sem vinculo ainda e invalido`() {
        val erros = validarFuncionario(estadoValido().copy(vinculos = emptyList()))

        assertFalse(erros.valido)
        assertTrue(erros.vinculos)
        assertFalse(erros.nome)
        assertFalse(erros.email)
    }

    // --- E-mail: é a CHAVE do primeiro acesso (ADR-0015 §2.1), então tem forma, não só presença ---

    @Test
    fun `e-mail sem arroba ou sem dominio e invalido`() {
        listOf("ana", "ana@", "@fluviapp.com.br", "ana@fluviapp", "ana fulana@x.com", "").forEach {
            assertTrue("deveria recusar '$it'", validarFuncionario(estadoValido().copy(email = it)).email)
        }
    }

    @Test
    fun `e-mail com espacos ao redor e aceito — o VM grava aparado`() {
        assertFalse(validarFuncionario(estadoValido().copy(email = "  ana@fluviapp.com.br  ")).email)
    }
}