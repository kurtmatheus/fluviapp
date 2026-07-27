package dev.matheus.fluviapp.ui.viewmodel.helpers.login

import dev.matheus.fluviapp.ui.states.PrimeiroAcessoUiState

/**
 * Validação da criação de senha no primeiro acesso — pura e JVM-testável.
 *
 * O mínimo de 6 caracteres não é escolha de gosto: é o piso que o **Firebase Auth** aceita em
 * `updatePassword`. Validar aqui transforma uma rejeição de rede (mensagem genérica, depois do clique)
 * em erro de campo, antes de sair do aparelho.
 */
const val TAMANHO_MINIMO_SENHA = 6

data class ErrosPrimeiroAcesso(
    val senha: Boolean = false,
    val confirmacao: Boolean = false,
) {
    val valido: Boolean get() = !senha && !confirmacao
}

fun validarPrimeiroAcesso(state: PrimeiroAcessoUiState): ErrosPrimeiroAcesso = ErrosPrimeiroAcesso(
    senha = state.senha.length < TAMANHO_MINIMO_SENHA,
    // Confirmação só é "erro dela" quando difere de uma senha que já é válida; senão os dois campos
    // acendem por um problema só e a pessoa não sabe qual corrigir.
    confirmacao = state.senha.length >= TAMANHO_MINIMO_SENHA && state.confirmacao != state.senha,
)