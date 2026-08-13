package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem

import dev.matheus.fluviapp.domain.passagem.CategoriaPassagem
import dev.matheus.fluviapp.domain.passagem.TipoPassagem
import dev.matheus.fluviapp.extensions.formataParaMoedaBrasileira
import dev.matheus.fluviapp.ui.states.passagem.BilheteEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.CabecalhoDaViagem
import dev.matheus.fluviapp.ui.states.passagem.ClienteEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.ConfirmacaoDaEmissao
import dev.matheus.fluviapp.ui.states.passagem.LancamentoConferido
import dev.matheus.fluviapp.ui.states.passagem.PagamentoEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.ParticipanteEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.PessoaConferida
import dev.matheus.fluviapp.ui.states.passagem.VeiculoConferido

/**
 * **O detalhamento, montado do que está em edição** — função pura, e sem I/O nenhum.
 *
 * É a diferença entre este mapper e o da conferência de embarque: lá se traduz um agregado **gravado**, com
 * ids a resolver; aqui se traduz o **formulário**, onde nome, documento e placa já estão à mão. A emissão
 * ainda não aconteceu — e o detalhamento existe justamente para ela poder não acontecer.
 *
 * O que ele formata é o que se confere em voz alta no balcão: quem viaja, o que embarca, quanto entrou. O
 * documento sai **formatado pelo tipo** (`TipoDocumento.formatar`), porque é assim que a pessoa o lê no
 * cartão que tem na mão — comparar `52998224725` com um RG impresso é mais lento e mais errado.
 */
fun confirmacaoDe(
    cabecalho: CabecalhoDaViagem,
    bilhete: BilheteEmEdicao,
    participante: ParticipanteEmEdicao,
    pagamento: PagamentoEmEdicao,
    agencia: String,
): ConfirmacaoDaEmissao = ConfirmacaoDaEmissao(
    cabecalho = cabecalho,
    bilhete = descricaoDo(bilhete, participante),
    pessoas = pessoasDe(participante),
    veiculo = (participante as? ParticipanteEmEdicao.DeVeiculo)?.veiculo?.let { veiculo ->
        VeiculoConferido(
            placa = veiculo.placa,
            classe = veiculo.classe?.rotulo.orEmpty(),
            modelo = veiculo.modelo.takeIf { it.isNotBlank() },
            cor = veiculo.cor.takeIf { it.isNotBlank() },
            cilindrada = veiculo.cilindrada.takeIf { it.isNotBlank() }?.let { "$it cc" },
        )
    },
    lancamentos = pagamento.lancamentos.mapNotNull { linha ->
        linha.valorEmReais()?.let { LancamentoConferido(linha.forma.rotulo, it.formataParaMoedaBrasileira()) }
    },
    total = pagamento.total.formataParaMoedaBrasileira(),
    observacao = pagamento.observacao.takeIf { it.isNotBlank() },
    agencia = agencia,
)

/**
 * O que se está vendendo, em uma linha: acomodação, quantas pessoas e o tipo quando não é inteira.
 *
 * A quantidade entra porque é **o que mais se erra** num balcão movimentado — vender suíte para dois e
 * cadastrar um. Ela aparece aqui ao lado do espaço, que é onde a incoerência salta.
 */
private fun descricaoDo(bilhete: BilheteEmEdicao, participante: ParticipanteEmEdicao): String =
    when (bilhete.categoria) {
        CategoriaPassagem.PASSAGEIRO -> {
            val pessoas = (participante as? ParticipanteEmEdicao.DePassageiro)?.pessoas?.size ?: 1
            listOfNotNull(
                bilhete.acomodacao?.rotulo,
                if (pessoas > 1) "$pessoas pessoas" else null,
                bilhete.tipo.rotulo().takeIf { bilhete.tipo != TipoPassagem.INTEIRA },
                bilhete.gratuidade?.rotulo(),
            ).joinToString(" · ")
        }

        CategoriaPassagem.VEICULO -> listOfNotNull(
            CategoriaPassagem.VEICULO.rotulo,
            (participante as? ParticipanteEmEdicao.DeVeiculo)?.veiculo?.classe?.rotulo,
        ).joinToString(" · ")
    }

/**
 * As pessoas do bilhete, **nomeadas pelo papel**: titular, acompanhante ou responsável pela retirada.
 *
 * O papel não é enfeite — é o que o operador precisa para conferir a pessoa certa quando há três no mesmo
 * bilhete, e é a única forma de a posição 0 (que é o titular, ADR-0023 D3) aparecer como significado em vez
 * de como ordem.
 */
private fun pessoasDe(participante: ParticipanteEmEdicao): List<PessoaConferida> = when (participante) {
    is ParticipanteEmEdicao.DePassageiro -> participante.pessoas
        .filterNot { it.vazio }
        .mapIndexed { indice, pessoa ->
            pessoa.conferida(
                papel = when {
                    participante.pessoas.count { !it.vazio } == 1 -> "Passageiro"
                    indice == 0 -> "Titular"
                    else -> "Acompanhante"
                },
            )
        }

    is ParticipanteEmEdicao.DeVeiculo -> listOfNotNull(
        participante.responsavel?.takeIf { !it.vazio }?.conferida(papel = "Responsável pela retirada"),
    )
}

private fun ClienteEmEdicao.conferida(papel: String) = PessoaConferida(
    papel = papel,
    nome = nome,
    // **Mascarado**, e não formatado por inteiro — é tratamento de dado pessoal, não estética (LGPD).
    //
    // O detalhamento fica aberto no balcão, de frente para a fila, e é a tela que mais tempo passa parada:
    // um documento inteiro ali é o que se decora de relance ou sai numa foto de tela. A máscara é a mesma
    // política que o `TipoDocumento` já aplica no resto do app (ADR-0020 D2) — e ela preserva o que a
    // conferência precisa: os dígitos finais bastam para casar com o cartão na mão de quem está na frente.
    documento = tipoDocumento?.let { "${it.rotulo} ${it.exibir(numeroDocumento, ocultar = true)}" }.orEmpty(),
    nascimento = dataNascimento,
)