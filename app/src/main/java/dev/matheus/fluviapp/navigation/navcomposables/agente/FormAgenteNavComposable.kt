package dev.matheus.fluviapp.navigation.navcomposables.agente

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.matheus.fluviapp.navigation.destinations.FluviAppNavComposableDestinations
import dev.matheus.fluviapp.ui.screens.forms.agentes.FormAgenteScreen
import dev.matheus.fluviapp.ui.viewmodel.AgenteViewModel
import kotlinx.coroutines.launch

internal const val ID_AGENTE_ARGUMENT = "idAgente"

fun NavGraphBuilder.formAgenteNavComposable(
    onClickVoltar: () -> Unit,
    onNavegaParaMainScreen: () -> Unit
) {
    composable(
        route = "${FluviAppNavComposableDestinations.FormAgenteNavComposable.route}/{$ID_AGENTE_ARGUMENT}"
    ) {
        val viewModel = hiltViewModel<AgenteViewModel>()
        val uiState by viewModel.uiState.collectAsState()

        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        FormAgenteScreen(
            uiState = uiState,
            onClickVoltar = onClickVoltar,
            onClickSalvar = {
                viewModel.formAgenteHelper.onNavegaParaMainScreen = onNavegaParaMainScreen
                if (viewModel.validaFormAgenteHelper.isFormularioValido()) {
                    viewModel.formAgenteHelper.atualizarProcessamento()
                    coroutineScope.launch {
                        viewModel.salvar(context)
                    }
                }

            }
        )
    }
}