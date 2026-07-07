package dev.matheus.fluviapp.navigation.navcomposables.passagem

import androidx.compose.runtime.collectAsState
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.matheus.fluviapp.extensions.sharedViewModel
import dev.matheus.fluviapp.navigation.destinations.FluviAppNavComposableDestinations
import dev.matheus.fluviapp.ui.screens.forms.passagem.ResultadosPassagemSearchScreen
import dev.matheus.fluviapp.ui.viewmodel.PesquisarPassagemViewModel

fun NavGraphBuilder.resultPassagemNavComposable(
    navController: NavController,
    onClickVoltar: () -> Unit,
    onNavegaParaDetalhesPassagem: (String) -> Unit,
) {
    composable(
        route = FluviAppNavComposableDestinations.ResultPesquisarPassagemNavComposable.route
    ) {
        val viewModel =
            it.sharedViewModel<PesquisarPassagemViewModel>(navController = navController)
        val state = viewModel.uiState.collectAsState()

        ResultadosPassagemSearchScreen(
            state = state.value,
            onClickVoltar = onClickVoltar,
            onClickSelecionado = onNavegaParaDetalhesPassagem,
            onClickRightIcon = {
                viewModel.showSearchBar()
            }
        )
    }
}