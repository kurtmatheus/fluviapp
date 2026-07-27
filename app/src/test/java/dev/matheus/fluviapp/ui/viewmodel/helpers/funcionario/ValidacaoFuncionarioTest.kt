package dev.matheus.fluviapp.ui.viewmodel.helpers.funcionario

import dev.matheus.fluviapp.ui.states.FormFuncionarioUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidacaoFuncionarioTest {

    private fun estadoValido() = FormFuncionarioUiState(
        agencia = "MATRIZ",
        funcionario = "Ana",
        email = "ana@fluviapp.com.br",
        lotacao = "PORTO NORTE",
    )

    @Test
    fun `campos em branco sao invalidos`() {
        val erros = validarFuncionario(FormFuncionarioUiState())
        assertTrue(erros.agencia)
        assertTrue(erros.funcionario)
        assertTrue(erros.email)
        assertTrue(erros.lotacao)
        assertFalse(erros.valido)
    }

    @Test
    fun `estado completo e valido`() {
        assertTrue(validarFuncionario(estadoValido()).valido)
    }

    @Test
    fun `apenas nome preenchido ainda e invalido`() {
        val erros = validarFuncionario(FormFuncionarioUiState(funcionario = "Ana"))
        assertFalse(erros.valido)
        assertFalse(erros.funcionario)
        assertTrue(erros.agencia)
        assertTrue(erros.email)
        assertTrue(erros.lotacao)
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