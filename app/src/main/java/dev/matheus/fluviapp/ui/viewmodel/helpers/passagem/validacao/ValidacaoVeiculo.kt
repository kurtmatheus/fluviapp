package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.validacao

import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Descricao.MOTO
import dev.matheus.fluviapp.ui.states.passagem.FormVeiculoUiState

/**
 * Validação **pura** do sub-form de veículo (molde ADR-0006, espelha `validarViagem`): `(state) ->
 * ErrosVeiculo`, **sem mutar estado** — quem aplica os erros e decide o save é o ViewModel. Substitui o
 * `ValidacaoFormVeiculoHelper` impuro (mutava o `uiState` e relia as flags). JVM-testável.
 *
 * O responsável pela retirada é **opcional** (o veículo pode não ter um nomeado — ADR-0008/domínio): só o
 * **número** do documento é exigido quando um **tipo** de documento foi escolhido.
 */
data class ErrosVeiculo(
    val documentoResponsavel: Boolean = false,
    /** @StringRes da mensagem do documento do responsável: obrigatório × inválido (0 = sem erro). */
    val textDocumentoResponsavel: Int = 0,
    val tipoVeiculo: Boolean = false,
    val modeloVeiculo: Boolean = false,
    val placaVeiculo: Boolean = false,
    val cilindrada: Boolean = false,
) {
    val valido: Boolean
        get() = !documentoResponsavel && !tipoVeiculo && !modeloVeiculo && !placaVeiculo && !cilindrada
}

fun validarVeiculo(state: FormVeiculoUiState): ErrosVeiculo {
    // Três regras, nesta ordem:
    //  1. campo em somente-leitura = documento herdado do titular, já validado lá — não se revalida;
    //  2. editável e vazio = obrigatório, **independente de haver tipo escolhido**. É o comportamento que
    //     já existia; note que o KDoc acima diz outra coisa ("exigido quando um tipo foi escolhido") e
    //     nunca foi o que o código fazia. Mantém-se o código, que é o que a suíte cobre;
    //  3. preenchido = tem de ser válido para o tipo (ADR-0020 D2) — a parte nova.
    val doc = when {
        state.isDocumentoResponsavelRetiradaReadOnly -> ErroDocumento()
        state.documentoResponsavelRetirada.isBlank() ->
            ErroDocumento(erro = true, texto = R.string.error_camp_obrig)

        else -> validarDocumento(
            state.tipoDocumentoResponsavelRetirada,
            state.documentoResponsavelRetirada,
        )
    }

    return ErrosVeiculo(
        documentoResponsavel = doc.erro,
        textDocumentoResponsavel = doc.texto,
        tipoVeiculo = state.tipoVeiculo.isBlank(),
        modeloVeiculo = state.modeloVeiculo.isBlank(),
        placaVeiculo = state.placaVeiculo.isBlank(),
        // Moto exige cilindrada (ADR-0013) — ela alimenta a tarifaMotoBase; sem ela, bloqueio enganoso
        // "sem tarifa" na emissão.
        cilindrada = state.tipoVeiculo == MOTO.name && state.cilindrada.isBlank(),
    )
}