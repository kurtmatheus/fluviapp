package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.validacao

import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.extensions.converterParaLocalDate
import dev.matheus.fluviapp.ui.states.passagem.FormPassagemUiState
import java.time.LocalDate

/**
 * Validação **pura** dos dados gerais da passagem (molde ADR-0006, fatia 3): `(state, isGratuidade) ->
 * ErrosDadosPassagem`, sem mutar estado. Substitui o `ValidacaoFormPassagemHelper` impuro.
 *
 * `agencia`/`agente` **marcam o campo** mas **não entram no veredito** — comportamento preservado da
 * versão anterior (lock de teste). A validação real deles virá com a **rework do agente/Equipe**
 * (agente = usuário, plataforma multi-agência — ver `docs/design/form-passagem-validacao-exibicao.md` §6),
 * não neste incremento.
 */
data class ErrosDadosPassagem(
    val dataViagem: Boolean = false,
    /** @StringRes da mensagem do campo data (0 = sem erro). */
    val textDataViagem: Int = 0,
    val horaViagem: Boolean = false,
    val agencia: Boolean = false,
    val agente: Boolean = false,
    val formaPagamento: Boolean = false,
    val valorPago: Boolean = false,
    val valorPix: Boolean = false,
    val valorDinheiro: Boolean = false,
    val valorDebito: Boolean = false,
    val valorCredito: Boolean = false,
) {
    val valido: Boolean
        get() = !dataViagem && !horaViagem && !formaPagamento &&
            !valorPago && !valorPix && !valorDinheiro && !valorDebito && !valorCredito
}

fun validarDadosPassagem(state: FormPassagemUiState, isGratuidade: Boolean): ErrosDadosPassagem {
    // Data: validada só em NOVA passagem (editar ignora a regra). Em branco = obrigatório; no passado =
    // inválida. O curto-circuito por `isEditing` evita parsear data em branco.
    val (dataErro, textData) = when {
        state.isEditing -> false to 0
        state.dataViagem.isBlank() -> true to R.string.error_camp_obrig
        state.dataViagem.converterParaLocalDate().isBefore(LocalDate.now()) ->
            true to R.string.error_data_menor
        else -> false to 0
    }

    // Com seleção de forma de pagamento habilitada (padrão), pelo menos uma forma é exigida — salvo
    // gratuidade (isenta).
    val semFormaPagamento = state.isFormaPagamentoEnabled && !isGratuidade &&
        !state.isPixChecked && !state.isDinheiroChecked &&
        !state.isDebitoChecked && !state.isCreditoChecked

    return ErrosDadosPassagem(
        dataViagem = dataErro,
        textDataViagem = textData,
        horaViagem = state.horaViagem.isBlank(),
        agencia = state.agencia.isBlank(),
        agente = state.agente.isBlank(),
        formaPagamento = semFormaPagamento,
        valorPago = !state.isFormaPagamentoEnabled && !isGratuidade && state.valorPago.isBlank(),
        valorPix = state.isFormaPagamentoEnabled && state.isPixChecked && state.valorPix.isBlank(),
        valorDinheiro = state.isFormaPagamentoEnabled && state.isDinheiroChecked && state.valorDinheiro.isBlank(),
        valorDebito = state.isFormaPagamentoEnabled && state.isDebitoChecked && state.valorDebito.isBlank(),
        valorCredito = state.isFormaPagamentoEnabled && state.isCreditoChecked && state.valorCredito.isBlank(),
    )
}