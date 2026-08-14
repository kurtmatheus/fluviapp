package dev.matheus.fluviapp.navigation.navcomposables.passagem

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.matheus.fluviapp.navigation.destinations.FluviAppNavComposableDestinations
import dev.matheus.fluviapp.ui.screens.passagem.PesquisaPassagemScreen
import dev.matheus.fluviapp.ui.viewmodel.passagem.PesquisaPassagemViewModel

/**
 * A busca de bilhetes na navegação (F9.6) — a ação do menu que faz a seção **Passagens** acender.
 *
 * Ela não tem argumento: o recorte não vem da rota, vem do **vínculo em vigor** (ADR-0025 D2). Passar a
 * agência pelo caminho seria oferecer, na URL, a coordenada que a política já decide — e que ninguém deve
 * poder escolher.
 */
fun NavGraphBuilder.pesquisaPassagemNavComposable(
    onClickVoltar: () -> Unit,
    onAbrirBilhete: (String) -> Unit,
) {
    composable(route = FluviAppNavComposableDestinations.PesquisaPassagemNavComposable.route) {
        val viewModel = hiltViewModel<PesquisaPassagemViewModel>()
        val state by viewModel.uiState.collectAsState()

        PesquisaPassagemScreen(
            state = state,
            onEscolherData = viewModel::escolherData,
            onAlternarStatus = viewModel::alternarStatus,
            onAlternarCategoria = viewModel::alternarCategoria,
            onAbrirBilhete = onAbrirBilhete,
            onClickVoltar = onClickVoltar,
        )
    }
}