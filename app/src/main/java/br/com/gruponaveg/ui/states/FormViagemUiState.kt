package br.com.gruponaveg.ui.states

import br.com.gruponaveg.R
import br.com.gruponaveg.model.cadastro.constantes.Constante
import br.com.gruponaveg.model.viagem.Empresa
import br.com.gruponaveg.model.viagem.Navio

data class FormViagemUiState(
    val titleJanela: Int = R.string.subtitle_cadastrar_nova_viagem,

    val empresa: String = "",
    val onEmpresaChange: (String) -> Unit = {},
    val listaEmpresas: List<Empresa> = emptyList(),
    val isEmpresaError: Boolean = false,

    val navio: String = "",
    val onNavioChange: (String) -> Unit = {},
    val listaNavios: List<Navio> = emptyList(),
    val isNavioError: Boolean = false,
    val isNavioDisable: Boolean = true,

    val listaMunicipios: List<Constante> = emptyList(),

    val trechoOrigem: String = "",
    val onTrechoOrigemChange: (String) -> Unit = {},
    val isTrechoOrigemError: Boolean = false,
    val onClickLimparTrechoOrigem: () -> Unit = {},

    val trechoDestino: String = "",
    val onTrechoDestinoChange: (String) -> Unit = {},
    val isTrechoDestinoError: Boolean = false,
    val isTrechoDestinoDisabled: Boolean = true,
    val onClickLimparTrechoDestino: () -> Unit = {},

    val isProcessando: Boolean = false
)
