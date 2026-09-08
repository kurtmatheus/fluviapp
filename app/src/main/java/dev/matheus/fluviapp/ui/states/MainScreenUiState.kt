package dev.matheus.fluviapp.ui.states

import dev.matheus.fluviapp.domain.operacoes.MetricasDeAcesso
import dev.matheus.fluviapp.domain.operacoes.Perfil
import dev.matheus.fluviapp.domain.screendata.SecaoMenu

data class MainScreenUiState(
    val userName: String = "",
    /** Seções liberadas ao cargo do usuário (política PermissoesUsuario). */
    val secoesVisiveis: List<SecaoMenu> = emptyList(),

    /**
     * Se a barra inferior com o embarque existe neste painel — `PermissoesUsuario.temEntradaDeEmbarque`.
     *
     * Entra como **booleano já decidido**, e não como papel/cargo/atuação para a tela recompor: a
     * pergunta é de política, e política respondida na tela é política que se repete diferente na
     * próxima tela.
     */
    val podeEmbarcar: Boolean = false,

    /**
     * Se a opção de **trocar de perfil** aparece no menu ([ADR-0032] D5) — quem tem papel de plataforma e
     * vínculo com uma empresa. Entra decidido, pelo mesmo motivo de [podeEmbarcar].
     */
    val podeTrocarPerfil: Boolean = false,

    /**
     * Qual perfil está ativo, para o menu dizer **para onde a troca leva** — e não onde se está: "operar
     * como <empresa>" sob o perfil de plataforma, "administrar a plataforma" sob o de empresa. Um rótulo
     * que anunciasse o estado atual faria a opção parecer um indicador, e ela é um gesto.
     */
    val perfilAtivo: Perfil = Perfil.PLATAFORMA,

    /** O nome da empresa do vínculo — o que entra no rótulo da troca. Vazio quando não há vínculo. */
    val empresaDoVinculo: String = "",

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

    /**
     * O painel da plataforma — e desde a [ADR-0032] D6 ele **mostra o que a plataforma administra**: o
     * estado do acesso de quem entra no app (decisão do analista em 2026-09-08).
     *
     * Era `data object`, e o recado *"a plataforma monta o universo"* era um lugar vazio esperando conteúdo
     * desde a F8.4 — o ADR-0022 D5 dizia que o sumário dela vinha na F10, porque *sumário vem depois do que
     * resume*. As métricas de acesso não são o sumário da operação: são o assunto **próprio** da plataforma,
     * e por isso cabem antes.
     *
     * [acesso] nulo é *"quem olha não é `ADM`"* — o `GESTOR` administra o negócio da plataforma, não o
     * acesso a ela (ADR-0021 D1), e vê o recado de antes. Nulo também é o instante inicial de quem é
     * `ADM`: o fluxo espera o primeiro snapshot das duas coleções antes de emitir número, porque zero se lê
     * como fato e não como *ainda não sei*.
     */
    data class DaPlataforma(val acesso: MetricasDeAcesso? = null) : InicioDaTela

    /** "Viagens Disponíveis" — a lista pode estar vazia, e vazia aqui quer dizer *não há saída*. */
    data class DaEmpresa(val disponiveis: List<ViagemDisponivelCard>) : InicioDaTela

    /** Falta provisionar — recado oposto ao da lista vazia, e por isso estado separado. */
    data object SemConcessao : InicioDaTela
}
