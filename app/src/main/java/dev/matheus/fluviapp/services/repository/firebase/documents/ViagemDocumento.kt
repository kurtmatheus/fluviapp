package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.model.viagem.Viagem

data class ViagemDocumento(
    val codigo: String = "",
    val empresa: String = "",
    val navio: String = "",
    val origem: String = "",
    val destino: String = ""
)

fun ViagemDocumento.toViagem(id: String): Viagem {
    return Viagem(
        id = id,
        codigo = codigo,
        empresa = empresa,
        navio = navio,
        origem = origem,
        destino = destino
    )
}
