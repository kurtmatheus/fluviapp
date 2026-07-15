package dev.matheus.fluviapp.navigation.navcomposables.passagem

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.extensions.toastMessage
import dev.matheus.fluviapp.navigation.destinations.FluviAppNavComposableDestinations
import dev.matheus.fluviapp.ui.screens.forms.passagem.FormPassagemScreen
import dev.matheus.fluviapp.ui.viewmodel.passagem.FormPassagemViewModel
import kotlinx.coroutines.launch

internal const val FORM_PASSAGEM_ARGUMENT = "idViagem"
internal const val EDIT_PASSAGEM_ARGUMENT = "idPassagem"

fun NavGraphBuilder.formPassagemNavComposable(
    onCLickVoltar: () -> Unit,
    onNavegaParaDetalhesPassagem: (String) -> Unit
) {
    composable(
        route = "${FluviAppNavComposableDestinations.FormPassagemNavComposable.route}/{$FORM_PASSAGEM_ARGUMENT}/{$EDIT_PASSAGEM_ARGUMENT}"
    ) {

        val viewModel = hiltViewModel<FormPassagemViewModel>()
        val statePassagem = viewModel.uiStatePassagem.collectAsState()
        val statePassageiro = viewModel.uiStatePassageiro.collectAsState()
        val stateVeiculo = viewModel.uiStateVeiculo.collectAsState()
        val coroutineScope = rememberCoroutineScope()
        val context = LocalContext.current
        var scrollParaErro by remember { mutableIntStateOf(0) }

        FormPassagemScreen(
            statePassagem = statePassagem.value,
            statePassageiro = statePassageiro.value,
            stateVeiculo = stateVeiculo.value,
            scrollParaErro = scrollParaErro,
            onClickVoltar = onCLickVoltar,
            onClickAvancar = {
                if (viewModel.validarFormularios()) {
                    viewModel.formPassagemHelper.atualizarIsSaving()
                    coroutineScope.launch {
                        viewModel.salvarPassagem(context)?.let {
                            onNavegaParaDetalhesPassagem(it)
                        } ?: context.toastMessage(
                            context.getString(R.string.error_salvar_pass)
                        )
                        viewModel.formPassagemHelper.atualizarIsSaving()
                    }
                } else {
                    scrollParaErro++ // rola até o 1º campo inválido (mais acima)
                }
            },
        )
    }
}