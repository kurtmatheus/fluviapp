package dev.matheus.fluviapp.ui.states

import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.viagem.Empresa
import dev.matheus.fluviapp.domain.viagem.TipoEmbarcacao

/**
 * Estado do formulário de embarcação — puro (só dados + flags). Sem lambdas embutidas: os eventos são
 * métodos no FormEmbarcacaoViewModel (molde cadastro-modulos §7.2). As capacidades guardam só dígitos
 * (teclado numérico + filtro), então são sempre inteiros válidos (em branco = 0). `empresa` é o nome
 * selecionado no dropdown (estado de UI); o vínculo persistido é por id (o VM resolve — ADR-0008).
 *
 * [tipo] é **nulo aqui e não na entidade**, e a diferença é o ponto: no domínio não existe embarcação sem
 * tipo (`Embarcacao.tipo`), mas existe formulário ainda não preenchido. O nulo do estado significa *ainda
 * não escolhido*; é a validação que impede que ele chegue ao domínio.
 */
data class FormEmbarcacaoUiState(
    val titulo: Int = R.string.subtitle_cadastrar_nova_embarcacao,

    val nome: String = "",
    val isNomeError: Boolean = false,

    val empresa: String = "",
    val isEmpresaError: Boolean = false,

    val tipo: TipoEmbarcacao? = null,
    val isTipoError: Boolean = false,

    val capacidadeVeiculo: String = "",
    val capacidadeSuite2: String = "",
    val capacidadeSuite3: String = "",
    val capacidadeCamarote: String = "",

    val listaEmpresas: List<Empresa> = emptyList(),

    val isProcessing: Boolean = false,
) {

    /**
     * Se o formulário deve **perguntar** a capacidade de veículos. Escolhida a lancha, a pergunta some — e
     * com ela a chance de nascer uma "lancha com doze vagas de carro", que nenhuma validação posterior
     * conseguiria desmentir sem antes ter deixado alguém digitá-la.
     */
    val perguntaCapacidadeVeiculo: Boolean get() = tipo?.levaVeiculo == true
}