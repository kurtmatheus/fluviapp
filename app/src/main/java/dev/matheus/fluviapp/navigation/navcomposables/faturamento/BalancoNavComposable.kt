package dev.matheus.fluviapp.navigation.navcomposables.faturamento

import androidx.compose.runtime.collectAsState
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.matheus.fluviapp.extensions.sharedViewModel
import dev.matheus.fluviapp.navigation.destinations.FluviAppNavComposableDestinations
import dev.matheus.fluviapp.ui.screens.faturamento.BalancoScreen
import dev.matheus.fluviapp.ui.viewmodel.faturamento.BalancoViewModel

fun NavGraphBuilder.balancoNavComposable(
    navController: NavController,
    onClickVoltar: () -> Unit,
) {
    composable(
        route = FluviAppNavComposableDestinations.BalancoNavComposable.route
    ) {

        val viewModel = it.sharedViewModel<BalancoViewModel>(navController = navController)
        val state = viewModel.uiState.collectAsState()

        BalancoScreen(
            state = state.value,
            onClickVoltar = onClickVoltar,
            onClickPesquisar = { data ->
                if (viewModel.helper.validarFormulario()) {
                    viewModel.helper.atualizarProcessamento()
                    viewModel.atualizarLista(data)
                }
            }
        )
    }
}