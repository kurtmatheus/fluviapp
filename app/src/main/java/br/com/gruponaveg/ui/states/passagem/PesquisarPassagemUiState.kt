package br.com.gruponaveg.ui.states.passagem

import androidx.room.Ignore
import br.com.gruponaveg.extensions.formatarDataBarrasBr
import br.com.gruponaveg.model.cadastro.constantes.Constante
import br.com.gruponaveg.model.operacoes.Usuario
import br.com.gruponaveg.model.screendata.DadosPassagem
import java.time.LocalDate

data class PesquisarPassagemUiState(
    val data: String = LocalDate.now().formatarDataBarrasBr(),
    val onDataChange: (String) -> Unit = {},
    val isDataError: Boolean = false,

    val listaSituacaoPassagem: List<Constante> = emptyList(),
    val situacao: String = "",
    val onSituacaoChange: (String) -> Unit = {},
    val isSituacaoError: Boolean = false,

    val isVeiculoChecked: Boolean = false,
    val onCheckVeiculo: () -> Unit = {},

    val isPassageiroChecked: Boolean = false,
    val onCheckPassageiro: () -> Unit = {},

    val isFiltroError: Boolean = false,

    val listaResultadoPassagens: List<DadosPassagem> = emptyList(),

    val isShowDeleteDialog: Boolean = false,
    val onExibirConfirmDeleteDialog: (Boolean) -> Unit = {},

    val isShowBarraPesquisa: Boolean = false,

    val pesquisa: String = "",
    val onPesquisaChange: (String) -> Unit = {},

    val isProcessing: Boolean = false,

    val temPermissaoEspecial: Boolean = false,
    val listaOperadores: List<String> = emptyList(),
    val operador: String = Usuario.GERAL,
    val onOperadorChange: (String) -> Unit = {}
) {

    @Ignore
    val filtrarTodos = isPassageiroChecked && isVeiculoChecked

    @Ignore
    val filtrarVeiculos = !isPassageiroChecked && isVeiculoChecked

    @Ignore
    val filtrarPassageiros = isPassageiroChecked && !isVeiculoChecked
}
