package dev.matheus.fluviapp.extensions

import dev.matheus.fluviapp.domain.viagem.Viagem

/**
 * Deriva o código da viagem. O nome da embarcação vem por parâmetro (ADR-0008 Fase 3: a Viagem não guarda
 * mais o nome, ele é resolvido do `embarcacaoId` na fronteira de escrita). Mantém o formato ORI-DES-NAVI.
 */
fun Viagem.formatarCodigoViagemEmbarcacaoFB(embarcacaoNome: String): String {
    val siglaEmbarcacao = if (embarcacaoNome.length >= 8) embarcacaoNome.substring(4..7) else embarcacaoNome
    return "${origem.substring(0..2).uppercase()}-" +
            "${destino.substring(0..2).uppercase()}-" +
            siglaEmbarcacao.uppercase()
}