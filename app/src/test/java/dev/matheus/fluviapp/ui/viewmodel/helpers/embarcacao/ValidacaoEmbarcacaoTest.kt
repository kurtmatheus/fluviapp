package dev.matheus.fluviapp.ui.viewmodel.helpers.embarcacao

import dev.matheus.fluviapp.ui.states.FormEmbarcacaoUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidacaoEmbarcacaoTest {

    @Test
    fun `campos obrigatorios em branco sao invalidos`() {
        val erros = validarEmbarcacao(FormEmbarcacaoUiState())
        assertTrue(erros.nome)
        assertTrue(erros.empresa)
        assertFalse(erros.valido)
    }

    @Test
    fun `nome e empresa preenchidos sao validos (capacidades opcionais)`() {
        val erros = validarEmbarcacao(FormEmbarcacaoUiState(nome = "FLUVI I", empresa = "ACME"))
        assertFalse(erros.nome)
        assertFalse(erros.empresa)
        assertTrue(erros.valido)
    }

    @Test
    fun `so o nome sem empresa e invalido`() {
        val erros = validarEmbarcacao(FormEmbarcacaoUiState(nome = "FLUVI I"))
        assertFalse(erros.nome)
        assertTrue(erros.empresa)
        assertFalse(erros.valido)
    }
}
