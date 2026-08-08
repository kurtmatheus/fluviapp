package dev.matheus.fluviapp.navigation.navcomposables.rota

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
import dev.matheus.fluviapp.ui.screens.forms.rota.FormRotaScreen
import dev.matheus.fluviapp.ui.screens.forms.rota.ResultSearchRotaScreen
import dev.matheus.fluviapp.ui.viewmodel.rota.FormRotaViewModel
import dev.matheus.fluviapp.ui.viewmodel.rota.PesquisaRotaViewModel

/** Sem argumento de rota, e é a decisão em forma de assinatura: não existe editar (§7.1). */
fun NavGraphBuilder.formRotaNavComposable(
    onNavegaParaMainScreen: () -> Unit,
    onClickVoltar: () -> Unit,
) {
    composable(
        route = FluviAppNavComposableDestinations.FormRotaNavComposable.route,
    ) {
        val viewModel = hiltViewModel<FormRotaViewModel>()
        val state by viewModel.uiState.collectAsState()
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            viewModel.sucesso.collect {
                context.toastMessage(context.getString(R.string.msg_rota_criada))
                onNavegaParaMainScreen()
            }
        }

        FormRotaScreen(
            uiState = state,
            onPortoOrigemChange = viewModel::onPortoOrigemChange,
            onPortoDestinoChange = viewModel::onPortoDestinoChange,
            onDistanciaChange = viewModel::onDistanciaChange,
            onTempoChange = viewModel::onTempoChange,
            onClickSalvar = viewModel::salvar,
            onClickVoltar = onClickVoltar,
        )
    }
}

fun NavGraphBuilder.resultSearchRotaNavComposable(
    onClickVoltar: () -> Unit,
) {
    composable(
        route = FluviAppNavComposableDestinations.ResultPesquisarRotaNavComposable.route,
    ) {
        val viewModel = hiltViewModel<PesquisaRotaViewModel>()
        val uiState by viewModel.uiState.collectAsState()

        ResultSearchRotaScreen(
            uiState = uiState,
            onPortoChange = viewModel::onPortoChange,
            onClickVoltar = onClickVoltar,
            onInativar = viewModel::onInativar,
        )
    }
}