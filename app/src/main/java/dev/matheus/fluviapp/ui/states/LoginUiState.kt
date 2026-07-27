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

    /**
     * Sinal one-shot do **primeiro acesso** (ADR-0015 §2.1): não-nulo = autenticou, não tem perfil e
     * existe funcionário com este e-mail. Carrega o **e-mail** porque é ele que casa as duas frentes do
     * pré-cadastro; a tela de criar senha resolve o funcionário a partir dele.
     */
    val primeiroAcessoEmail: String? = null,

    val logando: Boolean = false
)
