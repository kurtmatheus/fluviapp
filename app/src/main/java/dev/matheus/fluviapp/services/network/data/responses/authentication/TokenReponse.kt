package dev.matheus.fluviapp.services.network.data.responses.authentication

import com.fasterxml.jackson.annotation.JsonProperty

data class TokenReponse(
    @JsonProperty("type") val type: String,
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("expires_at") val expiresAt: String,
    @JsonProperty("idUsuario") val idUsuario: Int
)
