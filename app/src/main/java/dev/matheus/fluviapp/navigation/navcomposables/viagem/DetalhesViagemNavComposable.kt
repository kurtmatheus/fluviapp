package dev.matheus.fluviapp.navigation.navcomposables.viagem

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.extensions.sharedViewModel
import dev.matheus.fluviapp.extensions.toastMessage
import dev.matheus.fluviapp.navigation.destinations.FluviAppNavComposableDestinations
import dev.matheus.fluviapp.ui.screens.forms.viagem.DetalhesViagemScreen
import dev.matheus.fluviapp.ui.viewmodel.viagem.PesquisarViagemViewModel

internal const val DETALHES_VIAGEM_ARGUMENT = "idViagem"

fun NavGraphBuilder.detalhesViagemNavComposable(
    navController: NavController,
    onClickVoltar: () -> Unit,
    onNavegaParaMainScreen: () -> Unit,
    onNavegaParaFormularioViagem: (String) -> Unit,
    onNavegaParaFormularioPassagem: (String) -> Unit,
) {
    composable(
        route = "${FluviAppNavComposableDestinations.DetalhesViagemNavComposable.route}/{$DETALHES_VIAGEM_ARGUMENT}"
    ) { navBackStackEntry ->
        navBackStackEntry.arguments?.getString(DETALHES_VIAGEM_ARGUMENT)?.let { idViagem ->

            val viewModel = navBackStackEntry.sharedViewModel<PesquisarViagemViewModel>(navController = navController)
            val state by viewModel.uiState.collectAsState()
            val context = LocalContext.current

            // Efeito, não composição: seleciona o card uma vez por id (não a cada recomposição).
            LaunchedEffect(idViagem) { viewModel.carregarDadosSelecionados(idViagem) }

            // Exclusão: o VM sinaliza o resultado; aqui (borda de UI) fazemos toast + navegação.
            LaunchedEffect(Unit) {
                viewModel.exclusao.collect { sucesso ->
                    if (sucesso) {
                        context.toastMessage(context.getString(R.string.msg_exclusao_viagem))
                        onNavegaParaMainScreen()
                    } else {
                        context.toastMessage(context.getString(R.string.error_transmissao_exclusao))
                    }
                }
            }

            DetalhesViagemScreen(
                state = state,
                onClickVoltar = onClickVoltar,
                isShowConfirmDeleteDialog = state.isShowDeleteDialog,
                onShowConfirmDeleteDialog = { viewModel.exibirConfirmDeleteDialog() },
                onClickConfirmDialog = { viewModel.deletarViagem(idViagem) },
                onClickDismissDialog = { viewModel.exibirConfirmDeleteDialog() },
                onClickEditarViagem = onNavegaParaFormularioViagem,
                onClickAdicionarPassagem = onNavegaParaFormularioPassagem,
            )
        } ?: LaunchedEffect(Unit) {
            onClickVoltar()
        }
    }
}
