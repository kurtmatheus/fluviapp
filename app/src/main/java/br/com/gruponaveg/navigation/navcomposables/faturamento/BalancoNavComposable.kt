package br.com.gruponaveg.navigation.navcomposables.faturamento

import androidx.compose.runtime.collectAsState
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import br.com.gruponaveg.extensions.sharedViewModel
import br.com.gruponaveg.navigation.destinations.NavegAppNavComposableDestinations
import br.com.gruponaveg.ui.screens.faturamento.BalancoScreen
import br.com.gruponaveg.ui.viewmodel.faturamento.BalancoViewModel

fun NavGraphBuilder.balancoNavComposable(
    navController: NavController,
    onClickVoltar: () -> Unit,
) {
    composable(
        route = NavegAppNavComposableDestinations.BalancoNavComposable.route
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