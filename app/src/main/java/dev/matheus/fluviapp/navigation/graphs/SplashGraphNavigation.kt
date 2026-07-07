package dev.matheus.fluviapp.navigation.graphs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.matheus.fluviapp.navigation.destinations.FluviAppGraphDestinations
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

        when (state.splashScreenState) {
            SplashScreenState.Carregando -> Box(Modifier.fillMaxSize()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            SplashScreenState.Deslogado -> {
                LaunchedEffect(Unit) {
                    onNavegaParaLogin()
                }
            }

            SplashScreenState.Logado -> {
                LaunchedEffect(Unit) {
                    onNavegaParaHome()
                }
            }
        }
    }
}
