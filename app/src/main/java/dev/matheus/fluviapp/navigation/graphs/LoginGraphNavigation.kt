package dev.matheus.fluviapp.navigation.graphs

import android.util.Log
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.extensions.toastMessage
import dev.matheus.fluviapp.navigation.destinations.ARG_EMAIL_PREFILL
import dev.matheus.fluviapp.navigation.destinations.FluviAppGraphDestinations
import dev.matheus.fluviapp.services.repository.firebase.autenticacao.GoogleCredentialProvider
import dev.matheus.fluviapp.ui.screens.CadastroScreen
import dev.matheus.fluviapp.ui.screens.LoginScreen
import dev.matheus.fluviapp.ui.screens.RecuperarSenhaScreen
import dev.matheus.fluviapp.ui.viewmodel.CadastroViewModel
import dev.matheus.fluviapp.ui.viewmodel.LoginViewModel
import dev.matheus.fluviapp.ui.viewmodel.RecuperarSenhaViewModel
import kotlinx.coroutines.launch

private const val TAG_LOGIN_GRAPH = "loginGraph"

fun NavGraphBuilder.loginGraph(
    onNavegarParaMainScreen: () -> Unit,
    onNavegaParaCadastro: () -> Unit,
    onNavegaParaRecuperarSenha: (String) -> Unit,
    onVoltarParaLogin: () -> Unit,
    onVoltarComEmail: (String) -> Unit,
) {
    composable(
        route = FluviAppGraphDestinations.LoginGraph.route
    ) { backStackEntry ->
        val viewModel = hiltViewModel<LoginViewModel>()
        val state by viewModel.uiState.collectAsState()

        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        // e-mail vindo do cadastro (colisao): pre-preenche uma vez e consome.
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
            onClickRecuperarSenha = onNavegaParaRecuperarSenha,
            onClickGoogle = {
                scope.launch {
                    try {
                        val serverClientId = context.getString(R.string.default_web_client_id)
                        val idToken = GoogleCredentialProvider.obterIdToken(context, serverClientId)
                        viewModel.onNavegaParaMainScreen = onNavegarParaMainScreen
                        viewModel.autenticarComGoogle(idToken)
                    } catch (e: GetCredentialCancellationException) {
                        Log.i(TAG_LOGIN_GRAPH, "Login Google cancelado pelo usuário")
                    } catch (e: Exception) {
                        // NoCredentialException, ProviderConfigurationException etc. entram aqui.
                        Log.e(TAG_LOGIN_GRAPH, "Credential Manager falhou: ${e.javaClass.simpleName}: ${e.message}", e)
                        viewModel.falhaLoginGoogle()
                    }
                }
            },
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

        LaunchedEffect(key1 = state.irParaLoginComEmail) {
            state.irParaLoginComEmail?.let { email ->
                context.toastMessage(context.getString(R.string.error_email_ja_cadastrado))
                onVoltarComEmail(email)
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