package br.com.gruponaveg.services.repository.firebase.documents

import br.com.gruponaveg.model.cadastro.passagem.Agente

data class AgenteDocumento(
    val nome: String = "",
    val agencia: String = "",
    val lotacao: String = ""
)

fun AgenteDocumento.toAgente(id: String): Agente {
    return Agente(
        id = id,
        descricaoNome = nome,
        agencia = agencia,
        lotacao = lotacao
    )
}