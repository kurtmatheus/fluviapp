package dev.matheus.fluviapp.services.network.data.responses.faturamento

import dev.matheus.fluviapp.services.network.data.responses.cadastro.passagem.PessoaResponse
import dev.matheus.fluviapp.services.network.data.responses.cadastro.passagem.VeiculoResponse
import com.fasterxml.jackson.annotation.JsonProperty

data class PassagemVeiculoResponse(
    @JsonProperty("id") val id: String,
    @JsonProperty("passagemModel") val passagemResponse: PassagemResponse,
    @JsonProperty("veiculoModel") val veiculoResponse: VeiculoResponse,
    @JsonProperty("responsavelRetiradaModel") val responsavelRetiradaResponse: PessoaResponse? = null
)
