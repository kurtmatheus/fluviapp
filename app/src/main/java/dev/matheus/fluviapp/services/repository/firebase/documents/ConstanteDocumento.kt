package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.model.cadastro.constantes.Constante

data class ConstanteDocumento(
    val descricao: String = "",
    val categoria: String = ""
)

fun ConstanteDocumento.toConstante(id: String): Constante {
    return Constante(
        id = id,
        descricaoNome = descricao,
        categoria = categoria
    )
}