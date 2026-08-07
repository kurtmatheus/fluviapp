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
    onNavegaParaSelecaoVinculo: () -> Unit,
) {
    composable(
        route = FluviAppGraphDestinations.SplashScreen.route
    ) {
        val viewModel = hiltViewModel<SplashScreenViewModel>()
        val state by viewModel.uiState.collectAsState()

        // A tela é a marca enquanto o contexto carrega; o estado decide a saída — e, no erro, ela para
        // de ser só decorativa: mostra o que houve e oferece repetir (ADR-0020 D9).
        SplashScreen(
            houveErro = state.splashScreenState == SplashScreenState.Erro,
            onTentarNovamente = viewModel::tentarNovamente,
        )

        LaunchedEffect(state.splashScreenState) {
            when (state.splashScreenState) {
                SplashScreenState.Deslogado -> onNavegaParaLogin()
                SplashScreenState.Logado -> onNavegaParaHome()
                // Sessão boa, falta dizer em nome de quem se opera (F6.4): a pergunta vem antes do painel.
                SplashScreenState.EscolherVinculo -> onNavegaParaSelecaoVinculo()
                // Os dois ficam na splash: um espera, o outro pede ação. Nenhum navega sozinho.
                SplashScreenState.Carregando, SplashScreenState.Erro -> Unit
            }
        }
    }
}