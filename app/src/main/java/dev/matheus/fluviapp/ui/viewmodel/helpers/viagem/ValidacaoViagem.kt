package dev.matheus.fluviapp.ui.viewmodel.helpers.viagem

import dev.matheus.fluviapp.ui.states.FormViagemUiState
import java.math.BigDecimal

/**
 * Validação do formulário de viagem — pura e JVM-testável ((state) -> resultado, sem mutar estado).
 * Empresa/embarcacao/trechos são obrigatórios. Tarifa (ADR-0013) é opcional por acomodação (branco = não
 * ofertada), mas quando preenchida tem de ser número > 0 — `tarifasInvalidas` traz as chaves em erro.
 */
data class ErrosViagem(
    val empresa: Boolean = false,
    val embarcacao: Boolean = false,
    val trechoOrigem: Boolean = false,
    val trechoDestino: Boolean = false,
    val tarifasInvalidas: Set<String> = emptySet(),
) {
    val valido: Boolean
        get() = !empresa && !embarcacao && !trechoOrigem && !trechoDestino && tarifasInvalidas.isEmpty()
}

fun validarViagem(state: FormViagemUiState): ErrosViagem = ErrosViagem(
    empresa = state.empresa.isBlank(),
    embarcacao = state.embarcacao.isBlank(),
    trechoOrigem = state.trechoOrigem.isBlank(),
    trechoDestino = state.trechoDestino.isBlank(),
    tarifasInvalidas = state.tarifas
        .filter { it.valor.isNotBlank() && !tarifaValida(it.valor) }
        .map { it.chave }
        .toSet(),
)

/** Preenchida vale só se for número > 0 (aceita vírgula decimal pt-BR). */
private fun tarifaValida(valor: String): Boolean {
    val numero = valor.trim().replace(",", ".").toBigDecimalOrNull() ?: return false
    return numero > BigDecimal.ZERO
}
