package br.com.gruponaveg.navigation.navcomposables.passagem

import androidx.compose.runtime.collectAsState
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import br.com.gruponaveg.extensions.sharedViewModel
import br.com.gruponaveg.navigation.destinations.NavegAppNavComposableDestinations
import br.com.gruponaveg.ui.screens.forms.passagem.ResultadosPassagemSearchScreen
import br.com.gruponaveg.ui.viewmodel.PesquisarPassagemViewModel

fun NavGraphBuilder.resultPassagemNavComposable(
    navController: NavController,
    onClickVoltar: () -> Unit,
    onNavegaParaDetalhesPassagem: (String) -> Unit,
) {
    composable(
        route = NavegAppNavComposableDestinations.ResultPesquisarPassagemNavComposable.route
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