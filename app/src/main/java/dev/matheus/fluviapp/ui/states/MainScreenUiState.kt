package dev.matheus.fluviapp.ui.states

import dev.matheus.fluviapp.domain.screendata.SecaoMenu

data class MainScreenUiState(
    val userName: String = "",
    /** Seções liberadas ao cargo do usuário (política PermissoesUsuario). */
    val secoesVisiveis: List<SecaoMenu> = emptyList(),

    /**
     * **O Início, decidido pelo domínio** (F8.4). O `listaViagens` que existia aqui antes da
     * revitalização era uma lista só, igual para todo mundo; agora o tipo carrega *de quem é o painel* —
     * a plataforma não vê saídas porque não vende, e "sem concessão" é estado próprio, não lista vazia.
     */
    val inicio: InicioDaTela = InicioDaTela.Carregando,

    val isRefreshing: Boolean = false,

    /** Sync falhou (offline): banner não-bloqueante sobre os dados do cache (D4). */
    val sincronizacaoComErro: Boolean = false,

    val mainScreenState: MainScreenState = MainScreenState.HOME,
)

/**
 * O que a tela inicial desenha — a tradução do `InicioDoPainel` do domínio, com os cards já formatados.
 *
 * O estado a mais em relação ao domínio é [Carregando], e ele é de apresentação: o domínio responde sobre
 * um escopo que já chegou, a tela precisa dizer algo enquanto ele não chegou. Sem ele, o painel da empresa
 * piscaria "não há saídas" antes da primeira leitura — que é o recado errado dado no pior momento.
 */
sealed interface InicioDaTela {
    data object Carregando : InicioDaTela

    /** A plataforma monta o universo; o sumário dela é a F10. */
    data object DaPlataforma : InicioDaTela

    /** "Viagens Disponíveis" — a lista pode estar vazia, e vazia aqui quer dizer *não há saída*. */
    data class DaEmpresa(val disponiveis: List<ViagemDisponivelCard>) : InicioDaTela

    /** Falta provisionar — recado oposto ao da lista vazia, e por isso estado separado. */
    data object SemConcessao : InicioDaTela
}
