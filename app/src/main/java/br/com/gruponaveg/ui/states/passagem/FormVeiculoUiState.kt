package br.com.gruponaveg.ui.states.passagem

import br.com.gruponaveg.model.cadastro.constantes.Constante

data class FormVeiculoUiState(
    val tipoDocumentoResponsavelRetirada: String = "",
    val onTipoDocumentoResponsavelRetiradaChange: (String) -> Unit = {},
    val isTipoDocumentoResponsavelRetiradaError: Boolean = false,
    val onClickLimparTipoDocumentoResponsavelRetirada: () -> Unit = {},

    val listaNomeResponsavelRetirada: List<String> = emptyList(),
    val documentoResponsavelRetirada: String = "",
    val onDocumentoResponsavelRetiradaChange: (String) -> Unit = {},
    val isDocumentoResponsavelRetiradaError: Boolean = false,
    val isDocumentoResponsavelRetiradaReadOnly: Boolean = true,

    val nomeResponsavelRetirada: String = "",
    val onNomeResponsavelRetiradaChange: (String) -> Unit = {},
    val isNomeResponsavelRetiradaError: Boolean = false,

    val tipoVeiculo: String = "",
    val listaTipoVeiculo: List<Constante> = emptyList(),
    val onTipoVeiculoChange: (String) -> Unit = {},
    val isTipoVeiculoError: Boolean = false,

    val modeloVeiculo: String = "",
    val onModeloVeiculoChange: (String) -> Unit = {},
    val isModeloVeiculoError: Boolean = false,

    val placaVeiculo: String = "",
    val onPlacaVeiculoChange: (String) -> Unit = {},
    val isPlacaVeiculoError: Boolean = false,

    val corVeiculo: String = "",
    val onCorVeiculoChange: (String) -> Unit = {},
    val isCorVeiculoError: Boolean = false,
)
