package br.com.gruponaveg.services.network.data.requests.cadastro

data class ViagemRequest(
    val codigo: String,
    val idNavio: Int,
    val idTrechoOrigem: Int,
    val idTrechoDestino: Int
)
