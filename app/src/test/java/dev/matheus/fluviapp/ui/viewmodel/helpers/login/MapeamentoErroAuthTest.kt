package dev.matheus.fluviapp.ui.viewmodel.helpers.login

import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.services.repository.firebase.autenticacao.MotivoFalhaAuth
import org.junit.Assert.assertEquals
import org.junit.Test

/** Agora 100% puro (enum de domínio, sem tipos do Firebase) — todos os ramos cobertos. */
class MapeamentoErroAuthTest {

    @Test
    fun `credencial invalida mapeia para usuario_incorreto`() {
        assertEquals(R.string.error_usuario_incorreto, mapearMensagemErroAuth(MotivoFalhaAuth.CREDENCIAL_INVALIDA))
    }

    @Test
    fun `usuario inexistente mapeia para inexistente`() {
        assertEquals(R.string.error_usuario_inexistente, mapearMensagemErroAuth(MotivoFalhaAuth.USUARIO_INEXISTENTE))
    }

    @Test
    fun `desconhecido mapeia para falha_auth`() {
        assertEquals(R.string.error_falha_auth, mapearMensagemErroAuth(MotivoFalhaAuth.DESCONHECIDO))
    }
}