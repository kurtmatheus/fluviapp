package br.com.gruponaveg.ui.viewmodel.helpers.passagem.validacao

import br.com.gruponaveg.ui.states.passagem.FormVeiculoUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class ValidacaoFormVeiculoHelper(
    private val uiState: MutableStateFlow<FormVeiculoUiState>,
) {

    fun isFormularioVeiculoValido(): Boolean {
        validarFormulario(uiState.value)
        return !uiState.value.isTipoDocumentoResponsavelRetiradaError &&
                !uiState.value.isDocumentoResponsavelRetiradaError &&
                !uiState.value.isNomeResponsavelRetiradaError &&
                !uiState.value.isTipoVeiculoError &&
                !uiState.value.isModeloVeiculoError &&
                !uiState.value.isPlacaVeiculoError &&
                !uiState.value.isCorVeiculoError
    }

    private fun validarFormulario(state: FormVeiculoUiState) {
        if (!state.isDocumentoResponsavelRetiradaReadOnly && state.documentoResponsavelRetirada.isBlank()) {
            uiState.update {
                it.copy(
                    isDocumentoResponsavelRetiradaError = true
                )
            }
        }

        if (state.tipoVeiculo.isBlank()) {
            uiState.update {
                it.copy(
                    isTipoVeiculoError = true
                )
            }
        }

        if (state.modeloVeiculo.isBlank()) {
            uiState.update {
                it.copy(
                    isModeloVeiculoError = true
                )
            }
        }

        if (state.placaVeiculo.isBlank()) {
            uiState.update {
                it.copy(
                    isPlacaVeiculoError = true
                )
            }
        }
    }
}
