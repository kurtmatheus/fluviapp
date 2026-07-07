package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.model.cadastro.passagem.Agente

data class AgenteDocumento(
    val nome: String = "",
    val agencia: String = "",
    val lotacao: String = "",
    val podeSelecionarFormaPagamento: Boolean = false
)

fun AgenteDocumento.toAgente(id: String): Agente {
    return Agente(
        id = id,
        descricaoNome = nome,
        agencia = agencia,
        lotacao = lotacao,
        podeSelecionarFormaPagamento = podeSelecionarFormaPagamento
    )
}