package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.validacao

import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.extensions.converterParaLocalDate
import dev.matheus.fluviapp.ui.states.passagem.FormPassagemUiState
import java.time.LocalDate

/**
 * Validação **pura** dos dados gerais da passagem (molde ADR-0006, fatia 3): `(state, isGratuidade) ->
 * ErrosDadosPassagem`, sem mutar estado. Substitui o `ValidacaoFormPassagemHelper` impuro.
 *
 * Os campos `agencia`/`agente` saíram em P2.3 (ADR-0015 §3). Eram **validações órfãs**: exigiam o
 * preenchimento de uma área que a tela nem desenhava, e por isso marcavam o campo sem entrar no
 * veredito — um erro que ninguém via, preso por um lock de teste. Agora a agência é derivada do emissor
 * e o agente **é** o emissor; não sobrou o que o formulário validasse. Fecha o §6 do estudo do form.
 */
data class ErrosDadosPassagem(
    val dataViagem: Boolean = false,
    /** @StringRes da mensagem do campo data (0 = sem erro). */
    val textDataViagem: Int = 0,
    val horaViagem: Boolean = false,
    val formaPagamento: Boolean = false,
    val valorPix: Boolean = false,
    val valorDinheiro: Boolean = false,
    val valorDebito: Boolean = false,
    val valorCredito: Boolean = false,
) {
    val valido: Boolean
        get() = !dataViagem && !horaViagem && !formaPagamento &&
            !valorPix && !valorDinheiro && !valorDebito && !valorCredito
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

    // Pelo menos uma forma de pagamento é exigida — salvo gratuidade (isenta). Não há mais o gate de
    // capability (ADR-0015 §4a): todo emissor escolhe a forma, então a regra é só sobre a gratuidade.
    val semFormaPagamento = !isGratuidade &&
        !state.isPixChecked && !state.isDinheiroChecked &&
        !state.isDebitoChecked && !state.isCreditoChecked

    return ErrosDadosPassagem(
        dataViagem = dataErro,
        textDataViagem = textData,
        horaViagem = state.horaViagem.isBlank(),
        formaPagamento = semFormaPagamento,
        valorPix = state.isPixChecked && state.valorPix.isBlank(),
        valorDinheiro = state.isDinheiroChecked && state.valorDinheiro.isBlank(),
        valorDebito = state.isDebitoChecked && state.valorDebito.isBlank(),
        valorCredito = state.isCreditoChecked && state.valorCredito.isBlank(),
    )
}