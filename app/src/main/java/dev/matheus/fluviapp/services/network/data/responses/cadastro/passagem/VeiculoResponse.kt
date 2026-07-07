package dev.matheus.fluviapp.services.network.data.responses.cadastro.passagem

import dev.matheus.fluviapp.services.network.data.responses.cadastro.constantes.ConstanteConteudoResponse
import com.fasterxml.jackson.annotation.JsonProperty

data class VeiculoResponse(
    @JsonProperty("id") val id: String,
    @JsonProperty("modelo") val modelo: String,
    @JsonProperty("placa") val placa: String,
    @JsonProperty("cor") val cor: String,
    @JsonProperty("tipoVeiculoModel") val tipoVeiculoResponse: ConstanteConteudoResponse
)
