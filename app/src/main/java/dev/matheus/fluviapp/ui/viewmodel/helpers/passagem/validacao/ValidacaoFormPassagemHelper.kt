package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.validacao

import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.extensions.converterParaLocalDate
import dev.matheus.fluviapp.ui.states.passagem.FormPassageiroUiState
import dev.matheus.fluviapp.ui.states.passagem.FormPassagemUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

class ValidacaoFormPassagemHelper(
    private val uiState: MutableStateFlow<FormPassagemUiState>,
    private val uiStatePassageiro: MutableStateFlow<FormPassageiroUiState>
) {

    fun isFormularioPassagemValido(): Boolean {
        validarFormulario(uiState.value)

        return !uiState.value.isDataViagemError &&
                !uiState.value.isHoraViagemError &&
//                !uiState.value.isAgenciaError &&
//                !uiState.value.isAgenteError &&
                !uiState.value.isFormaPagamentoError &&
                !uiState.value.isValorPagoError &&
                !uiState.value.isValorPixError &&
                !uiState.value.isValorDinheiroError &&
                !uiState.value.isValorDebitoError &&
                !uiState.value.isValorCreditoError
    }

    private fun validarFormulario(state: FormPassagemUiState) {
        when {
            state.dataViagem.isBlank() && !state.isEditing -> uiState.update {
                it.copy(
                    isDataViagemError = true,
                    textDataViagemError = R.string.error_camp_obrig
                )
            }

            state.dataViagem.converterParaLocalDate().isBefore(LocalDate.now()) && !state.isEditing -> uiState.update {
                it.copy(
                    isDataViagemError = true,
                    textDataViagemError = R.string.error_data_menor
                )
            }

            else -> uiState.update {
                it.copy(
                    isDataViagemError = false,
                    textDataViagemError = 0
                )
            }
        }

        if (state.horaViagem.isBlank()) {
            uiState.update {
                it.copy(
                    isHoraViagemError = true
                )
            }
        }

        if (state.agencia.isBlank()) {
            uiState.update {
                it.copy(
                    isAgenciaError = true
                )
            }
        }

        if (state.agente.isBlank()) {
            uiState.update {
                it.copy(
                    isAgenteError = true
                )
            }
        }

        if ((!state.isFormaPagamentoEnabled && !uiStatePassageiro.value.isGratuidade) &&
            state.valorPago.isBlank()) {
            uiState.update {
                it.copy(
                    isValorPagoError = true
                )
            }
        }

        val isFormaPagamentoError = state.isFormaPagamentoEnabled &&
                !state.isPixChecked &&
                !state.isDinheiroChecked &&
                !state.isDebitoChecked &&
                !state.isCreditoChecked &&
                !uiStatePassageiro.value.isGratuidade

        if (isFormaPagamentoError) {
            uiState.update {
                it.copy(
                    isFormaPagamentoError = true
                )
            }
        }

        if (state.isFormaPagamentoEnabled &&
            state.isPixChecked &&
            state.valorPix.isBlank()) {
            uiState.update {
                it.copy(
                    isValorPixError = true
                )
            }
        }

        if (state.isFormaPagamentoEnabled &&
            state.isDinheiroChecked &&
            state.valorDinheiro.isBlank()) {
            uiState.update {
                it.copy(
                    isValorDinheiroError = true
                )
            }
        }

        if (state.isFormaPagamentoEnabled &&
            state.isDebitoChecked &&
            state.valorDebito.isBlank()) {
            uiState.update {
                it.copy(
                    isValorDebitoError = true
                )
            }
        }

        if (state.isFormaPagamentoEnabled &&
            state.isCreditoChecked &&
            state.valorCredito.isBlank()
        ) {
            uiState.update {
                it.copy(
                    isValorCreditoError = true
                )
            }
        }


    }
}
