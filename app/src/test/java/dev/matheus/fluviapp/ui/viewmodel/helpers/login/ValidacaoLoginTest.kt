package dev.matheus.fluviapp.ui.viewmodel.helpers.login

import dev.matheus.fluviapp.ui.states.LoginUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidacaoLoginTest {

    @Test
    fun `campos preenchidos sao validos`() {
        val r = validarCamposLogin(LoginUiState(email = "a@b.com", senha = "123"))
        assertFalse(r.isUsuarioError)
        assertFalse(r.isSenhaError)
        assertTrue(r.camposValidos())
    }

    @Test
    fun `email em branco marca erro e invalida`() {
        val r = validarCamposLogin(LoginUiState(email = "", senha = "123"))
        assertTrue(r.isUsuarioError)
        assertFalse(r.camposValidos())
    }

    @Test
    fun `senha em branco marca erro e invalida`() {
        val r = validarCamposLogin(LoginUiState(email = "a@b.com", senha = ""))
        assertTrue(r.isSenhaError)
        assertFalse(r.camposValidos())
    }

    @Test
    fun `ambos em branco marcam erro`() {
        val r = validarCamposLogin(LoginUiState())
        assertTrue(r.isUsuarioError)
        assertTrue(r.isSenhaError)
        assertFalse(r.camposValidos())
    }
}