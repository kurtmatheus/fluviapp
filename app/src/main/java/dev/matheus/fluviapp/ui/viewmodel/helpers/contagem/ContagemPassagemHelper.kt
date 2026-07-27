package dev.matheus.fluviapp.ui.viewmodel.helpers.contagem

import dev.matheus.fluviapp.model.screendata.DadosContagemPassagem
import dev.matheus.fluviapp.ui.states.contagem.ContagemPassagemUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class ContagemPassagemHelper(
    private val uiState: MutableStateFlow<ContagemPassagemUiState>,
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

    fun atualizarDadosContagem(dados: List<DadosContagemPassagem>) {
        uiState.update {
            it.copy(
                listaDadosContagemPassagens = dados,
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
