package dev.matheus.fluviapp.navigation.destinations

/** A chave da ocorrência (`viagemId@yyyy-MM-dd`) que a emissão recebe do card de saída (F9.5). */
const val ARG_OCORRENCIA = "ocorrencia"

/** O id da passagem — o que o QR carrega, e o que o bilhete recebe para se desenhar. */
const val ARG_ID_PASSAGEM = "idPassagem"

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

    /**
     * O **bilhete digital** — destino próprio, e não um pedaço da emissão.
     *
     * *"Mesmo bilhete"* (analista): quem acabou de emitir e quem for buscar uma passagem antiga chegam ao
     * mesmo lugar. O documento é um só, e duas telas desenhando o mesmo documento é como elas divergem.
     */
    data object BilheteNavComposable :
        FluviAppNavComposableDestinations("bilhete/{$ARG_ID_PASSAGEM}") {
        fun comPassagem(idPassagem: String) = "bilhete/$idPassagem"
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