package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem

import dev.matheus.fluviapp.model.passagem.Passagem
import dev.matheus.fluviapp.ui.states.passagem.FormPassageiroUiState
import dev.matheus.fluviapp.ui.states.passagem.FormPassagemUiState

/**
 * Regra de desconto ANTAC, extraída de `FormPassagemHelper.verificarDesconto` para função pura
 * (rede de regressão do passo 1 / ADR-0004). Comportamento idêntico — o helper apenas delega.
 *
 * Regra: acomodação REDE, em nova passagem (não-edição) e não-gratuidade acumula o desconto ANTAC
 * (metade se meia-passagem) sobre o desconto já informado; caso contrário, mantém só o informado.
 */
internal fun calcularDesconto(
    statePassageiro: FormPassageiroUiState,
    statePassagem: FormPassagemUiState,
): Double {
    val descontoInformado = statePassagem.desconto.ifBlank { "0" }.toDouble()
    return if (statePassageiro.ehAcomodacaoRede &&
        !statePassagem.isEditing &&
        !statePassageiro.isGratuidade
    ) {
        val descontoAntac = if (statePassageiro.isMeiaPassagem) {
            Passagem.DESCONTO_ANTAC.toDouble().div(2.0)
        } else {
            Passagem.DESCONTO_ANTAC.toDouble()
        }
        descontoAntac + descontoInformado
    } else {
        descontoInformado
    }
}