package dev.matheus.fluviapp.services.network.data.responses.operacoes

import dev.matheus.fluviapp.model.operacoes.Usuario
import com.fasterxml.jackson.annotation.JsonProperty

data class UsuarioResponse(
    @JsonProperty("id") val id: Int,
    @JsonProperty("email") val email: String = "",
    @JsonProperty("nome") val nome: String = "",
    @JsonProperty("cargo") val cargo: String = ""
)

fun UsuarioResponse.toUsuario() = Usuario(
    id = id.toString(),
    email = email,
    senha = "",
    nome = nome,
    cargo = cargo,
    ultimoUsuarioLogado = true
)
