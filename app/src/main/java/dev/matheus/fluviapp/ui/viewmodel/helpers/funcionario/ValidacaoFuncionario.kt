package dev.matheus.fluviapp.ui.viewmodel.helpers.funcionario

import dev.matheus.fluviapp.ui.states.FormFuncionarioUiState

/**
 * Validação do formulário de membro — pura e JVM-testável ((state) -> resultado, sem mutar estado).
 *
 * Três obrigatórios: **nome**, **e-mail** e **ao menos um vínculo**.
 *
 * O e-mail tem forma verificada, não só presença: ele é a **chave** que casa o pré-cadastro com a conta
 * do Auth no primeiro acesso (ADR-0015 §2.1). E-mail torto aqui vira uma pessoa que nunca consegue
 * entrar — falha silenciosa e tardia, do tipo que só aparece no dia do acesso.
 *
 * O padrão é deliberadamente simples (`algo@algo.dominio`, sem espaços). Validar e-mail "de verdade" por
 * regex é folclore: quem confirma que o endereço existe é o Auth, na entrega.
 *
 * ### Por que o vínculo é obrigatório aqui e opcional no documento
 *
 * A fronteira aceita funcionário sem vínculo — documento gravado antes desta fatia, ou pessoa cujo
 * vínculo se perdeu na leitura. Já o **cadastro** exige um: quem cadastra sabe para qual empresa está
 * contratando, e uma pessoa sem vínculo nenhum não enxerga seção alguma e não emite nada. Deixar salvar
 * assim seria produzir, com um clique, o estado que o app inteiro trata como "não é da casa".
 */
private val PADRAO_EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

data class ErrosFuncionario(
    val nome: Boolean = false,
    val email: Boolean = false,
    val vinculos: Boolean = false,
) {
    val valido: Boolean get() = !nome && !email && !vinculos
}

fun validarFuncionario(state: FormFuncionarioUiState): ErrosFuncionario = ErrosFuncionario(
    nome = state.nome.isBlank(),
    email = !PADRAO_EMAIL.matches(state.email.trim()),
    vinculos = state.vinculos.isEmpty(),
)