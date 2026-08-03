package dev.matheus.fluviapp.ui.viewmodel.helpers.navio

import dev.matheus.fluviapp.revitalizacao.ForaDoEscopo
import org.junit.experimental.categories.Category
import dev.matheus.fluviapp.ui.states.FormNavioUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@Category(ForaDoEscopo::class)
class ValidacaoNavioTest {

    @Test
    fun `campos obrigatorios em branco sao invalidos`() {
        val erros = validarNavio(FormNavioUiState())
        assertTrue(erros.nome)
        assertTrue(erros.empresa)
        assertFalse(erros.valido)
    }

    @Test
    fun `nome e empresa preenchidos sao validos (capacidades opcionais)`() {
        val erros = validarNavio(FormNavioUiState(nome = "FLUVI I", empresa = "ACME"))
        assertFalse(erros.nome)
        assertFalse(erros.empresa)
        assertTrue(erros.valido)
    }

    @Test
    fun `so o nome sem empresa e invalido`() {
        val erros = validarNavio(FormNavioUiState(nome = "FLUVI I"))
        assertFalse(erros.nome)
        assertTrue(erros.empresa)
        assertFalse(erros.valido)
    }
}
