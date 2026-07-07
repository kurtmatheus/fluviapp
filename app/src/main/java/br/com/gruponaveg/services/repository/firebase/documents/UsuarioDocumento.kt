package br.com.gruponaveg.services.repository.firebase.documents

import br.com.gruponaveg.model.operacoes.Usuario

data class UsuarioDocumento(
    val email: String = "",
    val nome: String = "",
    val cargo: String = ""
)

fun UsuarioDocumento.toUsuario(id: String): Usuario {
    return Usuario(
        id = id,
        email = email,
        senha = "",
        nome = nome,
        cargo = cargo
    )
}
