package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.validacao

import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.MOTO
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
    val tipoVeiculo: Boolean = false,
    val modeloVeiculo: Boolean = false,
    val placaVeiculo: Boolean = false,
    val cilindrada: Boolean = false,
) {
    val valido: Boolean
        get() = !documentoResponsavel && !tipoVeiculo && !modeloVeiculo && !placaVeiculo && !cilindrada
}

fun validarVeiculo(state: FormVeiculoUiState): ErrosVeiculo = ErrosVeiculo(
    documentoResponsavel = !state.isDocumentoResponsavelRetiradaReadOnly &&
        state.documentoResponsavelRetirada.isBlank(),
    tipoVeiculo = state.tipoVeiculo.isBlank(),
    modeloVeiculo = state.modeloVeiculo.isBlank(),
    placaVeiculo = state.placaVeiculo.isBlank(),
    // Moto exige cilindrada (ADR-0013) — ela alimenta a tarifaMotoBase; sem ela, bloqueio enganoso "sem
    // tarifa" na emissão.
    cilindrada = state.tipoVeiculo == MOTO.name && state.cilindrada.isBlank(),
)