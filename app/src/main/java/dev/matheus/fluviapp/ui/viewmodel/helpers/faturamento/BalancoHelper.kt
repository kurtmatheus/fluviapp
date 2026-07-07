package dev.matheus.fluviapp.ui.viewmodel.helpers.faturamento

import dev.matheus.fluviapp.model.screendata.DadosBalancoPassagem
import dev.matheus.fluviapp.ui.states.faturamento.BalancoState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class BalancoHelper(
    private val uiState: MutableStateFlow<BalancoState>,
) {

    init {
        atualizarCampos()
    }

    private fun atualizarCampos() {
        uiState.update { state ->
            state.copy(
                onDataViagemChange = {
                    atualizaDataViagem(it)
                }
            )
        }
    }

    private fun atualizaDataViagem(data: String) {
        uiState.update {
            it.copy(
                dataViagem = data,
                isDataViagemError = false
            )
        }
    }

    fun atualizarDadosBalanco(dados: List<DadosBalancoPassagem>) {
        uiState.update {
            it.copy(
                listaDadosBalancoPassagens = dados,
                jaFoiGerado = true
            )
        }
    }

    fun validarFormulario(): Boolean {
        if (uiState.value.dataViagem.isBlank()) {
            uiState.update {
                it.copy(
                    isDataViagemError = true
                )
            }
        }
        return !uiState.value.isDataViagemError
    }

    fun atualizarProcessamento() {
        uiState.update {
            it.copy(
                isProcessing = !it.isProcessing
            )
        }
    }
}
