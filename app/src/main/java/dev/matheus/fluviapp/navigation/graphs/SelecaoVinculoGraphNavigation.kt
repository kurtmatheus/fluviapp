package dev.matheus.fluviapp.navigation.graphs

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.matheus.fluviapp.navigation.destinations.FluviAppGraphDestinations
import dev.matheus.fluviapp.ui.screens.SelecaoVinculoScreen
import dev.matheus.fluviapp.ui.viewmodel.SelecaoVinculoViewModel

/**
 * A seleção de contexto (F6.4). O grafo faz o de sempre: hospeda a tela e navega **uma vez**, no evento
 * one-shot — a decisão de para onde ir não é da tela.
 */
fun NavGraphBuilder.selecaoVinculoGraph(
    onNavegaParaHome: () -> Unit,
) {
    composable(
        route = FluviAppGraphDestinations.SelecaoVinculo.route
    ) {
        val viewModel = hiltViewModel<SelecaoVinculoViewModel>()
        val state by viewModel.uiState.collectAsState()

        LaunchedEffect(Unit) {
            viewModel.escolhido.collect { onNavegaParaHome() }
        }

        SelecaoVinculoScreen(
            uiState = state,
            onEscolher = viewModel::escolher,
        )
    }
}