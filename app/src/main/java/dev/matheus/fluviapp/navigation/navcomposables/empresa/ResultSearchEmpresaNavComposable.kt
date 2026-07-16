package dev.matheus.fluviapp.navigation.navcomposables.empresa

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.matheus.fluviapp.navigation.destinations.FluviAppNavComposableDestinations
import dev.matheus.fluviapp.ui.screens.forms.empresa.ResultSearchEmpresaScreen
import dev.matheus.fluviapp.ui.viewmodel.empresa.PesquisaEmpresaViewModel

fun NavGraphBuilder.resultSearchEmpresaNavComposable(
    onClickVoltar: () -> Unit,
    onNavegaParaEditorEmpresa: (String) -> Unit,
) {
    composable(
        route = FluviAppNavComposableDestinations.ResultPesquisarEmpresaNavComposable.route,
    ) {
        val viewModel = hiltViewModel<PesquisaEmpresaViewModel>()
        val uiState by viewModel.uiState.collectAsState()

        ResultSearchEmpresaScreen(
            uiState = uiState,
            onNomeChange = viewModel::onNomeChange,
            onClickVoltar = onClickVoltar,
            onNavegaParaEditor = onNavegaParaEditorEmpresa,
            onDeletar = viewModel::onDeletar,
        )
    }
}
