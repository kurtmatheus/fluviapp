package br.com.gruponaveg.services.network.data.responses.cadastro.viagem

import br.com.gruponaveg.services.network.data.responses.cadastro.constantes.ConstanteConteudoResponse
import com.fasterxml.jackson.annotation.JsonProperty

data class ViagemResponse(
    @JsonProperty("id") val id: Int,
    @JsonProperty("codigo") val codigo: String,
    @JsonProperty("navioModel") val navioResponse: NavioResponse,
    @JsonProperty("trechoOrigemModel") val trechoOrigemResponse: ConstanteConteudoResponse,
    @JsonProperty("trechoDestinoModel") val trechoDestinoResponse: ConstanteConteudoResponse
)
