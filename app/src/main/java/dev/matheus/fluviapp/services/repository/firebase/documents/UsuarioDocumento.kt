package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.model.operacoes.Agencia
import dev.matheus.fluviapp.model.operacoes.Usuario

data class UsuarioDocumento(
    val email: String = "",
    val nome: String = "",
    val cargo: String = "",
    // Capacidades organizacionais (ADR-0015 §2). Aditivos e schemaless: perfil gravado antes destes
    // campos simplesmente não os tem, e o default vazio cobre a leitura.
    val agencia: String = "",
    val lotacao: String = ""
)

fun UsuarioDocumento.toUsuario(id: String): Usuario {
    return Usuario(
        id = id,
        email = email,
        nome = nome,
        cargo = cargo,
        // Fronteira: ausente/desconhecido vira AUTONOMO (a agência coringa), então o modelo nunca carrega
        // agência vazia. O cargo NÃO recebe tratamento equivalente de propósito — lá, desconhecido tem que
        // virar "sem permissão" (fail-closed, ADR-0010), não um default.
        agencia = Agencia.deOuPadrao(agencia).name,
        lotacao = lotacao
    )
}