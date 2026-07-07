package dev.matheus.fluviapp.extensions

import dev.matheus.fluviapp.ui.states.passagem.FormPassagemUiState

fun FormPassagemUiState.obterValorTotalAPagar(): Double {
    return valorPago.ifBlank { "0" }.toDouble() +
            valorPix.ifBlank { "0" }.toDouble() +
            valorDinheiro.ifBlank { "0" }.toDouble() +
            valorDebito.ifBlank { "0" }.toDouble() +
            valorCredito.ifBlank { "0" }.toDouble()
}