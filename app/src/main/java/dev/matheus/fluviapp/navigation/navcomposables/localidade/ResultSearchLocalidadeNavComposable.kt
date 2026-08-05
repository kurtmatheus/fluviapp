package dev.matheus.fluviapp.navigation.navcomposables.localidade

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.matheus.fluviapp.navigation.destinations.FluviAppNavComposableDestinations
import dev.matheus.fluviapp.ui.screens.forms.localidade.ResultSearchLocalidadeScreen
import dev.matheus.fluviapp.ui.viewmodel.localidade.PesquisaLocalidadeViewModel

fun NavGraphBuilder.resultSearchLocalidadeNavComposable(
    onClickVoltar: () -> Unit,
    onNavegaParaEditorLocalidade: (String) -> Unit,
) {
    composable(
        route = FluviAppNavComposableDestinations.ResultPesquisarLocalidadeNavComposable.route,
    ) {
        val viewModel = hiltViewModel<PesquisaLocalidadeViewModel>()
        val uiState by viewModel.uiState.collectAsState()

        ResultSearchLocalidadeScreen(
            uiState = uiState,
            onMunicipioChange = viewModel::onMunicipioChange,
            onClickVoltar = onClickVoltar,
            onNavegaParaEditor = onNavegaParaEditorLocalidade,
            onDeletar = viewModel::onDeletar,
        )
    }
}