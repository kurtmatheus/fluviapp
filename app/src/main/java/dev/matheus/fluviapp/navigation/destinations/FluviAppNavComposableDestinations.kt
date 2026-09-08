package dev.matheus.fluviapp.navigation.destinations

import dev.matheus.fluviapp.domain.screendata.SecaoMenu

/** A chave da ocorrência (`viagemId@yyyy-MM-dd`) que a emissão recebe do card de saída (F9.5). */
const val ARG_OCORRENCIA = "ocorrencia"

/** O id da passagem — o que o QR carrega, e o que o bilhete recebe para se desenhar. */
const val ARG_ID_PASSAGEM = "idPassagem"

/**
 * Os destinos de tela, e **a seção a que cada um pertence** ([ADR-0032] D1).
 *
 * A seção não é decoração: é o que a guarda de navegação consulta para reautorizar o destino. Ela mora
 * **aqui**, no construtor e **sem valor padrão**, por um motivo de forma — assim uma tela nova **não
 * compila** sem responder de quem ela é. A alternativa (um mapa em outro arquivo, ou um `when` sobre
 * rotas) esquece em silêncio, e o esquecimento se chama tela desprotegida.
 *
 * `secao = null` é resposta legítima, e quer dizer **fora do menu**: o bilhete, a emissão e o embarque
 * não são seções — são gestos alcançados de dentro da Passagem, e quem os governa é a política do gesto
 * (o guarda dentro do ViewModel), não a do menu. Dizer `null` é responder; o que não se admite é não
 * responder.
 */
sealed class FluviAppNavComposableDestinations(
    val route: String,
    val secao: SecaoMenu?,
) {
    /** O painel: quem chega aqui já passou pela splash, e o que ele mostra é a política que decide. */
    data object MainScreenNavComposable : FluviAppNavComposableDestinations("mainScreen", secao = null)

    data object FormPassagemNavComposable :
        FluviAppNavComposableDestinations("formPassagem", secao = null)

    data object FormPesquisarPassagemNavComposable :
        FluviAppNavComposableDestinations("formPesquisarPassagem", secao = null)

    data object ResultPesquisarPassagemNavComposable :
        FluviAppNavComposableDestinations("resultPesquisarPassagem", secao = null)

    data object DetalhesPassagemNavComposable :
        FluviAppNavComposableDestinations("detalhesPassagem", secao = null)

    /**
     * O embarque é **gesto, não seção** (ADR-0022): quem o governa é `temEntradaDeEmbarque` no painel e
     * `podeConfirmarEmbarque` no ViewModel — a guarda de navegação não tem o que dizer aqui.
     */
    data object EmbarqueNavComposable : FluviAppNavComposableDestinations("embarque", secao = null)

    /**
     * A emissão (F9.5) — **um destino, com a ocorrência obrigatória** no caminho.
     *
     * O argumento é `viagemId@yyyy-MM-dd`, a chave da saída. Ele é **obrigatório**, e não opcional: a rota do
     * formulário antigo aceitava ausência e trafegava o texto literal `"null"`, que chegou a ser gravado. Uma
     * emissão sem saída não é um bilhete a completar — é um bilhete que não tem para onde vender.
     */
    data object EmissaoNavComposable :
        FluviAppNavComposableDestinations("emissao/{$ARG_OCORRENCIA}", secao = null) {
        fun comOcorrencia(chave: String) = "emissao/$chave"
    }

    /**
     * O **bilhete digital** — destino próprio, e não um pedaço da emissão.
     *
     * *"Mesmo bilhete"* (analista): quem acabou de emitir e quem for buscar uma passagem antiga chegam ao
     * mesmo lugar. O documento é um só, e duas telas desenhando o mesmo documento é como elas divergem.
     */
    data object BilheteNavComposable :
        FluviAppNavComposableDestinations("bilhete/{$ARG_ID_PASSAGEM}", secao = null) {
        fun comPassagem(idPassagem: String) = "bilhete/$idPassagem"
    }

    /**
     * A **busca de bilhetes** (F9.6). Sem argumento: o recorte vem do vínculo em vigor, não da rota — a
     * agência é decisão da política, e não algo que se escolha pelo caminho.
     */
    data object PesquisaPassagemNavComposable :
        FluviAppNavComposableDestinations("pesquisaPassagem", SecaoMenu.PASSAGEM)

    data object ContagemPassagemNavComposable :
        FluviAppNavComposableDestinations("contagemPassagem", secao = null)

    data object FormFuncionarioNavComposable :
        FluviAppNavComposableDestinations("formFuncionario", SecaoMenu.EQUIPE)

    data object ResultPesquisarFuncionarioNavComposable :
        FluviAppNavComposableDestinations("pesquisarFuncionario", SecaoMenu.EQUIPE)

    data object FormEmpresaNavComposable :
        FluviAppNavComposableDestinations("formEmpresa", SecaoMenu.EMPRESA)

    data object ResultPesquisarEmpresaNavComposable :
        FluviAppNavComposableDestinations("pesquisarEmpresa", SecaoMenu.EMPRESA)

    data object FormEmbarcacaoNavComposable :
        FluviAppNavComposableDestinations("formEmbarcacao", SecaoMenu.EMBARCACAO)

    data object ResultPesquisarEmbarcacaoNavComposable :
        FluviAppNavComposableDestinations("pesquisarEmbarcacao", SecaoMenu.EMBARCACAO)

    data object FormLocalidadeNavComposable :
        FluviAppNavComposableDestinations("formLocalidade", SecaoMenu.LOCALIDADE)

    data object ResultPesquisarLocalidadeNavComposable :
        FluviAppNavComposableDestinations("pesquisarLocalidade", SecaoMenu.LOCALIDADE)

    data object FormPortoNavComposable :
        FluviAppNavComposableDestinations("formPorto", SecaoMenu.PORTO)

    data object ResultPesquisarPortoNavComposable :
        FluviAppNavComposableDestinations("pesquisarPorto", SecaoMenu.PORTO)

    data object FormUsuarioNavComposable :
        FluviAppNavComposableDestinations("formUsuario", SecaoMenu.USUARIOS)

    data object ResultPesquisarUsuarioNavComposable :
        FluviAppNavComposableDestinations("pesquisarUsuario", SecaoMenu.USUARIOS)

    data object FormRotaNavComposable :
        FluviAppNavComposableDestinations("formRota", SecaoMenu.ROTA)

    data object ResultPesquisarRotaNavComposable :
        FluviAppNavComposableDestinations("pesquisarRota", SecaoMenu.ROTA)

    // Sem `?idViagem=` — o destino antigo o tinha para editar, e editar não existe (§7.1).
    data object FormViagemNavComposable :
        FluviAppNavComposableDestinations("formViagem", SecaoMenu.VIAGEM)

    data object ResultPesquisarViagemNavComposable :
        FluviAppNavComposableDestinations("pesquisarViagem", SecaoMenu.VIAGEM)
}

