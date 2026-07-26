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
        agencia = state.agencia.isBlank(),
        agente = state.agente.isBlank(),
        formaPagamento = semFormaPagamento,
        valorPix = state.isPixChecked && state.valorPix.isBlank(),
        valorDinheiro = state.isDinheiroChecked && state.valorDinheiro.isBlank(),
        valorDebito = state.isDebitoChecked && state.valorDebito.isBlank(),
        valorCredito = state.isCreditoChecked && state.valorCredito.isBlank(),
    )
}