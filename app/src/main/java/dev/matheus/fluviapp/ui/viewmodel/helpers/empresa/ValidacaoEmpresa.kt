package dev.matheus.fluviapp.ui.viewmodel.helpers.empresa

import dev.matheus.fluviapp.ui.states.FormEmpresaUiState

/**
 * Validação do formulário de empresa — pura e JVM-testável ((state) -> resultado, sem mutar estado).
 * nome/razão social obrigatórios; CNPJ obrigatório e válido (14 dígitos + dígitos verificadores);
 * telefones e endereço opcionais.
 */
data class ErrosEmpresa(
    val nome: Boolean = false,
    val razaoSocial: Boolean = false,
    val cnpj: Boolean = false,
) {
    val valido: Boolean get() = !nome && !razaoSocial && !cnpj
}

fun validarEmpresa(state: FormEmpresaUiState): ErrosEmpresa = ErrosEmpresa(
    nome = state.nome.isBlank(),
    razaoSocial = state.razaoSocial.isBlank(),
    cnpj = !cnpjValido(state.cnpj),
)

/** Valida CNPJ (recebe só dígitos): 14 dígitos + dígitos verificadores (mód. 11). */
fun cnpjValido(digitos: String): Boolean {
    if (digitos.length != 14 || digitos.any { !it.isDigit() }) return false
    if (digitos.all { it == digitos[0] }) return false // rejeita sequências repetidas

    fun dv(base: String, pesos: IntArray): Int {
        val soma = base.mapIndexed { i, c -> (c - '0') * pesos[i] }.sum()
        val resto = soma % 11
        return if (resto < 2) 0 else 11 - resto
    }

    val pesos1 = intArrayOf(5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
    val pesos2 = intArrayOf(6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
    val dv1 = dv(digitos.substring(0, 12), pesos1)
    val dv2 = dv(digitos.substring(0, 12) + dv1, pesos2)
    return dv1 == (digitos[12] - '0') && dv2 == (digitos[13] - '0')
}
