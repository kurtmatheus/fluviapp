package dev.matheus.fluviapp.extensions

import dev.matheus.fluviapp.model.viagem.Viagem

/**
 * Deriva o código da viagem. O nome do navio vem por parâmetro (ADR-0008 Fase 3: a Viagem não guarda
 * mais o nome, ele é resolvido do `navioId` na fronteira de escrita). Mantém o formato ORI-DES-NAVI.
 */
fun Viagem.formatarCodigoViagemNavioFB(navioNome: String): String {
    val siglaNavio = if (navioNome.length >= 8) navioNome.substring(4..7) else navioNome
    return "${origem.substring(0..2).uppercase()}-" +
            "${destino.substring(0..2).uppercase()}-" +
            siglaNavio.uppercase()
}