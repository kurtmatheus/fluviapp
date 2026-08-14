package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem

import dev.matheus.fluviapp.domain.passagem.CategoriaPassagem
import dev.matheus.fluviapp.domain.passagem.Passagem
import dev.matheus.fluviapp.domain.passagem.PassagemDePassageiro
import dev.matheus.fluviapp.domain.passagem.PassagemDeVeiculo
import dev.matheus.fluviapp.domain.passagem.StatusPassagem
import dev.matheus.fluviapp.domain.passagem.TipoPassagem
import dev.matheus.fluviapp.domain.viagem.rotulo
import dev.matheus.fluviapp.ui.states.passagem.PassagemNaLista
import java.time.format.DateTimeFormatter

/**
 * `Passagem` → [PassagemNaLista]. **A projeção que não junta nada** — e é essa a característica dela.
 *
 * As outras duas resolvem referências: a conferência precisa da travessia, o bilhete precisa dos clientes. A
 * lista **não recebe referências**, porque não mostra nome nem placa: uma lista de cinquenta bilhetes que os
 * mostrasse faria cinquenta leituras de dado pessoal para preencher uma coluna de reconhecimento.
 *
 * O que ela mostra é o que basta para **achar o bilhete certo** e abri-lo: número, o que foi vendido, quando
 * sai e se ainda vale.
 */
fun Passagem.paraLista(): PassagemNaLista = PassagemNaLista(
    idPassagem = id,
    numero = "#$numero",
    bilhete = descricaoNaLista(),
    partida = ocorrencia.data.format(DIA_E_MES) + ", " + ocorrencia.data.dayOfWeek.rotulo,
    status = metadados.status.rotulo(),
    // **Encerrada mostra apagada, não some**: o bilhete cancelado continua sendo um fato, e é justamente ele
    // que alguém procura quando pergunta "o que aconteceu com aquela passagem?" (ADR-0018 D18).
    encerrada = metadados.status == StatusPassagem.CANCELADA || metadados.status == StatusPassagem.EMBARCADA,
)

private fun Passagem.descricaoNaLista(): String = when (this) {
    is PassagemDePassageiro -> listOfNotNull(
        acomodacao.rotulo,
        clientes.size.takeIf { it > 1 }?.let { "$it pessoas" },
        tipo.rotulo().takeIf { tipo != TipoPassagem.INTEIRA },
    ).joinToString(" · ")

    is PassagemDeVeiculo -> CategoriaPassagem.VEICULO.rotulo
}

private val DIA_E_MES: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")