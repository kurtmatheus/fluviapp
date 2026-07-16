package dev.matheus.fluviapp.navigation.navcomposables.navio

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.matheus.fluviapp.navigation.destinations.FluviAppNavComposableDestinations
import dev.matheus.fluviapp.ui.screens.forms.navio.ResultSearchNavioScreen
import dev.matheus.fluviapp.ui.viewmodel.navio.PesquisaNavioViewModel

fun NavGraphBuilder.resultSearchNavioNavComposable(
    onClickVoltar: () -> Unit,
    onNavegaParaEditorNavio: (String) -> Unit,
) {
    composable(
        route = FluviAppNavComposableDestinations.ResultPesquisarNavioNavComposable.route,
    ) {
        val viewModel = hiltViewModel<PesquisaNavioViewModel>()
        val uiState by viewModel.uiState.collectAsState()

        ResultSearchNavioScreen(
            uiState = uiState,
            onEmpresaChange = viewModel::onEmpresaChange,
            onClickVoltar = onClickVoltar,
            onNavegaParaEditor = onNavegaParaEditorNavio,
            onDeletar = viewModel::onDeletar,
        )
    }
}
