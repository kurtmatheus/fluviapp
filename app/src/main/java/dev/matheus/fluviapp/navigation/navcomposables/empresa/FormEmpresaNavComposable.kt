package dev.matheus.fluviapp.navigation.navcomposables.empresa

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.matheus.fluviapp.navigation.destinations.FluviAppNavComposableDestinations
import dev.matheus.fluviapp.ui.screens.forms.empresa.FormEmpresaScreen
import dev.matheus.fluviapp.ui.viewmodel.empresa.FormEmpresaViewModel

internal const val ID_EMPRESA_ARGUMENT = "idEmpresa"

fun NavGraphBuilder.formEmpresaNavComposable(
    onNavegaParaMainScreen: () -> Unit,
    onClickVoltar: () -> Unit,
) {
    composable(
        route = "${FluviAppNavComposableDestinations.FormEmpresaNavComposable.route}?$ID_EMPRESA_ARGUMENT={$ID_EMPRESA_ARGUMENT}",
        arguments = listOf(
            navArgument(ID_EMPRESA_ARGUMENT) {
                type = NavType.StringType
                defaultValue = ""
            }
        ),
    ) {
        val viewModel = hiltViewModel<FormEmpresaViewModel>()
        val state by viewModel.uiState.collectAsState()

        // Sucesso é um evento one-shot (não estado): navega uma vez, sem navegar-no-finally.
        LaunchedEffect(Unit) {
            viewModel.sucesso.collect { onNavegaParaMainScreen() }
        }

        FormEmpresaScreen(
            uiState = state,
            onNomeChange = viewModel::onNomeChange,
            onRazaoSocialChange = viewModel::onRazaoSocialChange,
            onCnpjChange = viewModel::onCnpjChange,
            onEnderecoChange = viewModel::onEnderecoChange,
            onTelefone1Change = viewModel::onTelefone1Change,
            onTelefone2Change = viewModel::onTelefone2Change,
            onAtuacaoToggle = viewModel::onAtuacaoToggle,
            onClickSalvar = viewModel::salvar,
            onClickVoltar = onClickVoltar,
        )
    }
}
