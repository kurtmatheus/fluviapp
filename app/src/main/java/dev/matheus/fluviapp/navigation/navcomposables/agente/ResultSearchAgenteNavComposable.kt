package dev.matheus.fluviapp.navigation.navcomposables.agente

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.matheus.fluviapp.navigation.destinations.FluviAppNavComposableDestinations
import dev.matheus.fluviapp.ui.screens.forms.agentes.ResultSearchAgenteScreen
import dev.matheus.fluviapp.ui.viewmodel.agente.PesquisaAgenteViewModel

fun NavGraphBuilder.resultSearchAgenteNavComposable(
    onClickVoltar: () -> Unit,
    onNavegaParaEditorAgente: (String) -> Unit,
) {
    composable(
        route = FluviAppNavComposableDestinations.ResultPesquisarAgenteNavComposable.route,
    ) {
        val viewModel = hiltViewModel<PesquisaAgenteViewModel>()
        val uiState by viewModel.uiState.collectAsState()

        ResultSearchAgenteScreen(
            uiState = uiState,
            onAgenciaChange = viewModel::onAgenciaChange,
            onLotacaoChange = viewModel::onLotacaoChange,
            onClickVoltar = onClickVoltar,
            onNavegaParaEditor = onNavegaParaEditorAgente,
            onDeletar = viewModel::onDeletar,
        )
    }
}
