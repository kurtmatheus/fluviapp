package dev.matheus.fluviapp.ui.viewmodel.helpers.cadastro

import dev.matheus.fluviapp.ui.states.CadastroUiState

const val TAMANHO_MINIMO_SENHA = 6

/**
 * Regra de validação do cadastro, pura (só sobre o state; sem rede), logo JVM-testável.
 * Nome/e-mail obrigatórios; senha >= [TAMANHO_MINIMO_SENHA] (exigência do Firebase); confirmação
 * igual à senha.
 */
internal fun validarCamposCadastro(state: CadastroUiState): CadastroUiState = state.copy(
    isNomeError = state.nome.isBlank(),
    isEmailError = state.email.isBlank(),
    isSenhaError = state.senha.length < TAMANHO_MINIMO_SENHA,
    isConfirmarSenhaError = state.confirmarSenha.isBlank() || state.confirmarSenha != state.senha,
)

internal fun CadastroUiState.camposValidos(): Boolean =
    !isNomeError && !isEmailError && !isSenhaError && !isConfirmarSenhaError