package dev.matheus.fluviapp.ui.states.passagem

import dev.matheus.fluviapp.domain.passagem.Acomodacao
import dev.matheus.fluviapp.domain.passagem.CategoriaPassagem
import dev.matheus.fluviapp.domain.passagem.ClasseVeiculo
import dev.matheus.fluviapp.domain.passagem.NaturezaVeiculo
import dev.matheus.fluviapp.domain.passagem.TipoPassagem
import dev.matheus.fluviapp.domain.viagem.TipoEmbarcacao

/**
 * **Um passo é uma pergunta** ([ADR-0029] D1) — e quase todas se respondem com um toque, porque o domínio já
 * enumera as respostas: categoria, acomodação, tipo tarifário, subtipo de gratuidade e classe de veículo são
 * `enum`, não texto livre. Onde há campo é onde só se digita: a pessoa e o veículo.
 */
sealed interface PassoDaEmissao {

    /** 1 — dois botões: passageiro ou veículo. A raiz do agregado (ADR-0023 D1). */
    data object Categoria : PassoDaEmissao

    // --- Fluxo do passageiro ---

    /** 2 — rede, suíte ou camarote. */
    data object EscolhaDeAcomodacao : PassoDaEmissao

    /** 3.1 — inteira, meia ou gratuidade. **Só na rede**: fora dela não há escolha a oferecer. */
    data object EscolhaDeTipo : PassoDaEmissao

    /**
     * 3.1.1 — qual gratuidade. Existe **só depois** de a resposta anterior ter sido *gratuidade*, e é assim
     * que "gratuidade sem subtipo" ([ADR-0028] D2) deixa de ser um estado a validar e passa a ser um estado
     * que não se alcança.
     */
    data object EscolhaDeGratuidade : PassoDaEmissao

    /** 3.2 — 1, 2 ou 3 pessoas. **Só na suíte e no camarote**; a rede é uma por bilhete. */
    data object QuantidadeDePessoas : PassoDaEmissao

    // --- Fluxo do veículo ---

    /**
     * 2 — **a natureza**: automotor, moto e similares, máquina ou rebocado ([ADR-0031] D8).
     *
     * Ela entrou quando as classes passaram de seis para dezessete. Uma lista de dezessete botões não é
     * escolha, é catálogo impresso — e três dos nomes compartilham o radical *carret-*, o que num seletor
     * único é erro de seleção que contamina a série de agregação sem deixar rastro.
     */
    data object NaturezaDoVeiculo : PassoDaEmissao

    /**
     * 2.1 — a classe, dentro da natureza escolhida.
     *
     * **É subpasso, e some quando tem uma resposta só**: num navio, cada natureza admitida tem uma única
     * classe a bordo, e perguntar seria contrariar o ADR-0029 no caso mais comum.
     */
    data object ClasseDoVeiculo : PassoDaEmissao

    /** 3 — o formulário do veículo, **rearranjado** conforme a natureza (ADR-0031 D2). */
    data object DadosDoVeiculo : PassoDaEmissao

    // --- Comuns ---

    /**
     * **O cliente** — quarto passo no fluxo de passageiro, e do veículo quando a classe não se pergunta.
     *
     * O [ADR-0029] decidira que ele seria o **quarto nos dois**, para que se aprendesse uma sequência só. O
     * [ADR-0031] D8 quebrou a simetria ao inserir a natureza, e a quebra foi aceita: o roteiro é derivado e
     * mostra *"passo N de M"*, então quem opera **lê o contador em vez de contar passos**. A alternativa —
     * dissolver a natureza quando ela tem classe única — deixaria o cliente ora no quarto ora no quinto, que
     * é pior do que sempre no quinto.
     *
     * Curiosamente a simetria **sobrevive onde o casco é estreito**: num navio a classe não se pergunta, e o
     * cliente volta a ser o quarto nos dois fluxos.
     *
     * @param indice qual pessoa do bilhete — 0 é o titular.
     * @param opcional o responsável pela retirada de um veículo, que **pode ser pulado**: bilhete de veículo
     *   sem ninguém nomeado é a forma normal.
     */
    data class DadosDoCliente(val indice: Int, val opcional: Boolean = false) : PassoDaEmissao

    /**
     * 5 — lançamentos e observação, e o **fim do roteiro**.
     *
     * O que vinha depois era uma tela de desfecho anunciando *"a passagem foi emitida"*. Ela **saiu**: uma
     * tela que só **diz** que deu certo cobra um toque de todo atendimento, enquanto o bilhete **mostra** que
     * deu certo — e ainda se salva ao aparecer ([ADR-0030] D2). Emitir passou a levar direto ao bilhete.
     */
    data object Pagamento : PassoDaEmissao
}

