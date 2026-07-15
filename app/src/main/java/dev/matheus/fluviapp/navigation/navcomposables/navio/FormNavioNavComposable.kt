package dev.matheus.fluviapp.navigation.navcomposables.navio

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.matheus.fluviapp.navigation.destinations.FluviAppNavComposableDestinations
import dev.matheus.fluviapp.ui.screens.forms.navio.FormNavioScreen
import dev.matheus.fluviapp.ui.viewmodel.navio.FormNavioViewModel

internal const val ID_NAVIO_ARGUMENT = "idNavio"

fun NavGraphBuilder.formNavioNavComposable(
    onNavegaParaMainScreen: () -> Unit,
    onClickVoltar: () -> Unit,
) {
    composable(
        route = "${FluviAppNavComposableDestinations.FormNavioNavComposable.route}?$ID_NAVIO_ARGUMENT={$ID_NAVIO_ARGUMENT}",
        arguments = listOf(
            navArgument(ID_NAVIO_ARGUMENT) {
                type = NavType.StringType
                defaultValue = ""
            }
        ),
    ) {
        val viewModel = hiltViewModel<FormNavioViewModel>()
        val state by viewModel.uiState.collectAsState()

        // Sucesso é um evento one-shot (não estado): navega uma vez, sem navegar-no-finally.
        LaunchedEffect(Unit) {
            viewModel.sucesso.collect { onNavegaParaMainScreen() }
        }

        FormNavioScreen(
            uiState = state,
            onNomeChange = viewModel::onNomeChange,
            onEmpresaChange = viewModel::onEmpresaChange,
            onCapacidadeVeiculoChange = viewModel::onCapacidadeVeiculoChange,
            onCapacidadeSuite2Change = viewModel::onCapacidadeSuite2Change,
            onCapacidadeSuite3Change = viewModel::onCapacidadeSuite3Change,
            onCapacidadeCamaroteChange = viewModel::onCapacidadeCamaroteChange,
            onClickSalvar = viewModel::salvar,
            onClickVoltar = onClickVoltar,
        )
    }
}
