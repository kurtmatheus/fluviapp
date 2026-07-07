package br.com.gruponaveg.extensions

import br.com.gruponaveg.model.viagem.Viagem

fun Viagem.formatarCodigoViagemNavioFB(): String {
    return "${origem.substring(0..2).uppercase()}-" +
            "${destino.substring(0..2).uppercase()}-" +
            navio.substring(4..7).uppercase()
}