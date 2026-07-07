package br.com.gruponaveg.ui.viewmodel.helpers.viagem

import br.com.gruponaveg.R
import br.com.gruponaveg.ui.states.PesquisarViagemUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class ValidacaoFormPesquisarViagemHelper(
    private val uiState: MutableStateFlow<PesquisarViagemUiState>
) {

    fun isFormularioValido(): Boolean {
        validarCampos(uiState.value)
        return !uiState.value.isEmpresaError &&
                !uiState.value.isNavioError &&
                !uiState.value.isTrechoError
    }

    private fun validarCampos(state: PesquisarViagemUiState) {

        if (state.isCheckedEmpresa && state.empresa.isBlank()) {
            uiState.update {
                it.copy(
                    isEmpresaError = true
                )
            }
        }

        if (state.isCheckedNavio && state.navio.isBlank()) {
            uiState.update {
                it.copy(
                    isNavioError = true
                )
            }
        }

        if (state.isCheckedTrecho &&
            state.origem.isBlank() &&
            state.destino.isBlank()) {
            uiState.update {
                it.copy(
                    isTrechoError = true,
                    textTrechoError = R.string.error_selecione_opcao
                )
            }
        }
    }
}

