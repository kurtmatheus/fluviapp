package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.validacao

import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.MOTO
import dev.matheus.fluviapp.ui.states.passagem.FormVeiculoUiState
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
                // cor é opcional (nunca setada) — removido o check morto que a fingia validar.
                !uiState.value.isCilindradaError
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

        // Moto exige cilindrada (ADR-0013): ela alimenta a tarifaMotoBase. Validar aqui evita o bloqueio
        // enganoso "sem tarifa cadastrada" na emissão (a causa real seria o cc ausente).
        if (state.tipoVeiculo == MOTO.name && state.cilindrada.isBlank()) {
            uiState.update {
                it.copy(
                    isCilindradaError = true
                )
            }
        }
    }
}
