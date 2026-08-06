package dev.matheus.fluviapp.navigation.navcomposables.porto

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.matheus.fluviapp.navigation.destinations.FluviAppNavComposableDestinations
import dev.matheus.fluviapp.ui.screens.forms.porto.ResultSearchPortoScreen
import dev.matheus.fluviapp.ui.viewmodel.porto.PesquisaPortoViewModel

fun NavGraphBuilder.resultSearchPortoNavComposable(
    onClickVoltar: () -> Unit,
    onNavegaParaEditorPorto: (String) -> Unit,
) {
    composable(
        route = FluviAppNavComposableDestinations.ResultPesquisarPortoNavComposable.route,
    ) {
        val viewModel = hiltViewModel<PesquisaPortoViewModel>()
        val uiState by viewModel.uiState.collectAsState()

        ResultSearchPortoScreen(
            uiState = uiState,
            onNomeChange = viewModel::onNomeChange,
            onClickVoltar = onClickVoltar,
            onNavegaParaEditor = onNavegaParaEditorPorto,
            onDeletar = viewModel::onDeletar,
        )
    }
}
