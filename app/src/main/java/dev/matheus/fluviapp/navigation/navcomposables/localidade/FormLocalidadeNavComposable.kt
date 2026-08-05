package dev.matheus.fluviapp.navigation.navcomposables.localidade

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
import dev.matheus.fluviapp.navigation.destinations.FluviAppNavComposableDestinations
import dev.matheus.fluviapp.ui.screens.forms.localidade.FormLocalidadeScreen
import dev.matheus.fluviapp.ui.viewmodel.localidade.FormLocalidadeViewModel

internal const val ID_LOCALIDADE_ARGUMENT = "idLocalidade"

fun NavGraphBuilder.formLocalidadeNavComposable(
    onNavegaParaMainScreen: () -> Unit,
    onClickVoltar: () -> Unit,
) {
    composable(
        route = "${FluviAppNavComposableDestinations.FormLocalidadeNavComposable.route}?$ID_LOCALIDADE_ARGUMENT={$ID_LOCALIDADE_ARGUMENT}",
        arguments = listOf(
            navArgument(ID_LOCALIDADE_ARGUMENT) {
                type = NavType.StringType
                defaultValue = ""
            }
        ),
    ) {
        val viewModel = hiltViewModel<FormLocalidadeViewModel>()
        val state by viewModel.uiState.collectAsState()

        val context = LocalContext.current

        // Sucesso é um evento one-shot: avisa e navega uma vez (mesmo Toast da Empresa e da Flotilha —
        // a tela que hospedaria um Snackbar é justamente a que está saindo).
        LaunchedEffect(Unit) {
            viewModel.sucesso.collect {
                context.toastMessage(context.getString(R.string.msg_localidade_salva))
                onNavegaParaMainScreen()
            }
        }

        FormLocalidadeScreen(
            uiState = state,
            onCodigoIbgeChange = viewModel::onCodigoIbgeChange,
            onMunicipioChange = viewModel::onMunicipioChange,
            onUfChange = viewModel::onUfChange,
            onConsultarIbge = viewModel::consultarIbge,
            onClickSalvar = viewModel::salvar,
            onClickVoltar = onClickVoltar,
        )
    }
}