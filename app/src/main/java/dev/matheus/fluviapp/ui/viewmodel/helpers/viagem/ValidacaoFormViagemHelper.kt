package dev.matheus.fluviapp.ui.viewmodel.helpers.viagem

import dev.matheus.fluviapp.ui.states.FormViagemUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class ValidacaoFormViagemHelper(
    private val uiState: MutableStateFlow<FormViagemUiState>
) {

    fun isFormularioValido(): Boolean {
        validarCampos(uiState.value)
        return !uiState.value.isNavioError &&
                !uiState.value.isTrechoOrigemError &&
                !uiState.value.isTrechoDestinoError
    }

    private fun validarCampos(state: FormViagemUiState) {

        if (state.navio.isBlank()) {
            uiState.update {
                it.copy(
                    isNavioError = true
                )
            }
        }

        if (state.trechoOrigem.isBlank()) {
            uiState.update {
                it.copy(
                    isTrechoOrigemError = true
                )
            }
        }

        if (state.trechoDestino.isBlank()) {
            uiState.update {
                it.copy(
                    isTrechoDestinoError = true
                )
            }
        }
    }
}

