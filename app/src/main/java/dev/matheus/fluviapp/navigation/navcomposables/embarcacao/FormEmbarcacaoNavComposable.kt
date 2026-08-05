package dev.matheus.fluviapp.navigation.navcomposables.embarcacao

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
import dev.matheus.fluviapp.ui.screens.forms.embarcacao.FormEmbarcacaoScreen
import dev.matheus.fluviapp.ui.viewmodel.embarcacao.FormEmbarcacaoViewModel

internal const val ID_EMBARCACAO_ARGUMENT = "idEmbarcacao"

fun NavGraphBuilder.formEmbarcacaoNavComposable(
    onNavegaParaMainScreen: () -> Unit,
    onClickVoltar: () -> Unit,
) {
    composable(
        route = "${FluviAppNavComposableDestinations.FormEmbarcacaoNavComposable.route}?$ID_EMBARCACAO_ARGUMENT={$ID_EMBARCACAO_ARGUMENT}",
        arguments = listOf(
            navArgument(ID_EMBARCACAO_ARGUMENT) {
                type = NavType.StringType
                defaultValue = ""
            }
        ),
    ) {
        val viewModel = hiltViewModel<FormEmbarcacaoViewModel>()
        val state by viewModel.uiState.collectAsState()

        val context = LocalContext.current

        // Sucesso é um evento one-shot (não estado): navega uma vez, sem navegar-no-finally.
        //
        // O aviso vem ANTES de navegar, e por isso é Toast e não Snackbar: a tela que hospedaria a barra é
        // justamente a que está saindo (mesma razão da Empresa, em `formEmpresaNavComposable`).
        LaunchedEffect(Unit) {
            viewModel.sucesso.collect {
                context.toastMessage(context.getString(R.string.msg_embarcacao_salva))
                onNavegaParaMainScreen()
            }
        }

        FormEmbarcacaoScreen(
            uiState = state,
            onNomeChange = viewModel::onNomeChange,
            onTipoChange = viewModel::onTipoChange,
            onEmpresaChange = viewModel::onEmpresaChange,
            onCapacidadeVeiculoChange = viewModel::onCapacidadeVeiculoChange,
            onCapacidadeSuite2Change = viewModel::onCapacidadeSuite2Change,
            onCapacidadeSuite3Change = viewModel::onCapacidadeSuite3Change,
            onCapacidadeCamaroteChange = viewModel::onCapacidadeCamaroteChange,
            onClickSalvar = viewModel::salvar,
            onClickVoltar = onClickVoltar,
        )
    }
}
