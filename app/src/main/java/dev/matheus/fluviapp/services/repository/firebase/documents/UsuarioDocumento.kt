package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.model.operacoes.Usuario

data class UsuarioDocumento(
    val email: String = "",
    /** Credencial alternativa ao e-mail (ADR-0015 §8.1) — o nome da pessoa é do `Funcionario`. */
    val username: String = "",
    /** Papel de sistema (`ADM`/`GESTOR`/`OPERADOR`). Antes se chamava `cargo`, que agora é do negócio. */
    val papel: String = "",
    /** Elo 1-1 com `funcionarios/{id}` (ADR-0015 §8.3). Vazio em papel puro de plataforma. */
    val funcionarioId: String = ""
)

fun UsuarioDocumento.toUsuario(id: String): Usuario {
    return Usuario(
        id = id,
        email = email,
        username = username,
        // Sem default: papel desconhecido/ausente tem que virar "sem permissão" na política
        // (fail-closed, ADR-0010) — não um valor de conveniência.
        papel = papel,
        funcionarioId = funcionarioId
    )
}