/**
 * **Todos os destinos de tela** — e o único lugar a atualizar quando nascer um.
 *
 * Ela existe porque Kotlin não enumera uma classe selada sem reflexão, e reflexão no Android custa
 * `kotlin-reflect` e vira o tipo de aresta que o R8 não enxerga — exatamente o que a fatia do R8 acabou
 * de tirar deste app.
 *
 * O que **garante** que um destino novo seja pensado não é esta lista: é o construtor, onde `secao` não
 * tem valor padrão. Esta lista responde outra pergunta — *quais existem* — e o teste
 * `SecaoDaRotaTest` a confere contra as seções declaradas.
 */
val TODOS_OS_DESTINOS: List<FluviAppNavComposableDestinations> = listOf(
    FluviAppNavComposableDestinations.MainScreenNavComposable,
    FluviAppNavComposableDestinations.FormPassagemNavComposable,
    FluviAppNavComposableDestinations.FormPesquisarPassagemNavComposable,
    FluviAppNavComposableDestinations.ResultPesquisarPassagemNavComposable,
    FluviAppNavComposableDestinations.DetalhesPassagemNavComposable,
    FluviAppNavComposableDestinations.EmbarqueNavComposable,
    FluviAppNavComposableDestinations.EmissaoNavComposable,
    FluviAppNavComposableDestinations.BilheteNavComposable,
    FluviAppNavComposableDestinations.PesquisaPassagemNavComposable,
    FluviAppNavComposableDestinations.ContagemPassagemNavComposable,
    FluviAppNavComposableDestinations.FormFuncionarioNavComposable,
    FluviAppNavComposableDestinations.ResultPesquisarFuncionarioNavComposable,
    FluviAppNavComposableDestinations.FormEmpresaNavComposable,
    FluviAppNavComposableDestinations.ResultPesquisarEmpresaNavComposable,
    FluviAppNavComposableDestinations.FormEmbarcacaoNavComposable,
    FluviAppNavComposableDestinations.ResultPesquisarEmbarcacaoNavComposable,
    FluviAppNavComposableDestinations.FormLocalidadeNavComposable,
    FluviAppNavComposableDestinations.ResultPesquisarLocalidadeNavComposable,
    FluviAppNavComposableDestinations.FormPortoNavComposable,
    FluviAppNavComposableDestinations.ResultPesquisarPortoNavComposable,
    FluviAppNavComposableDestinations.FormUsuarioNavComposable,
    FluviAppNavComposableDestinations.ResultPesquisarUsuarioNavComposable,
    FluviAppNavComposableDestinations.FormRotaNavComposable,
    FluviAppNavComposableDestinations.ResultPesquisarRotaNavComposable,
    FluviAppNavComposableDestinations.FormViagemNavComposable,
    FluviAppNavComposableDestinations.ResultPesquisarViagemNavComposable,
)

/**
 * A seção de uma rota, ou `null` quando o destino não pertence ao menu.
 *
 * Compara pela rota **declarada** (com o `{argumento}` no lugar), que é o que o `NavDestination` devolve
 * — e não pela navegada, que já vem com o valor substituído.
 */
fun secaoDaRota(rota: String?): SecaoMenu? =
    TODOS_OS_DESTINOS.firstOrNull { it.route == rota }?.secao
