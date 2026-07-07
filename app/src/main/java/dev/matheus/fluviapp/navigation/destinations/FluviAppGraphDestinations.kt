package dev.matheus.fluviapp.navigation.destinations

sealed class FluviAppGraphDestinations(val route: String) {
    data object SplashScreen : FluviAppGraphDestinations("splashScreen")
    data object LoginGraph : FluviAppGraphDestinations("loginGraph")
    data object MainScreenGraph : FluviAppGraphDestinations("mainScreenGraph")
    data object PesquisarViagemGraph : FluviAppGraphDestinations("pesquisarViagemGraph")
    data object PesquisarPassagemGraph : FluviAppGraphDestinations("pesquisarPassagemGraph")
}