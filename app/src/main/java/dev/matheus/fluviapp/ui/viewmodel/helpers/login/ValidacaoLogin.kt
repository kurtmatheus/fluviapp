package dev.matheus.fluviapp.ui.viewmodel.helpers.login

import dev.matheus.fluviapp.ui.states.LoginUiState

/**
 * Regra de validação do formulário de login, pura (só sobre o state; sem rede/repo), logo
 * JVM-testável. Marca erro quando o campo está em branco; não limpa erro já existente.
 */
internal fun validarCamposLogin(state: LoginUiState): LoginUiState = state.copy(
    isUsuarioError = state.isUsuarioError || state.email.isBlank(),
    isSenhaError = state.isSenhaError || state.senha.isBlank(),
)

internal fun LoginUiState.camposValidos(): Boolean = !isUsuarioError && !isSenhaError