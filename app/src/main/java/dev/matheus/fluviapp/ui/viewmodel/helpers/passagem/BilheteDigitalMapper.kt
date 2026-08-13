package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem

import dev.matheus.fluviapp.domain.cliente.Cliente
import dev.matheus.fluviapp.domain.passagem.CategoriaPassagem
import dev.matheus.fluviapp.domain.passagem.Passagem
import dev.matheus.fluviapp.domain.passagem.PassagemDePassageiro
import dev.matheus.fluviapp.domain.passagem.PassagemDeVeiculo
import dev.matheus.fluviapp.domain.passagem.TipoPassagem
import dev.matheus.fluviapp.domain.passagem.total
import dev.matheus.fluviapp.extensions.formataParaMoedaBrasileira
import dev.matheus.fluviapp.ui.states.passagem.BilheteDigital
import dev.matheus.fluviapp.ui.states.passagem.PassageiroDoBilhete
import dev.matheus.fluviapp.ui.states.passagem.VeiculoConferido
import dev.matheus.fluviapp.ui.viewmodel.helpers.inicio.rotuloCom

/**
 * `Passagem` + referências → [BilheteDigital]. **Função pura**, como as demais junções ([ADR-0025] D3).
 *
 * Este é o **consumidor que faltava** para `ColetorDeReferencias.completas`: escrito na F9.4 com os dois
 * regimes de junção — lookup em memória para as coleções pequenas, leitura por ids em lote para os pools —,
 * ele nunca tinha sido chamado. O bilhete é ele, e por uma razão de domínio: **nada é congelado no agregado**
 * (ADR-0023 D8), então o nome de quem viaja **só existe resolvendo o pool**.
 *
 * ### O que este mapper mostra e o outro esconde
 *
 * O documento sai **por inteiro**, ao contrário da conferência (que mascara). Não é inconsistência: a máscara
 * protege de **terceiros** — a fila diante do balcão —, e aqui não há terceiro. O comprovante vai para quem
 * já sabe o próprio número, e cortado ele deixaria de servir para ser conferido contra a identidade.
 *
 * ### Uma pessoa que o pool não devolveu
 *
 * Vira uma linha **sem nome**, e não uma linha ausente: um bilhete de suíte para três com dois nomes seria
 * lido como bilhete de dois. A ausência aparece; ela não se disfarça de outra coisa.
 */
fun Passagem.paraBilhete(referencias: ReferenciasDaPassagem, agencia: String): BilheteDigital =
    BilheteDigital(
        idPassagem = id,
        numero = "#$numero",
        agencia = agencia,
        travessia = referencias.rota?.rotuloCom(referencias.portosPorId).orEmpty(),
        partida = partidaDoBilhete(referencias),
        embarcacao = referencias.embarcacao.orEmpty(),
        bilhete = descricaoDoBilheteCompleta(),
        passageiros = passageirosDo(referencias),
        veiculo = (this as? PassagemDeVeiculo)?.let { passagem ->
            referencias.veiculosPorId[passagem.veiculoId]?.let { veiculo ->
                VeiculoConferido(
                    placa = veiculo.placa,
                    classe = veiculo.tipo.rotulo,
                    modelo = veiculo.modelo,
                    cor = veiculo.cor.takeIf { it.isNotBlank() },
                    cilindrada = veiculo.cilindrada?.let { "$it cc" },
                )
            }
        },
        total = lancamentos.total.formataParaMoedaBrasileira(),
        observacao = observacao,
        gratuidade = (this as? PassagemDePassageiro)?.gratuidade?.rotulo(),
    )

/** No bilhete a acomodação vem com o tipo tarifário sempre que ele **não** é inteira — é o que se fiscaliza. */
private fun Passagem.descricaoDoBilheteCompleta(): String = when (this) {
    is PassagemDePassageiro -> listOfNotNull(
        acomodacao.rotulo,
        clientes.size.takeIf { it > 1 }?.let { "$it pessoas" },
        tipo.rotulo().takeIf { tipo != TipoPassagem.INTEIRA },
    ).joinToString(" · ")

    is PassagemDeVeiculo -> CategoriaPassagem.VEICULO.rotulo
}

private fun Passagem.passageirosDo(referencias: ReferenciasDaPassagem): List<PassageiroDoBilhete> =
    when (this) {
        is PassagemDePassageiro -> clientes.mapIndexed { indice, id ->
            referencias.clientesPorId[id].comoPassageiro(
                papel = when {
                    clientes.size == 1 -> "Passageiro"
                    indice == 0 -> "Titular"
                    else -> "Acompanhante"
                },
            )
        }

        is PassagemDeVeiculo -> listOfNotNull(
            responsavelRetirada?.let {
                referencias.clientesPorId[it].comoPassageiro(papel = "Responsável pela retirada")
            },
        )
    }

/**
 * O documento **completo** — `TipoDocumento.formatar`, não `mascarar`.
 *
 * `null` (a pessoa que o pool não devolveu) vira linha sem nome: a ausência precisa aparecer, porque um
 * bilhete de três com dois nomes é lido como bilhete de dois.
 */
private fun Cliente?.comoPassageiro(papel: String) = PassageiroDoBilhete(
    papel = papel,
    nome = this?.nome.orEmpty(),
    documento = this?.let { "${it.tipoDocumento.rotulo} ${it.tipoDocumento.formatar(it.numeroDocumento)}" }
        .orEmpty(),
)

private fun Passagem.partidaDoBilhete(referencias: ReferenciasDaPassagem): String =
    cabecalhoDe(ocorrencia, referencias).partida