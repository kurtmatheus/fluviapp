package dev.matheus.fluviapp.navigation.destinations

sealed class FluviAppNavComposableDestinations(val route: String) {
    data object MainScreenNavComposable : FluviAppNavComposableDestinations("mainScreen")
    data object FormViagemNavComposable : FluviAppNavComposableDestinations("formViagem")
    data object FormPesquisarViagemNavComposable : FluviAppNavComposableDestinations("formPesquisarViagem")
    data object ResultPesquisarViagemNavComposable : FluviAppNavComposableDestinations("resultPesquisarViagem")
    data object DetalhesViagemNavComposable : FluviAppNavComposableDestinations("detalhesViagem")
    data object FormPassagemNavComposable : FluviAppNavComposableDestinations("formPassagem")
    data object FormPesquisarPassagemNavComposable : FluviAppNavComposableDestinations("formPesquisarPassagem")
    data object ResultPesquisarPassagemNavComposable : FluviAppNavComposableDestinations("resultPesquisarPassagem")
    data object DetalhesPassagemNavComposable : FluviAppNavComposableDestinations("detalhesPassagem")
    data object BalancoNavComposable : FluviAppNavComposableDestinations("listaRelatorios")
    data object FormAgenteNavComposable: FluviAppNavComposableDestinations("formAgente")
    data object ResultPesquisarAgenteNavComposable: FluviAppNavComposableDestinations("pesquisarAgente")
}