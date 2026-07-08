package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.model.operacoes.Usuario

data class UsuarioDocumento(
    val email: String = "",
    val nome: String = "",
    val cargo: String = ""
)

fun UsuarioDocumento.toUsuario(id: String): Usuario {
    return Usuario(
        id = id,
        email = email,
        nome = nome,
        cargo = cargo
    )
}
