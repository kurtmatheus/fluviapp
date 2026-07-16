package dev.matheus.fluviapp.navigation.navcomposables.viagem

import androidx.compose.runtime.collectAsState
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.matheus.fluviapp.extensions.sharedViewModel
import dev.matheus.fluviapp.navigation.destinations.FluviAppNavComposableDestinations
import dev.matheus.fluviapp.ui.screens.forms.viagem.ResultadosViagemSearchScreen
import dev.matheus.fluviapp.ui.viewmodel.viagem.PesquisarViagemViewModel

fun NavGraphBuilder.resultPesquisarViagemNavComposable(
    navController: NavController,
    onClickVoltar: () -> Unit,
    onNavegParaDetalhesViagem: (String) -> Unit,
    onNavegaParaFormularioViagem: (String) -> Unit,
) {
    composable(
        route = FluviAppNavComposableDestinations.ResultPesquisarViagemNavComposable.route
    ) {
        val viewModel = it.sharedViewModel<PesquisarViagemViewModel>(navController = navController)
        val state = viewModel.uiState.collectAsState()

        ResultadosViagemSearchScreen(
            state = state.value,
            onClickVoltar = onClickVoltar,
            onClickViagem = onNavegParaDetalhesViagem,
            onEditar = onNavegaParaFormularioViagem,
            onDeletar = viewModel::deletarViagem,
        )
    }
}