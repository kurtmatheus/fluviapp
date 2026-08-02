package dev.matheus.fluviapp.navigation.graphs

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.matheus.fluviapp.navigation.destinations.FluviAppGraphDestinations
import dev.matheus.fluviapp.ui.screens.SplashScreen
import dev.matheus.fluviapp.ui.states.SplashScreenState
import dev.matheus.fluviapp.ui.viewmodel.SplashScreenViewModel

fun NavGraphBuilder.splashGraph(
    onNavegaParaLogin: () -> Unit,
    onNavegaParaHome: () -> Unit,
) {
    composable(
        route = FluviAppGraphDestinations.SplashScreen.route
    ) {
        val viewModel = hiltViewModel<SplashScreenViewModel>()
        val state by viewModel.uiState.collectAsState()

        // A tela é sempre a mesma — a marca enquanto se resolve a sessão; o estado só decide a saída.
        SplashScreen()

        LaunchedEffect(state.splashScreenState) {
            when (state.splashScreenState) {
                SplashScreenState.Deslogado -> onNavegaParaLogin()
                SplashScreenState.Logado -> onNavegaParaHome()
                SplashScreenState.Carregando -> Unit
            }
        }
    }
}