package dev.matheus.fluviapp.ui.states

data class CadastroUiState(
    val nome: String = "",
    val onNomeChange: (String) -> Unit = {},
    val isNomeError: Boolean = false,

    val email: String = "",
    val onEmailChange: (String) -> Unit = {},
    val isEmailError: Boolean = false,

    val senha: String = "",
    val onSenhaChange: (String) -> Unit = {},
    val isSenhaError: Boolean = false,

    val confirmarSenha: String = "",
    val onConfirmarSenhaChange: (String) -> Unit = {},
    val isConfirmarSenhaError: Boolean = false,

    val isSenhaVisible: Boolean = false,

    val exibirErro: Boolean = false,
    val mensagemErro: Int = 0,

    val cadastrando: Boolean = false,
    val cadastrado: Boolean = false,
    /** Preenchido quando o e-mail já existe: sinaliza redirecionar ao login com esse e-mail. */
    val irParaLoginComEmail: String? = null,
)