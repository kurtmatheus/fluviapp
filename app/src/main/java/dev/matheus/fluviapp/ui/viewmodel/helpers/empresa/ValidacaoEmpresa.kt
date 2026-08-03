package dev.matheus.fluviapp.ui.viewmodel.helpers.empresa

import dev.matheus.fluviapp.domain.documento.TipoDocumento
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

/**
 * Valida CNPJ: 14 dígitos + dígitos verificadores (mód. 11).
 *
 * A regra passou a morar em [TipoDocumento] (ADR-0020 D2), que é onde todo documento é validado. Esta
 * função vira um apelido — a duplicata existia porque o CNPJ da empresa já era validado de verdade
 * enquanto o CPF do passageiro, vindo do catálogo como String, não era validado de forma alguma.
 */
fun cnpjValido(digitos: String): Boolean = TipoDocumento.CNPJ.validar(digitos)
