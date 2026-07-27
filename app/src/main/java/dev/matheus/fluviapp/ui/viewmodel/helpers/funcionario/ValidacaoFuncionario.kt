package dev.matheus.fluviapp.ui.viewmodel.helpers.funcionario

import dev.matheus.fluviapp.ui.states.FormFuncionarioUiState

/**
 * Validação do formulário de funcionário — pura e JVM-testável ((state) -> resultado, sem mutar estado).
 * Agência, nome (funcionário), **e-mail** e lotação obrigatórios.
 *
 * O e-mail tem forma verificada, não só presença: ele é a **chave** que casa o pré-cadastro com a conta
 * do Auth no primeiro acesso (ADR-0015 §2.1). E-mail torto aqui vira uma pessoa que nunca consegue
 * entrar — falha silenciosa e tardia, do tipo que só aparece no dia do acesso.
 *
 * O padrão é deliberadamente simples (`algo@algo.dominio`, sem espaços). Validar e-mail "de verdade" por
 * regex é folclore: quem confirma que o endereço existe é o Auth, na entrega.
 */
private val PADRAO_EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

data class ErrosFuncionario(
    val agencia: Boolean = false,
    val funcionario: Boolean = false,
    val email: Boolean = false,
    val lotacao: Boolean = false,
) {
    val valido: Boolean get() = !agencia && !funcionario && !email && !lotacao
}

fun validarFuncionario(state: FormFuncionarioUiState): ErrosFuncionario = ErrosFuncionario(
    agencia = state.agencia.isBlank(),
    funcionario = state.funcionario.isBlank(),
    email = !PADRAO_EMAIL.matches(state.email.trim()),
    lotacao = state.lotacao.isBlank(),
)