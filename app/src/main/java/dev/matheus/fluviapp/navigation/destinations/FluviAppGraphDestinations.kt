package dev.matheus.fluviapp.navigation.destinations

const val ARG_EMAIL_PREFILL = "email_prefill"

/** E-mail de quem está no primeiro acesso (ADR-0015 §2.1) — a tela resolve o funcionário por ele. */
const val ARG_EMAIL_PRIMEIRO_ACESSO = "email_primeiro_acesso"

sealed class FluviAppGraphDestinations(val route: String) {
    data object SplashScreen : FluviAppGraphDestinations("splashScreen")
    data object LoginGraph : FluviAppGraphDestinations("loginGraph")

    // `Cadastro` saiu em P2.2c: não há autocadastro (ADR-0015 §2.1). No lugar dele, a tela que a
    // gestão pré-cadastrou leva a pessoa a criar a própria senha.
    data object PrimeiroAcesso : FluviAppGraphDestinations("primeiroAcesso")
    data object RecuperarSenha : FluviAppGraphDestinations("recuperarSenha")
    data object MainScreenGraph : FluviAppGraphDestinations("mainScreenGraph")
    data object PesquisarViagemGraph : FluviAppGraphDestinations("pesquisarViagemGraph")
    data object PesquisarPassagemGraph : FluviAppGraphDestinations("pesquisarPassagemGraph")
}