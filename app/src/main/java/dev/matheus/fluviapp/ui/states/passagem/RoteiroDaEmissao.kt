package dev.matheus.fluviapp.ui.states.passagem

import dev.matheus.fluviapp.domain.passagem.Acomodacao
import dev.matheus.fluviapp.domain.passagem.CategoriaPassagem
import dev.matheus.fluviapp.domain.passagem.TipoPassagem

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

    /** 2 — a classe, em lista vertical de botões. É ela que decide o que o passo seguinte pergunta. */
    data object ClasseDoVeiculo : PassoDaEmissao

    /** 3 — o formulário do veículo, **rearranjado** conforme a classe (ADR-0023 D4). */
    data object DadosDoVeiculo : PassoDaEmissao

    // --- Comuns ---

    /**
     * 4 — **o cliente**, e é o mesmo número nos dois fluxos por decisão de desenho: quem opera aprende uma
     * sequência só, e a diferença entre passageiro e veículo fica onde ela é real (2 e 3).
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
fun roteiroDe(bilhete: BilheteEmEdicao, participante: ParticipanteEmEdicao): List<PassoDaEmissao> = buildList {
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
            add(PassoDaEmissao.ClasseDoVeiculo)
            val classeEscolhida = (participante as? ParticipanteEmEdicao.DeVeiculo)?.veiculo?.classe
            if (classeEscolhida == null) return@buildList

            add(PassoDaEmissao.DadosDoVeiculo)
            // O responsável é o **passo 4 do outro fluxo**, aqui opcional: mesma posição, mesmo gesto.
            add(PassoDaEmissao.DadosDoCliente(indice = 0, opcional = true))
        }
    }

    add(PassoDaEmissao.Pagamento)
}