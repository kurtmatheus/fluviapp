package dev.matheus.fluviapp.ui.viewmodel.helpers.viagem

import dev.matheus.fluviapp.ui.states.FormViagemUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidacaoViagemTest {

    @Test
    fun `campos em branco sao invalidos`() {
        val erros = validarViagem(FormViagemUiState())
        assertTrue(erros.empresa)
        assertTrue(erros.navio)
        assertTrue(erros.trechoOrigem)
        assertTrue(erros.trechoDestino)
        assertFalse(erros.valido)
    }

    @Test
    fun `estado completo e valido`() {
        val erros = validarViagem(
            FormViagemUiState(
                empresa = "NAVEGACAO MODELO",
                navio = "F/B Modelo",
                trechoOrigem = "Porto Norte",
                trechoDestino = "Ilha Central",
            ),
        )
        assertTrue(erros.valido)
    }

    @Test
    fun `empresa passa a ser validada`() {
        val erros = validarViagem(
            FormViagemUiState(navio = "F/B", trechoOrigem = "A", trechoDestino = "B"),
        )
        assertTrue(erros.empresa)
        assertFalse(erros.valido)
    }
}
