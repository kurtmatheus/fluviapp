package dev.matheus.fluviapp.ui.states

data class LoginUiState(
    val carregandoUsuarios: Boolean = false,

    val email: String = "",
    val onUsuarioChange: (String) -> Unit = {},
    val isUsuarioError: Boolean = false,
    val senha: String = "",
    val onSenhaChange: (String) -> Unit = {},
    val isSenhaError: Boolean = false,
    val isSenhaVisible: Boolean = false,
    val logado: Boolean = false,

    val exibirErro: Boolean = false,
    val mensagemErro: Int = 0,
    val exibirReenviarVerificacao: Boolean = false,

    val logando: Boolean = false
)
