package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.validacao

import dev.matheus.fluviapp.ui.states.passagem.FormVeiculoUiState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidacaoFormVeiculoHelperTest {

    private fun validar(state: FormVeiculoUiState): Pair<Boolean, FormVeiculoUiState> {
        val flow = MutableStateFlow(state)
        val valido = ValidacaoFormVeiculoHelper(flow).isFormularioVeiculoValido()
        return valido to flow.value
    }

    @Test
    fun `veiculo com tipo, modelo e placa (doc read-only) e valido`() {
        val (valido, _) = validar(
            FormVeiculoUiState(tipoVeiculo = "CARRO", modeloVeiculo = "Modelo X", placaVeiculo = "ABC1D23"),
        )
        assertTrue(valido)
    }

    @Test
    fun `campos de veiculo em branco marcam erro e invalidam`() {
        val (valido, s) = validar(FormVeiculoUiState())
        assertFalse(valido)
        assertTrue(s.isTipoVeiculoError)
        assertTrue(s.isModeloVeiculoError)
        assertTrue(s.isPlacaVeiculoError)
    }

    @Test
    fun `documento do responsavel obrigatorio quando nao e read-only`() {
        val (valido, s) = validar(
            FormVeiculoUiState(
                tipoVeiculo = "CARRO",
                modeloVeiculo = "Modelo X",
                placaVeiculo = "ABC1D23",
                isDocumentoResponsavelRetiradaReadOnly = false,
                documentoResponsavelRetirada = "",
            ),
        )
        assertFalse(valido)
        assertTrue(s.isDocumentoResponsavelRetiradaError)
    }
}