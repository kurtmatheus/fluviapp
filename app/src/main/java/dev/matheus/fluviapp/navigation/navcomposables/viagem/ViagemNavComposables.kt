package dev.matheus.fluviapp.navigation.navcomposables.viagem

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.extensions.toastMessage
import dev.matheus.fluviapp.navigation.destinations.FluviAppNavComposableDestinations
import dev.matheus.fluviapp.ui.screens.forms.viagem.FormViagemScreen
import dev.matheus.fluviapp.ui.screens.forms.viagem.ResultSearchViagemScreen
import dev.matheus.fluviapp.ui.viewmodel.viagem.FormViagemViewModel
import dev.matheus.fluviapp.ui.viewmodel.viagem.PesquisaViagemViewModel

/**
 * Sem argumento de viagem, e é a decisão em forma de assinatura: não existe editar (§7.1).
 *
 * O destino antigo tinha `?idViagem={idViagem}` justamente para editar. A ausência dele aqui é o terceiro
 * lugar onde a imutabilidade está dita — a porta do repositório e o texto da tela são os outros dois.
 */
fun NavGraphBuilder.formViagemNavComposable(
    onNavegaParaMainScreen: () -> Unit,
    onClickVoltar: () -> Unit,
) {
    composable(
        route = FluviAppNavComposableDestinations.FormViagemNavComposable.route,
    ) {
        val viewModel = hiltViewModel<FormViagemViewModel>()
        val state by viewModel.uiState.collectAsState()
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            viewModel.sucesso.collect {
                context.toastMessage(context.getString(R.string.msg_viagem_criada))
                onNavegaParaMainScreen()
            }
        }

        FormViagemScreen(
            uiState = state,
            onRotaChange = viewModel::onRotaChange,
            onEmbarcacaoChange = viewModel::onEmbarcacaoChange,
            onDiaSemanaChange = viewModel::onDiaSemanaChange,
            onHoraChange = viewModel::onHoraChange,
            onClickSalvar = viewModel::salvar,
            onClickVoltar = onClickVoltar,
        )
    }
}

fun NavGraphBuilder.resultSearchViagemNavComposable(
    onClickVoltar: () -> Unit,
) {
    composable(
        route = FluviAppNavComposableDestinations.ResultPesquisarViagemNavComposable.route,
    ) {
        val viewModel = hiltViewModel<PesquisaViagemViewModel>()
        val uiState by viewModel.uiState.collectAsState()

        ResultSearchViagemScreen(
            uiState = uiState,
            onFiltroChange = viewModel::onFiltroChange,
            onClickVoltar = onClickVoltar,
            onInativar = viewModel::onInativar,
        )
    }
}