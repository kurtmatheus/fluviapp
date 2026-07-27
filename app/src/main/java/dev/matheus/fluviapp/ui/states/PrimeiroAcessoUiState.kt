package dev.matheus.fluviapp.ui.states

/**
 * Estado da tela de primeiro acesso (ADR-0015 §2.1) — puro, sem lambdas: os eventos são métodos do
 * `PrimeiroAcessoViewModel` (molde cadastro-modulos §7.2).
 *
 * [concluido] é o fim do fluxo: senha trocada **e** perfil criado. A pessoa volta ao login e entra de
 * novo com a senha nova — é o passo de confirmação que o §2.1 pede, e de quebra garante que a sessão
 * seguinte já nasce lendo o perfil que acabou de existir.
 */
data class PrimeiroAcessoUiState(
    val nome: String = "",
    val senha: String = "",
    val isSenhaError: Boolean = false,
    val confirmacao: String = "",
    val isConfirmacaoError: Boolean = false,
    val isSenhaVisible: Boolean = false,
    val mensagemErro: Int = 0,
    val processando: Boolean = false,
    val concluido: Boolean = false,
)