package br.com.gruponaveg.services.network.data.responses.cadastro.passagem

import com.fasterxml.jackson.annotation.JsonProperty

data class AgenteResponse (
    @JsonProperty("id") val id: Int,
    @JsonProperty("nome") val nome: String,
    @JsonProperty("agenciaId") val idAgencia: Int
)