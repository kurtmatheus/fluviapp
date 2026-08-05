package dev.matheus.fluviapp.navigation.navcomposables.embarcacao

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.matheus.fluviapp.navigation.destinations.FluviAppNavComposableDestinations
import dev.matheus.fluviapp.ui.screens.forms.embarcacao.ResultSearchEmbarcacaoScreen
import dev.matheus.fluviapp.ui.viewmodel.embarcacao.PesquisaEmbarcacaoViewModel

fun NavGraphBuilder.resultSearchEmbarcacaoNavComposable(
    onClickVoltar: () -> Unit,
    onNavegaParaEditorEmbarcacao: (String) -> Unit,
) {
    composable(
        route = FluviAppNavComposableDestinations.ResultPesquisarEmbarcacaoNavComposable.route,
    ) {
        val viewModel = hiltViewModel<PesquisaEmbarcacaoViewModel>()
        val uiState by viewModel.uiState.collectAsState()

        ResultSearchEmbarcacaoScreen(
            uiState = uiState,
            onEmpresaChange = viewModel::onEmpresaChange,
            onClickVoltar = onClickVoltar,
            onNavegaParaEditor = onNavegaParaEditorEmbarcacao,
            onDeletar = viewModel::onDeletar,
        )
    }
}
