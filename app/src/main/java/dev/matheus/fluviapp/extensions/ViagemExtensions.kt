package dev.matheus.fluviapp.extensions

import dev.matheus.fluviapp.model.viagem.Viagem

fun Viagem.formatarCodigoViagemNavioFB(): String {
    return "${origem.substring(0..2).uppercase()}-" +
            "${destino.substring(0..2).uppercase()}-" +
            navio.substring(4..7).uppercase()
}