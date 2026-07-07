package br.com.gruponaveg.services.repository.firebase.documents

import br.com.gruponaveg.model.cadastro.constantes.Constante

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