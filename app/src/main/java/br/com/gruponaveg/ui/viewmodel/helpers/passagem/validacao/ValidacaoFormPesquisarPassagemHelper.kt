package br.com.gruponaveg.ui.viewmodel.helpers.passagem.validacao

import br.com.gruponaveg.ui.states.passagem.PesquisarPassagemUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class ValidacaoFormPesquisarPassagemHelper(
    private val uiState: MutableStateFlow<PesquisarPassagemUiState>,
) {
    fun isFormularioValido(): Boolean {
        validarCampos()
        return !uiState.value.isDataError &&
                !uiState.value.isSituacaoError &&
                !uiState.value.isFiltroError
    }

    private fun validarCampos() {

        if (uiState.value.data.isBlank()) {
            uiState.update {
                it.copy(
                    isDataError = true
                )
            }
        }

        if (uiState.value.situacao.isBlank()) {
            uiState.update {
                it.copy(
                    isSituacaoError = true
                )
            }
        }

        if (!uiState.value.isVeiculoChecked && !uiState.value.isPassageiroChecked) {
            uiState.update {
                it.copy(
                    isFiltroError = true
                )
            }
        }
    }
}
