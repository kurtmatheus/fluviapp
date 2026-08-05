package dev.matheus.fluviapp.ui.viewmodel.helpers.localidade

import dev.matheus.fluviapp.domain.localidade.Uf
import dev.matheus.fluviapp.ui.states.FormLocalidadeUiState

/**
 * Validação do formulário de localidade — pura e JVM-testável.
 *
 * Três obrigatórios (município, UF e código do IBGE), e o terceiro tem uma regra a mais: **os dois
 * primeiros dígitos do código são o código da UF**, então um código de outro estado é um erro
 * *detectável sem rede e sem tabela* (`Uf.porCodigo`). É a verificação que sobrevive ao avião: a consulta
 * ao IBGE preenche quando há rede, esta recusa a incoerência sempre.
 *
 * O que ela **não** faz é conferir se o município existe — isso é o IBGE quem sabe, e depender dele para
 * salvar transformaria indisponibilidade de terceiro em cadastro travado.
 */
data class ErrosLocalidade(
    val municipio: Boolean = false,
    val uf: Boolean = false,
    val codigoIbge: Boolean = false,
) {
    val valido: Boolean get() = !municipio && !uf && !codigoIbge
}

fun validarLocalidade(state: FormLocalidadeUiState): ErrosLocalidade = ErrosLocalidade(
    municipio = state.municipio.isBlank(),
    uf = state.uf == null,
    codigoIbge = !codigoCoerente(state.codigoIbge, state.uf),
)

/**
 * `1501402` é de Belém: `15` é o Pará. Código com tamanho errado, ou cujo prefixo não é a UF escolhida,
 * é incoerente — e a incoerência é recusada mesmo quando a UF ainda não foi escolhida, porque um código
 * de 7 dígitos que não começa por UF nenhuma não é código do IBGE.
 */
private fun codigoCoerente(codigo: String, uf: Uf?): Boolean {
    if (codigo.length != TAMANHO) return false
    val ufDoCodigo = Uf.porCodigo(codigo.take(2)) ?: return false
    return uf == null || ufDoCodigo == uf
}

private const val TAMANHO = 7