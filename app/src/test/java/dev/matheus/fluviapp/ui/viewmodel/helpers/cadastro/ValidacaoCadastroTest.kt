package dev.matheus.fluviapp.ui.viewmodel.helpers.cadastro

import dev.matheus.fluviapp.ui.states.CadastroUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidacaoCadastroTest {

    private val valido = CadastroUiState(
        nome = "Ana",
        email = "a@b.com",
        senha = "123456",
        confirmarSenha = "123456",
    )

    @Test
    fun `campos completos sao validos`() {
        assertTrue(validarCamposCadastro(valido).camposValidos())
    }

    @Test
    fun `nome em branco marca erro`() {
        val r = validarCamposCadastro(valido.copy(nome = ""))
        assertTrue(r.isNomeError)
        assertFalse(r.camposValidos())
    }

    @Test
    fun `email em branco marca erro`() {
        val r = validarCamposCadastro(valido.copy(email = ""))
        assertTrue(r.isEmailError)
        assertFalse(r.camposValidos())
    }

    @Test
    fun `senha com menos de 6 marca erro`() {
        val r = validarCamposCadastro(valido.copy(senha = "123", confirmarSenha = "123"))
        assertTrue(r.isSenhaError)
        assertFalse(r.camposValidos())
    }

    @Test
    fun `senhas diferentes marcam erro na confirmacao`() {
        val r = validarCamposCadastro(valido.copy(confirmarSenha = "000000"))
        assertTrue(r.isConfirmarSenhaError)
        assertFalse(r.camposValidos())
    }
}