package dev.matheus.fluviapp.navigation.graphs

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.extensions.toastMessage
import dev.matheus.fluviapp.navigation.destinations.ARG_EMAIL_PREFILL
import dev.matheus.fluviapp.navigation.destinations.ARG_EMAIL_PRIMEIRO_ACESSO
import dev.matheus.fluviapp.navigation.destinations.FluviAppGraphDestinations
import dev.matheus.fluviapp.ui.screens.LoginScreen
import dev.matheus.fluviapp.ui.screens.PrimeiroAcessoScreen
import dev.matheus.fluviapp.ui.screens.RecuperarSenhaScreen
import dev.matheus.fluviapp.ui.viewmodel.LoginViewModel
import dev.matheus.fluviapp.ui.viewmodel.PrimeiroAcessoViewModel
import dev.matheus.fluviapp.ui.viewmodel.RecuperarSenhaViewModel

fun NavGraphBuilder.loginGraph(
    onNavegarParaMainScreen: () -> Unit,
    onNavegaParaPrimeiroAcesso: (String) -> Unit,
    onNavegaParaRecuperarSenha: (String) -> Unit,
    onVoltarParaLogin: () -> Unit,
    onPrimeiroAcessoConcluido: (String) -> Unit,
) {
    composable(
        route = FluviAppGraphDestinations.LoginGraph.route
    ) { backStackEntry ->
        val viewModel = hiltViewModel<LoginViewModel>()
        val state by viewModel.uiState.collectAsState()

        val context = LocalContext.current

        // e-mail vindo do primeiro acesso: pre-preenche uma vez e consome.
        LaunchedEffect(Unit) {
            val prefill = backStackEntry.savedStateHandle.get<String>(ARG_EMAIL_PREFILL)
            if (!prefill.isNullOrBlank()) {
                viewModel.preencherEmail(prefill)
                backStackEntry.savedStateHandle.remove<String>(ARG_EMAIL_PREFILL)
            }
        }

        LaunchedEffect(key1 = state.logado) {
            if (state.logado) {
                viewModel.sincronizar(context)
            }
        }

        // Primeiro acesso deduzido (ADR-0015 §2.1): navega e consome o sinal, senão a volta para o
        // login reabriria a tela de senha.
        LaunchedEffect(key1 = state.primeiroAcessoEmail) {
            state.primeiroAcessoEmail?.let { email ->
                onNavegaParaPrimeiroAcesso(email)
                viewModel.primeiroAcessoConsumido()
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
            onClickRecuperarSenha = onNavegaParaRecuperarSenha,
        )
    }

    composable(
        route = "${FluviAppGraphDestinations.RecuperarSenha.route}?$ARG_EMAIL_PREFILL={$ARG_EMAIL_PREFILL}",
        arguments = listOf(
            navArgument(ARG_EMAIL_PREFILL) {
                type = NavType.StringType
                defaultValue = ""
            }
        )
    ) {
        val viewModel = hiltViewModel<RecuperarSenhaViewModel>()
        val state by viewModel.uiState.collectAsState()

        RecuperarSenhaScreen(
            state = state,
            onClickVoltar = onVoltarParaLogin,
            onClickEnviar = { viewModel.recuperar() },
        )
    }

    composable(
        route = "${FluviAppGraphDestinations.PrimeiroAcesso.route}?$ARG_EMAIL_PRIMEIRO_ACESSO={$ARG_EMAIL_PRIMEIRO_ACESSO}",
        arguments = listOf(
            navArgument(ARG_EMAIL_PRIMEIRO_ACESSO) {
                type = NavType.StringType
                defaultValue = ""
            }
        )
    ) { backStackEntry ->
        val viewModel = hiltViewModel<PrimeiroAcessoViewModel>()
        val state by viewModel.uiState.collectAsState()
        val context = LocalContext.current

        // Senha trocada e perfil criado: a pessoa volta ao login e entra de novo com a senha nova —
        // é o passo de confirmação do §2.1, e a sessão seguinte já nasce lendo o perfil que existe.
        LaunchedEffect(key1 = state.concluido) {
            if (state.concluido) {
                context.toastMessage(context.getString(R.string.msg_primeiro_acesso_concluido))
                onPrimeiroAcessoConcluido(
                    backStackEntry.arguments?.getString(ARG_EMAIL_PRIMEIRO_ACESSO).orEmpty()
                )
            }
        }

        PrimeiroAcessoScreen(
            state = state,
            onSenhaChange = viewModel::onSenhaChange,
            onConfirmacaoChange = viewModel::onConfirmacaoChange,
            onClickVisibilidade = viewModel::alternarVisibilidadeSenha,
            onClickConfirmar = viewModel::confirmar,
        )
    }
}