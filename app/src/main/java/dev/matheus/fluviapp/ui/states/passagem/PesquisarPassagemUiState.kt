package dev.matheus.fluviapp.ui.states.passagem

import androidx.room.Ignore
import dev.matheus.fluviapp.extensions.formatarDataBarrasBr
import dev.matheus.fluviapp.domain.operacoes.Usuario
import dev.matheus.fluviapp.domain.passagem.StatusPassagem
import dev.matheus.fluviapp.domain.screendata.DadosPassagem
import java.time.LocalDate

data class PesquisarPassagemUiState(
    val data: String = LocalDate.now().formatarDataBarrasBr(),
    val onDataChange: (String) -> Unit = {},
    val isDataError: Boolean = false,

    // Os três status da FSM (ADR-0012). O catálogo semeado oferecia cinco — "EM TRANSITO", "FINALIZADA" e
    // "EM ANÁLISE", que nenhuma passagem jamais teve — e **omitia** `EMBARCADA`, que existe: o filtro
    // oferecia três buscas impossíveis e escondia uma possível.
    val listaSituacaoPassagem: List<String> = StatusPassagem.entries.map { it.name },
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
