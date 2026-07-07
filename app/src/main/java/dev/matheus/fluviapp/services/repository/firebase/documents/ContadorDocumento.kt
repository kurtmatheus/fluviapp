package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.model.ContadorBilhete

data class ContadorDocumento(
    val numeroBilhete: Int = 0
)

fun ContadorDocumento.toContadorBilhete(): ContadorBilhete {
    return ContadorBilhete(
        contagem = numeroBilhete
    )
}