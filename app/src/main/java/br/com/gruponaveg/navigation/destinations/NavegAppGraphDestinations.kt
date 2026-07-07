package br.com.gruponaveg.navigation.destinations

sealed class NavegAppGraphDestinations(val route: String) {
    data object SplashScreen : NavegAppGraphDestinations("splashScreen")
    data object LoginGraph : NavegAppGraphDestinations("loginGraph")
    data object MainScreenGraph : NavegAppGraphDestinations("mainScreenGraph")
    data object PesquisarViagemGraph : NavegAppGraphDestinations("pesquisarViagemGraph")
    data object PesquisarPassagemGraph : NavegAppGraphDestinations("pesquisarPassagemGraph")
}