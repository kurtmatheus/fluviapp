package dev.matheus.fluviapp.navigation.navcomposables.funcionario

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.matheus.fluviapp.navigation.destinations.FluviAppNavComposableDestinations
import dev.matheus.fluviapp.ui.screens.forms.funcionarios.ResultSearchFuncionarioScreen
import dev.matheus.fluviapp.ui.viewmodel.funcionario.PesquisaFuncionarioViewModel

fun NavGraphBuilder.resultSearchFuncionarioNavComposable(
    onClickVoltar: () -> Unit,
    onNavegaParaEditorFuncionario: (String) -> Unit,
) {
    composable(
        route = FluviAppNavComposableDestinations.ResultPesquisarFuncionarioNavComposable.route,
    ) {
        val viewModel = hiltViewModel<PesquisaFuncionarioViewModel>()
        val uiState by viewModel.uiState.collectAsState()

        ResultSearchFuncionarioScreen(
            uiState = uiState,
            onNomeChange = viewModel::onNomeChange,
            onEmpresaChange = viewModel::onEmpresaChange,
            onClickVoltar = onClickVoltar,
            onNavegaParaEditor = onNavegaParaEditorFuncionario,
            onDeletar = viewModel::onDeletar,
        )
    }
}
