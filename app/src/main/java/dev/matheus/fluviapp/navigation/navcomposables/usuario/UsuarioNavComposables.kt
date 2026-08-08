package dev.matheus.fluviapp.navigation.navcomposables.usuario

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
import dev.matheus.fluviapp.ui.screens.forms.usuario.FormUsuarioScreen
import dev.matheus.fluviapp.ui.screens.forms.usuario.ResultSearchUsuarioScreen
import dev.matheus.fluviapp.ui.viewmodel.usuario.FormUsuarioViewModel
import dev.matheus.fluviapp.ui.viewmodel.usuario.PesquisaUsuarioViewModel

fun NavGraphBuilder.formUsuarioNavComposable(
    onNavegaParaMainScreen: () -> Unit,
    onClickVoltar: () -> Unit,
) {
    composable(
        route = FluviAppNavComposableDestinations.FormUsuarioNavComposable.route,
    ) {
        val viewModel = hiltViewModel<FormUsuarioViewModel>()
        val state by viewModel.uiState.collectAsState()
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            viewModel.sucesso.collect {
                context.toastMessage(context.getString(R.string.msg_usuario_convidado))
                onNavegaParaMainScreen()
            }
        }

        FormUsuarioScreen(
            uiState = state,
            onNomeChange = viewModel::onNomeChange,
            onEmailChange = viewModel::onEmailChange,
            onPapelChange = viewModel::onPapelChange,
            onEmpresaChange = viewModel::onEmpresaChange,
            onCargoChange = viewModel::onCargoChange,
            onClickSalvar = viewModel::salvar,
            onClickVoltar = onClickVoltar,
        )
    }
}

fun NavGraphBuilder.resultSearchUsuarioNavComposable(
    onClickVoltar: () -> Unit,
) {
    composable(
        route = FluviAppNavComposableDestinations.ResultPesquisarUsuarioNavComposable.route,
    ) {
        val viewModel = hiltViewModel<PesquisaUsuarioViewModel>()
        val uiState by viewModel.uiState.collectAsState()

        ResultSearchUsuarioScreen(
            uiState = uiState,
            onEmailChange = viewModel::onEmailChange,
            onClickVoltar = onClickVoltar,
        )
    }
}