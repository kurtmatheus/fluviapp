package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.model.operacoes.Funcionario

data class FuncionarioDocumento(
    val nome: String = "",
    val agencia: String = "",
    val lotacao: String = "",
    /** Cargo de negócio (ADR-0015 §8.1). Documento gravado antes deste campo lê como vazio. */
    val cargo: String = ""
)

fun FuncionarioDocumento.toFuncionario(id: String): Funcionario {
    return Funcionario(
        id = id,
        descricaoNome = nome,
        agencia = agencia,
        lotacao = lotacao,
        // Ausente vira AGENTE (o menor privilégio), não "sem cargo": quem tem registro de funcionário já
        // está na operação. Valor DESCONHECIDO, esse, atravessa cru — quem nega é a política (fail-closed).
        cargo = cargo.ifBlank { Funcionario.Cargo.AGENTE.name }
    )
}