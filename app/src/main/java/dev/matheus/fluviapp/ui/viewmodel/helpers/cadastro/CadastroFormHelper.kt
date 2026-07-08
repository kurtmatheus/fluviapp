package dev.matheus.fluviapp.ui.viewmodel.helpers.cadastro

import dev.matheus.fluviapp.ui.states.CadastroUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class CadastroFormHelper(
    private val uiState: MutableStateFlow<CadastroUiState>,
) {
    init {
        initializeFields()
    }

    private fun initializeFields() {
        uiState.update { state ->
            state.copy(
                onNomeChange = { valor -> uiState.update { it.copy(nome = valor, isNomeError = false) } },
                onEmailChange = { valor -> uiState.update { it.copy(email = valor, isEmailError = false) } },
                onSenhaChange = { valor -> uiState.update { it.copy(senha = valor, isSenhaError = false) } },
                onConfirmarSenhaChange = { valor ->
                    uiState.update { it.copy(confirmarSenha = valor, isConfirmarSenhaError = false) }
                },
            )
        }
    }

    fun isFormularioValido(): Boolean {
        uiState.update { validarCamposCadastro(it) }
        return uiState.value.camposValidos()
    }

    fun updateSenhaVisible() {
        uiState.update { it.copy(isSenhaVisible = !it.isSenhaVisible) }
    }

    fun exibeErro() {
        uiState.update { it.copy(exibirErro = true) }
    }

    fun setMensagemErro(mensagemErro: Int) {
        uiState.update { it.copy(mensagemErro = mensagemErro) }
    }
}