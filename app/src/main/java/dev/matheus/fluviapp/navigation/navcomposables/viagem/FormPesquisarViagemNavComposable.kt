package dev.matheus.fluviapp.navigation.navcomposables.viagem

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.matheus.fluviapp.extensions.sharedViewModel
import dev.matheus.fluviapp.navigation.destinations.FluviAppNavComposableDestinations
import dev.matheus.fluviapp.ui.screens.forms.viagem.FormPesquisarViagemScreen
import dev.matheus.fluviapp.ui.viewmodel.viagem.PesquisarViagemViewModel
import kotlinx.coroutines.launch

fun NavGraphBuilder.formPesquisarViagemNavComposable(
    navController: NavController,
    onClickVoltar: () -> Unit,
    onNavegaParaResultadosPesquisa: () -> Unit,
) {
    composable(
        route = FluviAppNavComposableDestinations.FormPesquisarViagemNavComposable.route
    ) {
        val viewModel = it.sharedViewModel<PesquisarViagemViewModel>(navController = navController)
        val state = viewModel.uiState.collectAsState()
        val coroutineScope = rememberCoroutineScope()

        FormPesquisarViagemScreen(
            state = state.value,
            onClickVoltar = onClickVoltar,
            onClickPesquisar = {
                coroutineScope.launch {
                    if (viewModel.validacaoFormPesquisarViagemHelper.isFormularioValido()) {
                        viewModel.carregarViagensPesquisadas()
                        onNavegaParaResultadosPesquisa()
                    }
                }
            }
        )
    }
}