package dev.matheus.fluviapp.navigation.navcomposables.porto

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
import dev.matheus.fluviapp.ui.screens.forms.porto.FormPortoScreen
import dev.matheus.fluviapp.ui.viewmodel.porto.FormPortoViewModel

internal const val ID_PORTO_ARGUMENT = "idPorto"

fun NavGraphBuilder.formPortoNavComposable(
    onNavegaParaMainScreen: () -> Unit,
    onClickVoltar: () -> Unit,
) {
    composable(
        route = "${FluviAppNavComposableDestinations.FormPortoNavComposable.route}?$ID_PORTO_ARGUMENT={$ID_PORTO_ARGUMENT}",
        arguments = listOf(
            navArgument(ID_PORTO_ARGUMENT) {
                type = NavType.StringType
                defaultValue = ""
            }
        ),
    ) {
        val viewModel = hiltViewModel<FormPortoViewModel>()
        val state by viewModel.uiState.collectAsState()

        val context = LocalContext.current

        // Sucesso é um evento one-shot: avisa e navega uma vez (a tela que hospedaria um Snackbar é
        // justamente a que está saindo).
        LaunchedEffect(Unit) {
            viewModel.sucesso.collect {
                context.toastMessage(context.getString(R.string.msg_porto_salvo))
                onNavegaParaMainScreen()
            }
        }

        FormPortoScreen(
            uiState = state,
            onNomeChange = viewModel::onNomeChange,
            onLocalidadeChange = viewModel::onLocalidadeChange,
            onClickSalvar = viewModel::salvar,
            onClickVoltar = onClickVoltar,
        )
    }
}