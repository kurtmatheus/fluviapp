package dev.matheus.fluviapp.navigation.navcomposables.contagem

import androidx.compose.runtime.collectAsState
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.matheus.fluviapp.extensions.sharedViewModel
import dev.matheus.fluviapp.navigation.destinations.FluviAppNavComposableDestinations
import dev.matheus.fluviapp.ui.screens.contagem.ContagemPassagemScreen
import dev.matheus.fluviapp.ui.viewmodel.contagem.ContagemPassagemViewModel

fun NavGraphBuilder.contagemPassagemNavComposable(
    navController: NavController,
    onClickVoltar: () -> Unit,
) {
    composable(
        route = FluviAppNavComposableDestinations.ContagemPassagemNavComposable.route
    ) {

        val viewModel = it.sharedViewModel<ContagemPassagemViewModel>(navController = navController)
        val state = viewModel.uiState.collectAsState()

        ContagemPassagemScreen(
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