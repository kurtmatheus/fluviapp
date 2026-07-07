package br.com.gruponaveg.navigation.destinations

sealed class NavegAppNavComposableDestinations(val route: String) {
    data object MainScreenNavComposable : NavegAppNavComposableDestinations("mainScreen")
    data object FormViagemNavComposable : NavegAppNavComposableDestinations("formViagem")
    data object FormPesquisarViagemNavComposable : NavegAppNavComposableDestinations("formPesquisarViagem")
    data object ResultPesquisarViagemNavComposable : NavegAppNavComposableDestinations("resultPesquisarViagem")
    data object DetalhesViagemNavComposable : NavegAppNavComposableDestinations("detalhesViagem")
    data object FormPassagemNavComposable : NavegAppNavComposableDestinations("formPassagem")
    data object FormPesquisarPassagemNavComposable : NavegAppNavComposableDestinations("formPesquisarPassagem")
    data object ResultPesquisarPassagemNavComposable : NavegAppNavComposableDestinations("resultPesquisarPassagem")
    data object DetalhesPassagemNavComposable : NavegAppNavComposableDestinations("detalhesPassagem")
    data object BalancoNavComposable : NavegAppNavComposableDestinations("listaRelatorios")
    data object FormAgenteNavComposable: NavegAppNavComposableDestinations("formAgente")
    data object ResultPesquisarAgenteNavComposable: NavegAppNavComposableDestinations("pesquisarAgente")
}