package dev.matheus.fluviapp.ui.viewmodel.helpers.funcionario

import dev.matheus.fluviapp.ui.states.FormFuncionarioUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidacaoFuncionarioTest {

    @Test
    fun `campos em branco sao invalidos`() {
        val erros = validarFuncionario(FormFuncionarioUiState())
        assertTrue(erros.agencia)
        assertTrue(erros.funcionario)
        assertTrue(erros.lotacao)
        assertFalse(erros.valido)
    }

    @Test
    fun `estado completo e valido`() {
        val erros = validarFuncionario(
            FormFuncionarioUiState(agencia = "MATRIZ", funcionario = "Ana", lotacao = "PORTO NORTE"),
        )
        assertTrue(erros.valido)
    }

    @Test
    fun `apenas nome preenchido ainda e invalido`() {
        val erros = validarFuncionario(FormFuncionarioUiState(funcionario = "Ana"))
        assertFalse(erros.valido)
        assertFalse(erros.funcionario)
        assertTrue(erros.agencia)
        assertTrue(erros.lotacao)
    }
}
