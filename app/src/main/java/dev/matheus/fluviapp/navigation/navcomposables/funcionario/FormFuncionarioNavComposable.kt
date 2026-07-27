package dev.matheus.fluviapp.navigation.navcomposables.funcionario

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
import dev.matheus.fluviapp.ui.screens.forms.funcionarios.FormFuncionarioScreen
import dev.matheus.fluviapp.ui.viewmodel.funcionario.FormFuncionarioViewModel

internal const val ID_FUNCIONARIO_ARGUMENT = "idFuncionario"

fun NavGraphBuilder.formFuncionarioNavComposable(
    onClickVoltar: () -> Unit,
    onNavegaParaMainScreen: () -> Unit,
) {
    composable(
        route = "${FluviAppNavComposableDestinations.FormFuncionarioNavComposable.route}?$ID_FUNCIONARIO_ARGUMENT={$ID_FUNCIONARIO_ARGUMENT}",
        arguments = listOf(
            navArgument(ID_FUNCIONARIO_ARGUMENT) {
                type = NavType.StringType
                defaultValue = ""
            }
        ),
    ) {
        val viewModel = hiltViewModel<FormFuncionarioViewModel>()
        val uiState by viewModel.uiState.collectAsState()
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            viewModel.sucesso.collect {
                context.toastMessage(context.getString(R.string.msg_salva_agent))
                onNavegaParaMainScreen()
            }
        }

        FormFuncionarioScreen(
            uiState = uiState,
            onAgenciaChange = viewModel::onAgenciaChange,
            onFuncionarioChange = viewModel::onFuncionarioChange,
            onEmailChange = viewModel::onEmailChange,
            onLotacaoChange = viewModel::onLotacaoChange,
            onCargoChange = viewModel::onCargoChange,
            onClickSalvar = viewModel::salvar,
            onClickVoltar = onClickVoltar,
        )
    }
}
