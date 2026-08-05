package dev.matheus.fluviapp.navigation.navcomposables.viagem

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
import dev.matheus.fluviapp.ui.screens.forms.viagem.FormViagemScreen
import dev.matheus.fluviapp.ui.viewmodel.viagem.FormViagemViewModel

internal const val ID_VIAGEM_ARGUMENT = "idViagem"

fun NavGraphBuilder.formViagemNavComposable(
    onNavegaParaMainScreen: () -> Unit,
    onClickVoltar: () -> Unit,
) {
    composable(
        route = "${FluviAppNavComposableDestinations.FormViagemNavComposable.route}?$ID_VIAGEM_ARGUMENT={$ID_VIAGEM_ARGUMENT}",
        arguments = listOf(
            navArgument(ID_VIAGEM_ARGUMENT) {
                type = NavType.StringType
                defaultValue = ""
            }
        ),
    ) {
        val viewModel = hiltViewModel<FormViagemViewModel>()
        val state by viewModel.uiState.collectAsState()
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            viewModel.sucesso.collect {
                context.toastMessage(context.getString(R.string.msg_transmissao_viagem))
                onNavegaParaMainScreen()
            }
        }

        FormViagemScreen(
            uiState = state,
            onEmpresaChange = viewModel::onEmpresaChange,
            onEmbarcacaoChange = viewModel::onEmbarcacaoChange,
            onTrechoOrigemChange = viewModel::onTrechoOrigemChange,
            onLimparTrechoOrigem = viewModel::onLimparTrechoOrigem,
            onTrechoDestinoChange = viewModel::onTrechoDestinoChange,
            onLimparTrechoDestino = viewModel::onLimparTrechoDestino,
            onTarifaChange = viewModel::onTarifaChange,
            onClickSalvar = viewModel::salvar,
            onClickVoltar = onClickVoltar,
        )
    }
}
