package dev.matheus.fluviapp.services.network.data.responses.cadastro.viagem

import com.fasterxml.jackson.annotation.JsonProperty

data class EmpresaResponse(
    @JsonProperty("id") val id: Int,
    @JsonProperty("nome") val nome: String,
    @JsonProperty("razaoSocial") val razaoSocial: String,
    @JsonProperty("cnpj") val cnpj: String,
    @JsonProperty("endereco") val endereco: String,
    @JsonProperty("telefone1") val telefone1: String,
    @JsonProperty("telefone2") val telefone2: String,
)
