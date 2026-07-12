package dev.matheus.fluviapp.ui.viewmodel.helpers.agente

import dev.matheus.fluviapp.ui.states.FormAgenteUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidacaoAgenteTest {

    @Test
    fun `campos em branco sao invalidos`() {
        val erros = validarAgente(FormAgenteUiState())
        assertTrue(erros.agencia)
        assertTrue(erros.agente)
        assertTrue(erros.lotacao)
        assertFalse(erros.valido)
    }

    @Test
    fun `estado completo e valido`() {
        val erros = validarAgente(
            FormAgenteUiState(agencia = "MATRIZ", agente = "Ana", lotacao = "PORTO NORTE"),
        )
        assertTrue(erros.valido)
    }

    @Test
    fun `apenas nome preenchido ainda e invalido`() {
        val erros = validarAgente(FormAgenteUiState(agente = "Ana"))
        assertFalse(erros.valido)
        assertFalse(erros.agente)
        assertTrue(erros.agencia)
        assertTrue(erros.lotacao)
    }
}
