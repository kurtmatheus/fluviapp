package br.com.gruponaveg.ui.viewmodel.helpers.passagem.validacao

import br.com.gruponaveg.R
import br.com.gruponaveg.ui.states.passagem.FormPassageiroUiState
import br.com.gruponaveg.ui.states.passagem.FormPassagemUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ValidacaoFormPassageiroHelper(
    private val uiState: MutableStateFlow<FormPassageiroUiState>,
    private val uiStatePassagem: MutableStateFlow<FormPassagemUiState>,
) {

    fun isFormularioPassageiroValido(): Boolean {
        validarFormulario(uiState.value)

        val passageiro2valido = (uiState.value.isPassageiro2Checked &&
                !uiState.value.isTipoDocumentoPassageiro2Error &&
                !uiState.value.isDocumentoPassageiro2Error &&
                !uiState.value.isNomePassageiro2Error &&
                !uiState.value.isDataNascimentoPassageiro2Error) || !uiState.value.isPassageiro2Checked

        val passageiro3valido = (uiState.value.isPassageiro3Checked &&
                !uiState.value.isTipoDocumentoPassageiro3Error &&
                !uiState.value.isDocumentoPassageiro3Error &&
                !uiState.value.isNomePassageiro3Error &&
                !uiState.value.isDataNascimentoPassageiro3Error) || !uiState.value.isPassageiro3Checked

        return !uiState.value.isTipoDocumentoPassageiro1Error &&
                !uiState.value.isDocumentoPassageiro1Error &&
                !uiState.value.isNomePassageiro1Error &&
                !uiState.value.isDataNascimentoPassageiro1Error &&
                !uiState.value.isAcomodacaoError &&
                !uiState.value.isTipoPassagemError &&
                !uiState.value.isTipoGratuidadeError &&
                passageiro2valido &&
                passageiro3valido
    }

    private fun validarFormulario(state: FormPassageiroUiState) {
        if (state.acomodacao.isBlank()) {
            uiState.update {
                it.copy(
                    isAcomodacaoError = true
                )
            }
        }

        if (state.tipoPassagem.isBlank()) {
            uiState.update {
                it.copy(
                    isTipoPassagemError = true
                )
            }
        }

        if (state.isGratuidade && state.tipoGratuidade.isBlank()) {
            uiState.update {
                it.copy(
                    isTipoGratuidadeError = true
                )
            }
        }

        if (state.tipoDocumentoPassageiro3.isNotBlank() && state.documentoPassageiro1.isBlank()) {
            uiState.update {
                it.copy(
                    isDocumentoPassageiro1Error = true
                )
            }
        }

        if (state.nomePassageiro1.isBlank()) {
            uiState.update {
                it.copy(
                    isNomePassageiro1Error = true
                )
            }
        }

        val gratuidadeCrianca = state.isGratuidade &&
                state.tipoGratuidade.contains("CRIANCA", true)

        if (state.dataNascimentoPassageiro1.isBlank()) {
            uiState.update {
                it.copy(
                    isDataNascimentoPassageiro1Error = true,
                    textDataNascimentoError = R.string.error_camp_obrig
                )
            }
        } else {
            if (gratuidadeCrianca &&
                LocalDate.parse(
                    state.dataNascimentoPassageiro1,
                    DateTimeFormatter.ofPattern("dd/MM/yyyy")
                ).isBefore(
                    LocalDate.parse(
                        uiStatePassagem.value.dataViagem,
                        DateTimeFormatter.ofPattern("dd/MM/yyyy")
                    ).minusYears(6).plusDays(1)
                )
            ) {
                uiState.update {
                    it.copy(
                        isDataNascimentoPassageiro1Error = true,
                        textDataNascimentoError = R.string.error_data_crianca
                    )
                }
            }
        }

        if (state.isPassageiro2Checked) {
            if (state.nomePassageiro2.isBlank() &&
                state.documentoPassageiro2.isBlank()
            ) {
                uiState.update {
                    it.copy(
                        isNomePassageiro2Error = true
                    )
                }
            }

            if (state.tipoDocumentoPassageiro2.isNotBlank() &&
                state.documentoPassageiro2.isBlank()
            ) {
                uiState.update {
                    it.copy(
                        isDocumentoPassageiro2Error = true
                    )
                }
            }

            if (state.dataNascimentoPassageiro2.isBlank()) {
                uiState.update {
                    it.copy(
                        isDataNascimentoPassageiro2Error = true
                    )
                }
            }
        }

        if (state.isPassageiro3Checked) {
            if (state.tipoDocumentoPassageiro3.isNotBlank() &&
                state.documentoPassageiro1.isBlank()
            ) {
                uiState.update {
                    it.copy(
                        isDocumentoPassageiro3Error = true
                    )
                }
            }

            if (state.nomePassageiro3.isBlank()) {
                uiState.update {
                    it.copy(
                        isNomePassageiro3Error = true
                    )
                }
            }

            if (state.dataNascimentoPassageiro3.isBlank()) {
                uiState.update {
                    it.copy(
                        isDataNascimentoPassageiro3Error = true
                    )
                }
            }
        }
    }
}
