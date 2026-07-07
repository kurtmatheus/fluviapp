package br.com.gruponaveg.navigation.navcomposables.viagem

import androidx.compose.runtime.collectAsState
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import br.com.gruponaveg.extensions.sharedViewModel
import br.com.gruponaveg.navigation.destinations.NavegAppNavComposableDestinations
import br.com.gruponaveg.ui.screens.forms.viagem.ResultadosViagemSearchScreen
import br.com.gruponaveg.ui.viewmodel.viagem.PesquisarViagemViewModel

fun NavGraphBuilder.resultPesquisarViagemNavComposable(
    navController: NavController,
    onClickVoltar: () -> Unit,
    onNavegParaDetalhesViagem: (String) -> Unit
) {
    composable(
        route = NavegAppNavComposableDestinations.ResultPesquisarViagemNavComposable.route
    ) {
        val viewModel = it.sharedViewModel<PesquisarViagemViewModel>(navController = navController)
        val state = viewModel.uiState.collectAsState()

        ResultadosViagemSearchScreen(
            state = state.value,
            onClickVoltar = onClickVoltar,
            onClickViagem = onNavegParaDetalhesViagem
        )
    }
}