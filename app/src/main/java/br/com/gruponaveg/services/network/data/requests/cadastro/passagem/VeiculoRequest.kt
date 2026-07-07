package br.com.gruponaveg.services.network.data.requests.cadastro.passagem

import com.fasterxml.jackson.annotation.JsonProperty

data class VeiculoRequest(
    @JsonProperty("id") val id: String,
    @JsonProperty("modelo") val modelo: String,
    @JsonProperty("placa") val placa: String,
    @JsonProperty("cor") val cor: String,
    @JsonProperty("idTipoVeiculo") val idTipoVeiculo: Int,
)
