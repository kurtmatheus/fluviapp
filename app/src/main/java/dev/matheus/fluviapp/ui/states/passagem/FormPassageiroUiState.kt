package dev.matheus.fluviapp.ui.states.passagem

import androidx.room.Ignore
import dev.matheus.fluviapp.model.cadastro.constantes.Constante
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.GRATUIDADE
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.MEIA
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.REDE

data class FormPassageiroUiState(
    val listaNomePassageiro: List<String> = emptyList(),

    val tipoDocumentoPassageiro1: String = "",
    val onTipoDocumentoPassageiro1Change: (String) -> Unit = {},
    val isTipoDocumentoPassageiro1Error: Boolean = false,
    val onClickLimparDocumentoPassageiro1: () -> Unit = {},

    val documentoPassageiro1: String = "",
    val onDocumentoPassageiro1Change: (String) -> Unit = {},
    val isDocumentoPassageiro1Error: Boolean = false,
    val isDocumentoPassageiro1Disabled: Boolean = true,

    val nomePassageiro1: String = "",
    val onNomePassageiro1Change: (String) -> Unit = {},
    val isNomePassageiro1Error: Boolean = false,

    val dataNascimentoPassageiro1: String = "",
    val onDataNascimentoPassageiro1Change: (String) -> Unit = {},
    val isDataNascimentoPassageiro1Error: Boolean = false,
    val textDataNascimentoError: Int = 0,

    val isPassageiro2Checked: Boolean = false,
    val onCheckPassageiro2: (Boolean) -> Unit = {},

    val tipoDocumentoPassageiro2: String = "",
    val onTipoDocumentoPassageiro2Change: (String) -> Unit = {},
    val isTipoDocumentoPassageiro2Error: Boolean = false,
    val onClickLimparDocumentoPassageiro2: () -> Unit = {},

    val documentoPassageiro2: String = "",
    val onDocumentoPassageiro2Change: (String) -> Unit = {},
    val isDocumentoPassageiro2Error: Boolean = false,
    val isDocumentoPassageiro2Disabled: Boolean = true,

    val nomePassageiro2: String = "",
    val onNomePassageiro2Change: (String) -> Unit = {},
    val isNomePassageiro2Error: Boolean = false,

    val dataNascimentoPassageiro2: String = "",
    val onDataNascimentoPassageiro2Change: (String) -> Unit = {},
    val isDataNascimentoPassageiro2Error: Boolean = false,

    val isPassageiro3Checked: Boolean = false,
    val onCheckPassageiro3: (Boolean) -> Unit = {},

    val tipoDocumentoPassageiro3: String = "",
    val onTipoDocumentoPassageiro3Change: (String) -> Unit = {},
    val isTipoDocumentoPassageiro3Error: Boolean = false,
    val onClickLimparDocumentoPassageiro3: () -> Unit = {},

    val documentoPassageiro3: String = "",
    val onDocumentoPassageiro3Change: (String) -> Unit = {},
    val isDocumentoPassageiro3Error: Boolean = false,
    val isDocumentoPassageiro3Disabled: Boolean = true,

    val nomePassageiro3: String = "",
    val onNomePassageiro3Change: (String) -> Unit = {},
    val isNomePassageiro3Error: Boolean = false,

    val dataNascimentoPassageiro3: String = "",
    val onDataNascimentoPassageiro3Change: (String) -> Unit = {},
    val isDataNascimentoPassageiro3Error: Boolean = false,

    val listaAcomodacao: List<Constante> = emptyList(),
    val acomodacao: String = "",
    val isAcomodacaoSelecionada: (String) -> Boolean = { false },
    val onAcomodacaoChange: (String) -> Unit = {},
    val isAcomodacaoError: Boolean = false,

    val listaTipoPassagem: List<Constante> = emptyList(),
    val tipoPassagem: String = "",
    val onTipoPassagemChange: (String) -> Unit = {},
    val isTipoPassagemError: Boolean = false,

    val listaTipoGratuidade: List<Constante> = emptyList(),
    val tipoGratuidade: String = "",
    val onTipoGratuidadeChange: (String) -> Unit = {},
    val isTipoGratuidadeError: Boolean = false,
) {
    @Ignore
    val ehAcomodacaoRede = acomodacao == REDE.name

    @Ignore
    val isGratuidade = tipoPassagem == GRATUIDADE.name

    @Ignore
    val isMeiaPassagem = tipoPassagem == MEIA.name
}