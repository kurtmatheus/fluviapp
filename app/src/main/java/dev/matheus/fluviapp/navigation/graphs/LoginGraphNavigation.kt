package dev.matheus.fluviapp.navigation.graphs

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.matheus.fluviapp.navigation.destinations.FluviAppGraphDestinations
import dev.matheus.fluviapp.ui.screens.LoginScreen
import dev.matheus.fluviapp.ui.viewmodel.LoginViewModel

fun NavGraphBuilder.loginGraph(
    onNavegarParaMainScreen: () -> Unit
) {
    composable(
        route = FluviAppGraphDestinations.LoginGraph.route
    ) {
        val viewModel = hiltViewModel<LoginViewModel>()
        val state by viewModel.uiState.collectAsState()

        val context = LocalContext.current

        LaunchedEffect(key1 = state.logado) {
            if (state.logado) {
                viewModel.sincronizar(context)
            }
        }

        LoginScreen(
            state = state,
            onClickVisibilitySenha = {
                viewModel.loginFormHelper.updateSenhaVisible()
            },
            onClickLogar = {
                viewModel.onNavegaParaMainScreen = onNavegarParaMainScreen
                viewModel.validarLogin()
            },
            onClickReenviar = {
                viewModel.reenviarVerificacao()
            }
        )
    }
}