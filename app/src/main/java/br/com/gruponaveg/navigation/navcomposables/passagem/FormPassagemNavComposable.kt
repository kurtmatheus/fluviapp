package br.com.gruponaveg.navigation.navcomposables.passagem

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import br.com.gruponaveg.R
import br.com.gruponaveg.extensions.toastMessage
import br.com.gruponaveg.navigation.destinations.NavegAppNavComposableDestinations
import br.com.gruponaveg.ui.screens.forms.passagem.FormPassagemScreen
import br.com.gruponaveg.ui.viewmodel.passagem.FormPassagemViewModel
import kotlinx.coroutines.launch

internal const val FORM_PASSAGEM_ARGUMENT = "idViagem"
internal const val EDIT_PASSAGEM_ARGUMENT = "idPassagem"

fun NavGraphBuilder.formPassagemNavComposable(
    onCLickVoltar: () -> Unit,
    onNavegaParaDetalhesPassagem: (String) -> Unit
) {
    composable(
        route = "${NavegAppNavComposableDestinations.FormPassagemNavComposable.route}/{$FORM_PASSAGEM_ARGUMENT}/{$EDIT_PASSAGEM_ARGUMENT}"
    ) {

        val viewModel = hiltViewModel<FormPassagemViewModel>()
        val statePassagem = viewModel.uiStatePassagem.collectAsState()
        val statePassageiro = viewModel.uiStatePassageiro.collectAsState()
        val stateVeiculo = viewModel.uiStateVeiculo.collectAsState()
        val coroutineScope = rememberCoroutineScope()
        val context = LocalContext.current

        FormPassagemScreen(
            statePassagem = statePassagem.value,
            statePassageiro = statePassageiro.value,
            stateVeiculo = stateVeiculo.value,
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
                }
            },
        )
    }
}