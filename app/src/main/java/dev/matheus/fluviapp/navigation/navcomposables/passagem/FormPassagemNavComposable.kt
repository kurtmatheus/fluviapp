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
            onNomeResponsavelRetiradaChange = viewModel::onNomeResponsavelRetiradaChange,
            onTipoDocumentoResponsavelRetiradaChange = viewModel::onTipoDocumentoResponsavelRetiradaChange,
            onClickLimparTipoDocumentoResponsavelRetirada = viewModel::onClickLimparTipoDocumentoResponsavelRetirada,
            onDocumentoResponsavelRetiradaChange = viewModel::onDocumentoResponsavelRetiradaChange,
            onTipoVeiculoChange = viewModel::onTipoVeiculoChange,
            onModeloVeiculoChange = viewModel::onModeloVeiculoChange,
            onPlacaVeiculoChange = viewModel::onPlacaVeiculoChange,
            onCorVeiculoChange = viewModel::onCorVeiculoChange,
            onCilindradaChange = viewModel::onCilindradaChange,
            onAcomodacaoChange = viewModel::onAcomodacaoChange,
            onTipoPassagemChange = viewModel::onTipoPassagemChange,
            onTipoGratuidadeChange = viewModel::onTipoGratuidadeChange,
            onNomePassageiro1Change = viewModel::onNomePassageiro1Change,
            onTipoDocumentoPassageiro1Change = viewModel::onTipoDocumentoPassageiro1Change,
            onClickLimparDocumentoPassageiro1 = viewModel::onClickLimparDocumentoPassageiro1,
            onDocumentoPassageiro1Change = viewModel::onDocumentoPassageiro1Change,
            onDataNascimentoPassageiro1Change = viewModel::onDataNascimentoPassageiro1Change,
            onCheckPassageiro2 = viewModel::onCheckPassageiro2,
            onNomePassageiro2Change = viewModel::onNomePassageiro2Change,
            onTipoDocumentoPassageiro2Change = viewModel::onTipoDocumentoPassageiro2Change,
            onClickLimparDocumentoPassageiro2 = viewModel::onClickLimparDocumentoPassageiro2,
            onDocumentoPassageiro2Change = viewModel::onDocumentoPassageiro2Change,
            onDataNascimentoPassageiro2Change = viewModel::onDataNascimentoPassageiro2Change,
            onCheckPassageiro3 = viewModel::onCheckPassageiro3,
            onNomePassageiro3Change = viewModel::onNomePassageiro3Change,
            onTipoDocumentoPassageiro3Change = viewModel::onTipoDocumentoPassageiro3Change,
            onClickLimparDocumentoPassageiro3 = viewModel::onClickLimparDocumentoPassageiro3,
            onDocumentoPassageiro3Change = viewModel::onDocumentoPassageiro3Change,
            onDataNascimentoPassageiro3Change = viewModel::onDataNascimentoPassageiro3Change,
            onCheckVeiculo = viewModel::onCheckVeiculo,
            onDataViagemChange = viewModel::onDataViagemChange,
            onHoraViagemChange = viewModel::onHoraViagemChange,
            onCheckPix = viewModel::onCheckPix,
            onCheckDinheiro = viewModel::onCheckDinheiro,
            onCheckDebito = viewModel::onCheckDebito,
            onCheckCredito = viewModel::onCheckCredito,
            onValorPagoChange = viewModel::onValorPagoChange,
            onValorPixChange = viewModel::onValorPixChange,
            onValorDinheiroChange = viewModel::onValorDinheiroChange,
            onValorDebitoChange = viewModel::onValorDebitoChange,
            onValorCreditoChange = viewModel::onValorCreditoChange,
            onObservacaoChange = viewModel::onObservacaoChange,
            scrollParaErro = scrollParaErro,
            onClickVoltar = onCLickVoltar,
            onClickAvancar = {
                if (viewModel.validarFormularios()) {
                    viewModel.formPassagemHelper.atualizarIsSaving()
                    coroutineScope.launch {
                        val id = viewModel.salvarPassagem(context)
                        viewModel.formPassagemHelper.atualizarIsSaving()
                        when {
                            id != null -> onNavegaParaDetalhesPassagem(id)
                            // Bloqueio de emissão (fail-closed): banner persistente na tela + rola até ele.
                            viewModel.uiStatePassagem.value.emissaoBloqueadaMsg != 0 -> scrollParaErro++
                            else -> context.toastMessage(context.getString(R.string.error_salvar_pass))
                        }
                    }
                } else {
                    scrollParaErro++ // rola até o 1º campo inválido (mais acima)
                }
            },
        )
    }
}