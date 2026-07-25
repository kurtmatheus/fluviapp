package dev.matheus.fluviapp.ui.states.passagem

import dev.matheus.fluviapp.model.cadastro.constantes.Constante

/**
 * Estado puro do sub-form de veículo (molde ADR-0006): só dados + flags + listas. Os eventos são métodos
 * do FormPassagemViewModel (delegam aos `atualizar…` do FormVeiculoHelper), threadados pelas telas.
 */
data class FormVeiculoUiState(
    val tipoDocumentoResponsavelRetirada: String = "",
    val isTipoDocumentoResponsavelRetiradaError: Boolean = false,

    val listaNomeResponsavelRetirada: List<String> = emptyList(),
    val documentoResponsavelRetirada: String = "",
    val isDocumentoResponsavelRetiradaError: Boolean = false,
    val isDocumentoResponsavelRetiradaReadOnly: Boolean = true,

    val nomeResponsavelRetirada: String = "",
    val isNomeResponsavelRetiradaError: Boolean = false,

    val tipoVeiculo: String = "",
    val listaTipoVeiculo: List<Constante> = emptyList(),
    val isTipoVeiculoError: Boolean = false,

    val modeloVeiculo: String = "",
    val isModeloVeiculoError: Boolean = false,

    val placaVeiculo: String = "",
    val isPlacaVeiculoError: Boolean = false,

    val corVeiculo: String = "",
    val isCorVeiculoError: Boolean = false,

    // Cilindrada da moto (ADR-0013) — só relevante quando tipoVeiculo = MOTO; alimenta a tarifaMotoBase.
    val cilindrada: String = "",
    val isCilindradaError: Boolean = false,
)
