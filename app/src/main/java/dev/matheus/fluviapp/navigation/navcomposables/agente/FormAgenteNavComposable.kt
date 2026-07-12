package dev.matheus.fluviapp.navigation.navcomposables.agente

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
import dev.matheus.fluviapp.ui.screens.forms.agentes.FormAgenteScreen
import dev.matheus.fluviapp.ui.viewmodel.agente.FormAgenteViewModel

internal const val ID_AGENTE_ARGUMENT = "idAgente"

fun NavGraphBuilder.formAgenteNavComposable(
    onClickVoltar: () -> Unit,
    onNavegaParaMainScreen: () -> Unit,
) {
    composable(
        route = "${FluviAppNavComposableDestinations.FormAgenteNavComposable.route}?$ID_AGENTE_ARGUMENT={$ID_AGENTE_ARGUMENT}",
        arguments = listOf(
            navArgument(ID_AGENTE_ARGUMENT) {
                type = NavType.StringType
                defaultValue = ""
            }
        ),
    ) {
        val viewModel = hiltViewModel<FormAgenteViewModel>()
        val uiState by viewModel.uiState.collectAsState()
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            viewModel.sucesso.collect {
                context.toastMessage(context.getString(R.string.msg_salva_agent))
                onNavegaParaMainScreen()
            }
        }

        FormAgenteScreen(
            uiState = uiState,
            onAgenciaChange = viewModel::onAgenciaChange,
            onAgenteChange = viewModel::onAgenteChange,
            onLotacaoChange = viewModel::onLotacaoChange,
            onClickSalvar = viewModel::salvar,
            onClickVoltar = onClickVoltar,
        )
    }
}
