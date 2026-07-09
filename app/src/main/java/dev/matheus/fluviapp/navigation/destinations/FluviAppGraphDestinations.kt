package dev.matheus.fluviapp.navigation.destinations

const val ARG_EMAIL_PREFILL = "email_prefill"

sealed class FluviAppGraphDestinations(val route: String) {
    data object SplashScreen : FluviAppGraphDestinations("splashScreen")
    data object LoginGraph : FluviAppGraphDestinations("loginGraph")
    data object Cadastro : FluviAppGraphDestinations("cadastro")
    data object MainScreenGraph : FluviAppGraphDestinations("mainScreenGraph")
    data object PesquisarViagemGraph : FluviAppGraphDestinations("pesquisarViagemGraph")
    data object PesquisarPassagemGraph : FluviAppGraphDestinations("pesquisarPassagemGraph")
}