/**
 * **O roteiro é derivado do que se escolheu** ([ADR-0029] D3) — função pura: escolhas dentro, passos fora.
 *
 * Ele não pode ser uma constante porque o caminho **muda com as respostas**: escolher gratuidade insere um
 * passo, escolher suíte troca um passo por outro, escolher três pessoas acrescenta dois formulários. Derivar
 * dá três coisas que uma lista fixa não daria: *"passo 3 de 6"* correto em qualquer fluxo, voltar sem `if`
 * espalhado pela navegação, e o roteiro **verificável em teste** — comparar listas, sem tela nem ViewModel.
 */
fun roteiroDe(
    bilhete: BilheteEmEdicao,
    participante: ParticipanteEmEdicao,
    /**
     * O casco da viagem — ele **encurta o roteiro**, e não só as listas: é ele que decide se a classe ainda
     * é uma pergunta. `null` (embarcação não resolvida) não recorta nada, pelo mesmo princípio do cabeçalho.
     */
    tipoEmbarcacao: TipoEmbarcacao? = null,
): List<PassoDaEmissao> = buildList {
    add(PassoDaEmissao.Categoria)

    when (bilhete.categoria) {
        CategoriaPassagem.PASSAGEIRO -> {
            add(PassoDaEmissao.EscolhaDeAcomodacao)

            when (bilhete.acomodacao) {
                // A rede vende **uma** pessoa: não há quantidade a perguntar, e há tipo tarifário a escolher.
                Acomodacao.REDE -> {
                    add(PassoDaEmissao.EscolhaDeTipo)
                    if (bilhete.tipo == TipoPassagem.GRATUIDADE) add(PassoDaEmissao.EscolhaDeGratuidade)
                }

                // Suíte e camarote são sempre inteira (Acomodacao.tiposPermitidos): o que varia é quantos vão.
                Acomodacao.SUITE, Acomodacao.CAMAROTE -> add(PassoDaEmissao.QuantidadeDePessoas)

                // Ainda não escolhida: o roteiro para aqui, e é o que mantém "passo N de M" honesto — ele não
                // promete passos que dependem de uma resposta que ninguém deu.
                null -> return@buildList
            }

            val pessoas = (participante as? ParticipanteEmEdicao.DePassageiro)?.pessoas?.size ?: 1
            repeat(pessoas) { indice -> add(PassoDaEmissao.DadosDoCliente(indice)) }
        }

        CategoriaPassagem.VEICULO -> {
            add(PassoDaEmissao.NaturezaDoVeiculo)

            val veiculo = (participante as? ParticipanteEmEdicao.DeVeiculo)?.veiculo
            val natureza = veiculo?.natureza ?: return@buildList

            // **O subpasso só existe quando há o que perguntar.** Com uma classe possível, a resposta já foi
            // dada pela natureza — e o ViewModel a grava no mesmo gesto, para que este roteiro e o estado
            // nunca discordem sobre haver ou não classe escolhida.
            if (classesOfertaveis(natureza, tipoEmbarcacao).size > 1) add(PassoDaEmissao.ClasseDoVeiculo)
            if (veiculo.classe == null) return@buildList

            add(PassoDaEmissao.DadosDoVeiculo)
            // O responsável é opcional: bilhete de veículo sem ninguém nomeado é a forma normal.
            add(PassoDaEmissao.DadosDoCliente(indice = 0, opcional = true))
        }
    }

    add(PassoDaEmissao.Pagamento)
}

/**
 * As classes que uma natureza oferece **neste casco** — a interseção que o subpasso apresenta.
 *
 * Mora aqui, e não no domínio, porque o `null` é o caso: embarcação não resolvida não recorta nada, e essa
 * é decisão de apresentação (a fila não para por uma referência que não carregou), não do negócio.
 */
fun classesOfertaveis(natureza: NaturezaVeiculo, tipoEmbarcacao: TipoEmbarcacao?): List<ClasseVeiculo> =
    tipoEmbarcacao?.classesDa(natureza) ?: natureza.classes

/** As naturezas que este casco leva — as vazias não viram botão. */
fun naturezasOfertaveis(tipoEmbarcacao: TipoEmbarcacao?): List<NaturezaVeiculo> =
    tipoEmbarcacao?.naturezasAdmitidas ?: NaturezaVeiculo.entries
