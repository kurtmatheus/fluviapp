package dev.matheus.fluviapp.navigation.navcomposables.viagem

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.matheus.fluviapp.extensions.sharedViewModel
import dev.matheus.fluviapp.navigation.destinations.FluviAppNavComposableDestinations
import dev.matheus.fluviapp.ui.screens.forms.viagem.FormPesquisarViagemScreen
import dev.matheus.fluviapp.ui.viewmodel.viagem.PesquisarViagemViewModel

fun NavGraphBuilder.formPesquisarViagemNavComposable(
    navController: NavController,
    onClickVoltar: () -> Unit,
    onNavegaParaResultadosPesquisa: () -> Unit,
) {
    composable(
        route = FluviAppNavComposableDestinations.FormPesquisarViagemNavComposable.route
    ) {
        val viewModel = it.sharedViewModel<PesquisarViagemViewModel>(navController = navController)
        val state by viewModel.uiState.collectAsState()

        // Ir para resultados é evento one-shot: o VM valida e emite; a nav só navega.
        LaunchedEffect(Unit) {
            viewModel.irParaResultados.collect { onNavegaParaResultadosPesquisa() }
        }

        FormPesquisarViagemScreen(
            state = state,
            onCheckEmpresa = viewModel::onCheckEmpresa,
            onEmpresaChange = viewModel::onEmpresaChange,
            onCheckEmbarcacao = viewModel::onCheckEmbarcacao,
            onEmbarcacaoChange = viewModel::onEmbarcacaoChange,
            onCheckTrecho = viewModel::onCheckTrecho,
            onOrigemChange = viewModel::onOrigemChange,
            onDestinoChange = viewModel::onDestinoChange,
            onClickVoltar = onClickVoltar,
            onClickPesquisar = viewModel::pesquisar,
        )
    }
}
