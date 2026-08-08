package dev.matheus.fluviapp.ui.states

import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.operacoes.Atuacao
import dev.matheus.fluviapp.domain.viagem.Embarcacao

/**
 * Estado do formulário de empresa — puro (só dados + flags). Sem lambdas embutidas: os eventos são
 * métodos no FormEmpresaViewModel (molde cadastro-modulos §7.2). `cnpj` guarda só dígitos; a máscara
 * é aplicada na exibição (CnpjVisualTransformation).
 */
data class FormEmpresaUiState(
    val titulo: Int = R.string.subtitle_cadastrar_nova_empresa,
    val nome: String = "",
    val isNomeError: Boolean = false,
    val razaoSocial: String = "",
    val isRazaoSocialError: Boolean = false,
    val cnpj: String = "",
    val isCnpjError: Boolean = false,
    val endereco: String = "",
    val telefone1: String = "",
    val telefone2: String = "",
    /**
     * O que esta parte FAZ (ADR-0016 §4). Conjunto porque uma empresa exerce várias atuações ao mesmo
     * tempo — não é escolha única, e não é subtipo.
     */
    val atuacoes: Set<Atuacao> = emptySet(),
    val isAtuacoesError: Boolean = false,

    /**
     * Toda a frota da plataforma — os **candidatos** à concessão. Concede-se a embarcação, e não o
     * armador (ADR-0016 §7, 7ª rodada), então quem lista aqui não é a frota da própria parte: é a de
     * todo mundo, porque agenciar é vender o que é dos outros.
     */
    val embarcacoes: List<Embarcacao> = emptyList(),

    /** Ids concedidos à atuação de agenciamento desta parte (`atuacoes/AGENCIAMENTO.embarcacaoIds`). */
    val embarcacoesConcedidas: Set<String> = emptySet(),

    /**
     * Todos os portos ativos — os candidatos da **outra metade** da concessão (F7). Mesma lógica das
     * embarcações: a lista é a da plataforma, porque operar num porto não é ser dono dele.
     */
    val portos: List<PortoOpcao> = emptyList(),

    /** Ids concedidos (`atuacoes/AGENCIAMENTO.portoIds`) — **onde** esta parte pode operar. */
    val portosConcedidos: Set<String> = emptySet(),

    val isProcessing: Boolean = false,
) {

    /**
     * Se o formulário deve **perguntar** a concessão. Concessão é da atuação de agenciamento — pedi-la a
     * quem não agencia seria perguntar quem esta parte representa quando ela não representa ninguém.
     *
     * Mesmo gesto do tipo da embarcação escondendo a capacidade de veículo: a pergunta que não se aplica
     * não fica cinza, **não existe**.
     */
    val concedeEmbarcacoes: Boolean get() = Atuacao.AGENCIAMENTO in atuacoes

    /**
     * A concessão tem **duas dimensões** desde a F7 (§7.1): **em quê** (embarcações) e **onde** (portos).
     * As duas dependem da mesma atuação, e é por isso que a pergunta é a mesma — o que muda é a resposta.
     */
    val concedePortos: Boolean get() = concedeEmbarcacoes
}
