package dev.matheus.fluviapp.navigation.destinations

/** A chave da ocorrência (`viagemId@yyyy-MM-dd`) que a emissão recebe do card de saída (F9.5). */
const val ARG_OCORRENCIA = "ocorrencia"

sealed class FluviAppNavComposableDestinations(val route: String) {
    data object MainScreenNavComposable : FluviAppNavComposableDestinations("mainScreen")
    data object FormPassagemNavComposable : FluviAppNavComposableDestinations("formPassagem")
    data object FormPesquisarPassagemNavComposable : FluviAppNavComposableDestinations("formPesquisarPassagem")
    data object ResultPesquisarPassagemNavComposable : FluviAppNavComposableDestinations("resultPesquisarPassagem")
    data object DetalhesPassagemNavComposable : FluviAppNavComposableDestinations("detalhesPassagem")
    data object EmbarqueNavComposable : FluviAppNavComposableDestinations("embarque")

    /**
     * A emissão (F9.5) — **um destino, com a ocorrência obrigatória** no caminho.
     *
     * O argumento é `viagemId@yyyy-MM-dd`, a chave da saída. Ele é **obrigatório**, e não opcional: a rota do
     * formulário antigo aceitava ausência e trafegava o texto literal `"null"`, que chegou a ser gravado. Uma
     * emissão sem saída não é um bilhete a completar — é um bilhete que não tem para onde vender.
     */
    data object EmissaoNavComposable :
        FluviAppNavComposableDestinations("emissao/{$ARG_OCORRENCIA}") {
        fun comOcorrencia(chave: String) = "emissao/$chave"
    }
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