package dev.matheus.fluviapp.ui.viewmodel.helpers.usuario

import dev.matheus.fluviapp.domain.operacoes.Usuario
import dev.matheus.fluviapp.ui.states.FormUsuarioUiState

/**
 * Validação do convite — pura e JVM-testável.
 *
 * Três obrigatórios sempre (nome, e-mail e papel) e **dois condicionais**: empresa e cargo, exigidos só
 * quando o papel é `OPERADOR`. A condicional não é conveniência de tela — é o §8.1 escrito como regra:
 * papel de plataforma não tem registro na operação, então um convite de `ADM` com empresa seria um
 * convite que promete um vínculo que ninguém vai criar.
 *
 * O e-mail tem forma verificada porque é a **chave**: é o id do convite e é o que o primeiro acesso
 * procura. E-mail torto aqui vira uma pessoa que nunca encontra o próprio papel.
 */
private val PADRAO_EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

data class ErrosUsuario(
    val nome: Boolean = false,
    val email: Boolean = false,
    val papel: Boolean = false,
    val empresa: Boolean = false,
    val cargo: Boolean = false,
) {
    val valido: Boolean get() = !nome && !email && !papel && !empresa && !cargo
}

fun validarUsuario(state: FormUsuarioUiState): ErrosUsuario {
    val ehOperador = state.papel == Usuario.Papel.OPERADOR

    return ErrosUsuario(
        nome = state.nome.isBlank(),
        email = !PADRAO_EMAIL.matches(state.email.trim()),
        papel = state.papel == null,
        empresa = ehOperador && state.empresa.isBlank(),
        cargo = ehOperador && state.cargo.isBlank(),
    )
}