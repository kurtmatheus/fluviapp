package dev.matheus.fluviapp.navigation.graphs

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.extensions.toastMessage
import dev.matheus.fluviapp.navigation.destinations.FluviAppGraphDestinations
import dev.matheus.fluviapp.ui.screens.CadastroScreen
import dev.matheus.fluviapp.ui.screens.LoginScreen
import dev.matheus.fluviapp.ui.viewmodel.CadastroViewModel
import dev.matheus.fluviapp.ui.viewmodel.LoginViewModel

fun NavGraphBuilder.loginGraph(
    onNavegarParaMainScreen: () -> Unit,
    onNavegaParaCadastro: () -> Unit,
    onVoltarParaLogin: () -> Unit,
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
            },
            onClickCadastrar = onNavegaParaCadastro,
        )
    }

    composable(
        route = FluviAppGraphDestinations.Cadastro.route
    ) {
        val viewModel = hiltViewModel<CadastroViewModel>()
        val state by viewModel.uiState.collectAsState()
        val context = LocalContext.current

        LaunchedEffect(key1 = state.cadastrado) {
            if (state.cadastrado) {
                context.toastMessage(context.getString(R.string.msg_cadastro_sucesso))
                onVoltarParaLogin()
            }
        }

        CadastroScreen(
            state = state,
            onClickVoltar = onVoltarParaLogin,
            onClickVisibilitySenha = {
                viewModel.cadastroFormHelper.updateSenhaVisible()
            },
            onClickCadastrar = {
                viewModel.cadastrar()
            },
        )
    }
}