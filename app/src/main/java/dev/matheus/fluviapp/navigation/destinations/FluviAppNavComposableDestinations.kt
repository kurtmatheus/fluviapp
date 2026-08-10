package dev.matheus.fluviapp.navigation.destinations

sealed class FluviAppNavComposableDestinations(val route: String) {
    data object MainScreenNavComposable : FluviAppNavComposableDestinations("mainScreen")
    data object FormPassagemNavComposable : FluviAppNavComposableDestinations("formPassagem")
    data object FormPesquisarPassagemNavComposable : FluviAppNavComposableDestinations("formPesquisarPassagem")
    data object ResultPesquisarPassagemNavComposable : FluviAppNavComposableDestinations("resultPesquisarPassagem")
    data object DetalhesPassagemNavComposable : FluviAppNavComposableDestinations("detalhesPassagem")
    data object EmbarqueNavComposable : FluviAppNavComposableDestinations("embarque")
    data object ContagemPassagemNavComposable : FluviAppNavComposableDestinations("contagemPassagem")
    data object FormFuncionarioNavComposable: FluviAppNavComposableDestinations("formFuncionario")
    data object ResultPesquisarFuncionarioNavComposable: FluviAppNavComposableDestinations("pesquisarFuncionario")
    data object FormEmpresaNavComposable : FluviAppNavComposableDestinations("formEmpresa")
    data object ResultPesquisarEmpresaNavComposable : FluviAppNavComposableDestinations("pesquisarEmpresa")
    data object FormEmbarcacaoNavComposable : FluviAppNavComposableDestinations("formEmbarcacao")
    data object ResultPesquisarEmbarcacaoNavComposable : FluviAppNavComposableDestinations("pesquisarEmbarcacao")
    data object FormLocalidadeNavComposable : FluviAppNavComposableDestinations("formLocalidade")
    data object ResultPesquisarLocalidadeNavComposable : FluviAppNavComposableDestinations("pesquisarLocalidade")
    data object FormPortoNavComposable : FluviAppNavComposableDestinations("formPorto")
    data object ResultPesquisarPortoNavComposable : FluviAppNavComposableDestinations("pesquisarPorto")
    data object FormUsuarioNavComposable : FluviAppNavComposableDestinations("formUsuario")
    data object ResultPesquisarUsuarioNavComposable : FluviAppNavComposableDestinations("pesquisarUsuario")
    data object FormRotaNavComposable : FluviAppNavComposableDestinations("formRota")
    data object ResultPesquisarRotaNavComposable : FluviAppNavComposableDestinations("pesquisarRota")

    // Sem `?idViagem=` — o destino antigo o tinha para editar, e editar não existe (§7.1).
    data object FormViagemNavComposable : FluviAppNavComposableDestinations("formViagem")
    data object ResultPesquisarViagemNavComposable :
        FluviAppNavComposableDestinations("pesquisarViagem")
}