package dev.matheus.fluviapp.services.network.data.responses.cadastro.passagem

import dev.matheus.fluviapp.services.network.data.responses.cadastro.constantes.ConstanteConteudoResponse
import com.fasterxml.jackson.annotation.JsonProperty

data class PessoaResponse(
    @JsonProperty("id") val id: String,
    @JsonProperty("nome") val nome: String,
    @JsonProperty("tipoDocumentoModel") val tipoDocumentoResponse: ConstanteConteudoResponse,
    @JsonProperty("numeroDocumento") val numeroDocumento: String,
    @JsonProperty("dataNascimento") val dataNascimento: String
)
