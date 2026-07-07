package br.com.gruponaveg.services.repository.firebase.documents

import br.com.gruponaveg.model.ContadorBilhete

data class ContadorDocumento(
    val numeroBilhete: Int = 0
)

fun ContadorDocumento.toContadorBilhete(): ContadorBilhete {
    return ContadorBilhete(
        contagem = numeroBilhete
    )
}