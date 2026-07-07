package dev.matheus.fluviapp.navigation.navcomposables.passagem

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.extensions.toastMessage
import dev.matheus.fluviapp.navigation.destinations.FluviAppNavComposableDestinations
import dev.matheus.fluviapp.ui.screens.forms.passagem.DetalhesPassagemScreen
import dev.matheus.fluviapp.ui.viewmodel.passagem.DetalhesPassagemViewModel
import kotlinx.coroutines.launch

internal const val DETALHES_PASSAGEM_ARGUMENT = "idPassagem"

fun NavGraphBuilder.detalhesPassagemNavComposable(
    onNavegaParaMainScreen: () -> Unit,
    onNavegaParaFormularioNovaPassagem: (String) -> Unit,
    onNavegaParaFormularioEditarPassagem: (String, String) -> Unit,
) {
    composable(
        route = "${FluviAppNavComposableDestinations.DetalhesPassagemNavComposable.route}/{$DETALHES_PASSAGEM_ARGUMENT}"
    ) {

        val viewModel = hiltViewModel<DetalhesPassagemViewModel>()
        val state by viewModel.uiState.collectAsState()
        val stateImpressao by viewModel.uiStateImpressao.collectAsState()
        val coroutineScope = rememberCoroutineScope()
        val context = LocalContext.current

        DetalhesPassagemScreen(
            state = state,
            stateImpressao = stateImpressao,
            onClickVoltar = {
                viewModel.showConfirmDialog()
            },
            onClickContinuarCadastrando = onNavegaParaFormularioNovaPassagem,
            onClickDismissReturnDialog = {
                viewModel.showConfirmDialog()
            },
            onClickConfirmReturnDialog = {
                viewModel.showConfirmDialog()
                onNavegaParaMainScreen()
            },
            onClickDeletarPassagem = {
                viewModel.showConfirmDeleteDialog()
            },
            onClickDismissDeleteDialog = {
                viewModel.showConfirmDeleteDialog()
            },
            onClickEditarPassagem = onNavegaParaFormularioEditarPassagem,
            onClickConfirmDeleteDialog = {
                coroutineScope.launch {
                    viewModel.deletarPassagem(it)
                    context.toastMessage("Passagem Deletada com Sucesso.")
                    viewModel.showConfirmDeleteDialog()
                    onNavegaParaMainScreen()
                }
            },
            onClickSelecionarImpressora = {
                viewModel.impressaoHelper.selecionarImpressora(it)
            },
            onDismissDialogImpressoras = {
                viewModel.impressaoHelper.atualizarExibirDialogSelecionarImpressora()
            },
            onClickEmitir = {
                viewModel.showSheetEmissao()
            },
            onDismissSheetEmissao = {
                viewModel.showSheetEmissao()
            },
            onClickImpressaoDigital = {
                viewModel.showSheetEmissao()
                viewModel.showDialogImpressaoDigital()
            },
            onClickImpressaoFisica = {
                viewModel.showSheetEmissao()
                viewModel.impressaoHelper.atualizarViaCliente(true)
                viewModel.impressaoHelper.validarImprimir(context)
            },
            onClickDismissDialogViaNavio = {
                viewModel.impressaoHelper.atualizarExibirDialogViaNavio()
            },
            onClickImprimirViaNavio = {
                viewModel.impressaoHelper.atualizarViaCliente(false)
                viewModel.impressaoHelper.validarImprimir(context)
                viewModel.showConfirmDialog()
            },
            onDismissDialogImpressaoDigital = {
                viewModel.showDialogImpressaoDigital()
            },
            onProcessaImageBitmap = {
                viewModel.passagemDigitalHelper.processaImagemDigital(
                    context = context,
                    imageBitmap = it
                )
                viewModel.impressaoHelper.atualizaSituacao(isImpressaoDigital = true)
                context.toastMessage(context.resources.getString(R.string.msg_emissao_bem_sucedida))
            },
            onClickMenuImpressoras = {
                viewModel.impressaoHelper.atualizarExibirDialogSelecionarImpressora()
            },
            onParearNovaImpressora = {
                viewModel.impressaoHelper.navigateBluetoothConfig(context)
            }

        )
    }
}