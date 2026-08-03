package dev.matheus.fluviapp.ui.states.passagem

import androidx.room.Ignore
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Descricao.GRATUIDADE
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Descricao.MEIA
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Descricao.REDE
import dev.matheus.fluviapp.domain.passagem.TipoGratuidade
import dev.matheus.fluviapp.domain.passagem.TipoPassagem

data class FormPassageiroUiState(
    val listaNomePassageiro: List<String> = emptyList(),

    val tipoDocumentoPassageiro1: String = "",
    val isTipoDocumentoPassageiro1Error: Boolean = false,

    val documentoPassageiro1: String = "",
    val isDocumentoPassageiro1Error: Boolean = false,
    val isDocumentoPassageiro1Disabled: Boolean = true,

    val nomePassageiro1: String = "",
    val isNomePassageiro1Error: Boolean = false,

    val dataNascimentoPassageiro1: String = "",
    val isDataNascimentoPassageiro1Error: Boolean = false,
    val textDataNascimentoError: Int = 0,

    val isPassageiro2Checked: Boolean = false,

    val tipoDocumentoPassageiro2: String = "",
    val isTipoDocumentoPassageiro2Error: Boolean = false,

    val documentoPassageiro2: String = "",
    val isDocumentoPassageiro2Error: Boolean = false,
    val isDocumentoPassageiro2Disabled: Boolean = true,

    val nomePassageiro2: String = "",
    val isNomePassageiro2Error: Boolean = false,

    val dataNascimentoPassageiro2: String = "",
    val isDataNascimentoPassageiro2Error: Boolean = false,

    val isPassageiro3Checked: Boolean = false,

    val tipoDocumentoPassageiro3: String = "",
    val isTipoDocumentoPassageiro3Error: Boolean = false,

    val documentoPassageiro3: String = "",
    val isDocumentoPassageiro3Error: Boolean = false,
    val isDocumentoPassageiro3Disabled: Boolean = true,

    val nomePassageiro3: String = "",
    val isNomePassageiro3Error: Boolean = false,

    val dataNascimentoPassageiro3: String = "",
    val isDataNascimentoPassageiro3Error: Boolean = false,

    val listaAcomodacao: List<Constante> = emptyList(),
    val acomodacao: String = "",
    val isAcomodacaoError: Boolean = false,

    val listaTipoPassagem: List<String> = TipoPassagem.entries.map { it.name },
    val tipoPassagem: String = "",
    val isTipoPassagemError: Boolean = false,

    // Quatro gratuidades legais (ADR-0013). O catálogo semeado oferecia oito, incluindo `CORTESIA`, que o
    // ADR-0013 aposentou, e rótulos sem tipo nenhum atrás ("SEM GRATUIDADE", "ACOMPANHANTE - PeM").
    val listaTipoGratuidade: List<String> = TipoGratuidade.entries.map { it.name },
    val tipoGratuidade: String = "",
    val isTipoGratuidadeError: Boolean = false,
) {
    @Ignore
    val ehAcomodacaoRede = acomodacao == REDE.name

    @Ignore
    val isGratuidade = tipoPassagem == GRATUIDADE.name

    @Ignore
    val isMeiaPassagem = tipoPassagem == MEIA.name
}
