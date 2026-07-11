package dev.matheus.fluviapp.ui.states

data class RecuperarSenhaUiState(
    val email: String = "",
    val onEmailChange: (String) -> Unit = {},
    val isEmailError: Boolean = false,

    val exibirMensagem: Boolean = false,
    val mensagem: Int = 0,
    /** true = feedback de sucesso (accent), false = erro (vermelho). */
    val isSucesso: Boolean = false,

    val enviando: Boolean = false,
)