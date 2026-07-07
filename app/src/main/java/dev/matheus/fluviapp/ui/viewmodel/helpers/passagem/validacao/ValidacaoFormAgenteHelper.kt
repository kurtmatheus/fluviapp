package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.validacao

import dev.matheus.fluviapp.ui.states.AgenteUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class ValidacaoFormAgenteHelper(
    private val uiState: MutableStateFlow<AgenteUiState>,
) {

    fun isFormularioValido(): Boolean {
        validarCampos()
        return !uiState.value.isAgenteError &&
                !uiState.value.isAgenciaError &&
                !uiState.value.isLotacaoError
    }

    private fun validarCampos() {
        if (uiState.value.agente.isBlank()) {
            uiState.update { it.copy(isAgenteError = true) }
        }

        if (uiState.value.agencia.isBlank()) {
            uiState.update { it.copy(isAgenciaError = true) }
        }

        if (uiState.value.lotacao.isBlank()) {
            uiState.update { it.copy(isLotacaoError = true) }
        }
    }
}
