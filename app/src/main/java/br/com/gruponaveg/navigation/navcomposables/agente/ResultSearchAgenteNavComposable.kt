package br.com.gruponaveg.navigation.navcomposables.agente

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import br.com.gruponaveg.navigation.destinations.NavegAppNavComposableDestinations
import br.com.gruponaveg.ui.screens.forms.agentes.ResultSearchAgenteScreen
import br.com.gruponaveg.ui.viewmodel.AgenteViewModel

fun NavGraphBuilder.resultSearchAgenteNavComposable(
    onClickVoltar: () -> Unit,
    onNavegaParaEditorAgente: (String) -> Unit
) {
    composable(
        route = "${NavegAppNavComposableDestinations.ResultPesquisarAgenteNavComposable.route}/{$ID_AGENTE_ARGUMENT}"
    ) {
        val viewModel = hiltViewModel<AgenteViewModel>()
        val uiState by viewModel.uiState.collectAsState()

        ResultSearchAgenteScreen(
            uiState = uiState,
            onClickVoltar = onClickVoltar,
            onNavegaParaEditor = onNavegaParaEditorAgente
        )
    }
}