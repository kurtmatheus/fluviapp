package dev.matheus.fluviapp.services.network.data.responses.cadastro.viagem

import com.fasterxml.jackson.annotation.JsonProperty

data class NavioResponse(
    @JsonProperty("id") val id: Int,
    @JsonProperty("nome") val nome: String,
    @JsonProperty("capacidadeVeiculo") val capacidadeCarro: Int,
    @JsonProperty("capacidadeSuite2") val capacidadeSuite2: Int,
    @JsonProperty("capacidadeSuite3") val capacidadeSuite3: Int,
    @JsonProperty("capacidadeCamarote") val capacidadeCamarote: Int,
    @JsonProperty("empresaId") val idEmpresa: Int
)
