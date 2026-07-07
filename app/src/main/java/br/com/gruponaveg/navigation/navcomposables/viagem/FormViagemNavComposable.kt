package br.com.gruponaveg.navigation.navcomposables.viagem

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import br.com.gruponaveg.navigation.destinations.NavegAppNavComposableDestinations
import br.com.gruponaveg.ui.screens.forms.viagem.FormViagemScreen
import br.com.gruponaveg.ui.viewmodel.viagem.FormViagemViewModel
import kotlinx.coroutines.launch

internal const val ID_VIAGEM_ARGUMENT = "idViagem"

fun NavGraphBuilder.formViagemNavComposable(
    onNavegaParaMainScreen: () -> Unit,
    onCLickVoltar: () -> Unit
) {
    composable(
        route = "${NavegAppNavComposableDestinations.FormViagemNavComposable.route}/{$ID_VIAGEM_ARGUMENT}",
    ) {
        val viewModel = hiltViewModel<FormViagemViewModel>()
        val state = viewModel.uiState.collectAsState()

        val coroutineScope = rememberCoroutineScope()

        FormViagemScreen(
            uiState = state.value,
            onClickVoltar =  onCLickVoltar,
            onClickSalvar = { context ->
                if (viewModel.validacaoFormViagemHelper.isFormularioValido()) {
                    coroutineScope.launch {
                        viewModel.formViagemHelper.onNavegaParaMainScreen = onNavegaParaMainScreen
                        viewModel.salvarViagem(context = context)
                    }
                }
            }
        )
    }
}