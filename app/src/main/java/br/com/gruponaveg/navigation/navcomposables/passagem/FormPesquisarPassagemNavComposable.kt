package br.com.gruponaveg.navigation.navcomposables.passagem

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import br.com.gruponaveg.extensions.sharedViewModel
import br.com.gruponaveg.navigation.destinations.NavegAppNavComposableDestinations
import br.com.gruponaveg.ui.screens.forms.passagem.FormPesquisarPassagemScreen
import br.com.gruponaveg.ui.viewmodel.PesquisarPassagemViewModel
import kotlinx.coroutines.launch

fun NavGraphBuilder.formPesquisarPassagemNavComposable(
    navController: NavController,
    onClickVoltar: () -> Unit,
    onNavegaParaResultadosPesquisa: () -> Unit,
) {
    composable(
        route = NavegAppNavComposableDestinations.FormPesquisarPassagemNavComposable.route
    ) {
        val viewModel =
            it.sharedViewModel<PesquisarPassagemViewModel>(navController = navController)
        val state = viewModel.uiState.collectAsState()
        val coroutineScope = rememberCoroutineScope()


        FormPesquisarPassagemScreen(
            state = state.value,
            onClickVoltar = onClickVoltar,
            onClickPesquisar = {
                if (viewModel.validacaoFormPesquisarPassagemHelper.isFormularioValido()) {
                    viewModel.onNavegaParaResultadosPesquisa = onNavegaParaResultadosPesquisa
                    viewModel.formPesquisarPassagemHelper.atualizarProcessamento()
                    coroutineScope.launch {
                        viewModel.carregarDadosPesquisados()
                    }
                }
            }
        )
    }
}