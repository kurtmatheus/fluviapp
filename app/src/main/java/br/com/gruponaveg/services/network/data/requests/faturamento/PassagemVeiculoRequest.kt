package br.com.gruponaveg.services.network.data.requests.faturamento

data class PassagemVeiculoRequest(
    val idPassagem: String,
    val idVeiculo: String,
    val idResponsavelRetirada: String? = null
)
