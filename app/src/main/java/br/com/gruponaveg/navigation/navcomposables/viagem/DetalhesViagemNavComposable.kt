package br.com.gruponaveg.navigation.navcomposables.viagem

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import br.com.gruponaveg.extensions.sharedViewModel
import br.com.gruponaveg.navigation.destinations.NavegAppNavComposableDestinations
import br.com.gruponaveg.ui.screens.forms.viagem.DetalhesViagemScreen
import br.com.gruponaveg.ui.viewmodel.viagem.PesquisarViagemViewModel
import kotlinx.coroutines.launch

internal const val DETALHES_VIAGEM_ARGUMENT = "idViagem"

fun NavGraphBuilder.detalhesViagemNavComposable(
    navController: NavController,
    onClickVoltar: () -> Unit,
    onNavegaParaMainScreen: () -> Unit,
    onNavegaParaFormularioViagem: (String) -> Unit,
    onNavegaParaFormularioPassagem: (String) -> Unit,
) {
    composable(
        route = "${NavegAppNavComposableDestinations.DetalhesViagemNavComposable.route}/{$DETALHES_VIAGEM_ARGUMENT}"
    ) { navBackStackEntry ->
        navBackStackEntry.arguments?.getString(DETALHES_VIAGEM_ARGUMENT)?.let {

            val viewModel = navBackStackEntry.sharedViewModel<PesquisarViagemViewModel>(navController = navController)
            val state = viewModel.uiState.collectAsState()
            val coroutineScope = rememberCoroutineScope()
            val context = LocalContext.current

            viewModel.carregarDadosSelecionados(it)

            DetalhesViagemScreen(
                state = state.value,
                onClickVoltar = onClickVoltar,
                isShowConfirmDeleteDialog = state.value.isShowDeleteDialog,
                onShowConfirmDeleteDialog = {
                    viewModel.exibirConfirmDeleteDialog()
                },
                onClickConfirmDialog = {
                    coroutineScope.launch {
                        viewModel.onNavegaParaMainScreen = onNavegaParaMainScreen
                        viewModel.deletarViagem(it, context)
                    }
                },
                onClickDismissDialog = {
                    viewModel.exibirConfirmDeleteDialog()
                },
                onClickEditarViagem = onNavegaParaFormularioViagem,
                onClickAdicionarPassagem = onNavegaParaFormularioPassagem,
            )
        } ?: LaunchedEffect(Unit) {
            onClickVoltar()
        }
    }
}