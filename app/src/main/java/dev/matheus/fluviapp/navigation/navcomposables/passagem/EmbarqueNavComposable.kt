package dev.matheus.fluviapp.navigation.navcomposables.passagem

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.matheus.fluviapp.navigation.destinations.FluviAppNavComposableDestinations
import dev.matheus.fluviapp.ui.screens.passagem.EmbarqueScreen
import dev.matheus.fluviapp.ui.viewmodel.passagem.EmbarqueViewModel

fun NavGraphBuilder.embarqueNavComposable(
    onClickVoltar: () -> Unit,
) {
    composable(route = FluviAppNavComposableDestinations.EmbarqueNavComposable.route) {
        val viewModel = hiltViewModel<EmbarqueViewModel>()
        val state by viewModel.uiState.collectAsState()

        EmbarqueScreen(
            state = state,
            onClickVoltar = onClickVoltar,
            onQrLido = viewModel::aoLerQr,
            onConfirmar = viewModel::confirmarEmbarque,
            onReiniciar = viewModel::reiniciar
        )
    }
}