package dev.matheus.fluviapp.ui.screens.forms.passagem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.sampledata.listaAcomodacaoSample
import dev.matheus.fluviapp.sampledata.listaFormaPagamentoSample
import dev.matheus.fluviapp.ui.components.cards.ViagemCard
import dev.matheus.fluviapp.ui.components.contents.CommonTopRow
import dev.matheus.fluviapp.ui.components.forms.areas.CommonAreaForm
import dev.matheus.fluviapp.ui.components.forms.areas.passagem.ContentAreaVeiculoForm
import dev.matheus.fluviapp.ui.components.forms.areas.passagem.ContentPagamentoAreaForm
import dev.matheus.fluviapp.ui.components.forms.areas.passagem.ContentPassageiroAreaForm
import dev.matheus.fluviapp.ui.components.forms.buttons.CommonCheckboxField
import dev.matheus.fluviapp.ui.components.forms.buttons.CommonIconButton
import dev.matheus.fluviapp.ui.components.forms.fields.FormFieldCalendario
import dev.matheus.fluviapp.ui.components.forms.fields.FormFieldRelogio
import dev.matheus.fluviapp.ui.components.texts.TextSubTitleBrownItalic
import dev.matheus.fluviapp.ui.screens.forms.CommonScreenNoBottom
import dev.matheus.fluviapp.ui.states.passagem.FormPassageiroUiState
import dev.matheus.fluviapp.ui.states.passagem.FormPassagemUiState
import dev.matheus.fluviapp.ui.states.passagem.FormVeiculoUiState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FormPassagemScreen(
    statePassagem: FormPassagemUiState,
    statePassageiro: FormPassageiroUiState,
    stateVeiculo: FormVeiculoUiState,
    // Eventos do sub-form de veículo (molde ADR-0006, §1b) — threadados até o ContentAreaVeiculoForm.
    onNomeResponsavelRetiradaChange: (String) -> Unit = {},
    onTipoDocumentoResponsavelRetiradaChange: (String) -> Unit = {},
    onClickLimparTipoDocumentoResponsavelRetirada: () -> Unit = {},
    onDocumentoResponsavelRetiradaChange: (String) -> Unit = {},
    onTipoVeiculoChange: (String) -> Unit = {},
    onModeloVeiculoChange: (String) -> Unit = {},
    onPlacaVeiculoChange: (String) -> Unit = {},
    onCorVeiculoChange: (String) -> Unit = {},
    onCilindradaChange: (String) -> Unit = {},
    // Eventos do sub-form de passageiro (molde ADR-0006, §1b) — threadados até o ContentPassageiroAreaForm.
    onAcomodacaoChange: (String) -> Unit = {},
    onTipoPassagemChange: (String) -> Unit = {},
    onTipoGratuidadeChange: (String) -> Unit = {},
    onNomePassageiro1Change: (String) -> Unit = {},
    onTipoDocumentoPassageiro1Change: (String) -> Unit = {},
    onClickLimparDocumentoPassageiro1: () -> Unit = {},
    onDocumentoPassageiro1Change: (String) -> Unit = {},
    onDataNascimentoPassageiro1Change: (String) -> Unit = {},
    onCheckPassageiro2: (Boolean) -> Unit = {},
    onNomePassageiro2Change: (String) -> Unit = {},
    onTipoDocumentoPassageiro2Change: (String) -> Unit = {},
    onClickLimparDocumentoPassageiro2: () -> Unit = {},
    onDocumentoPassageiro2Change: (String) -> Unit = {},
    onDataNascimentoPassageiro2Change: (String) -> Unit = {},
    onCheckPassageiro3: (Boolean) -> Unit = {},
    onNomePassageiro3Change: (String) -> Unit = {},
    onTipoDocumentoPassageiro3Change: (String) -> Unit = {},
    onClickLimparDocumentoPassageiro3: () -> Unit = {},
    onDocumentoPassageiro3Change: (String) -> Unit = {},
    onDataNascimentoPassageiro3Change: (String) -> Unit = {},
    // Eventos dos dados da passagem (molde ADR-0006, §1b) — top-level e área de pagamento.
    onCheckVeiculo: (Boolean) -> Unit = {},
    onDataViagemChange: (String) -> Unit = {},
    onHoraViagemChange: (String) -> Unit = {},
    onCheckPix: (Boolean) -> Unit = {},
    onCheckDinheiro: (Boolean) -> Unit = {},
    onCheckDebito: (Boolean) -> Unit = {},
    onCheckCredito: (Boolean) -> Unit = {},
    onValorPixChange: (String) -> Unit = {},
    onValorDinheiroChange: (String) -> Unit = {},
    onValorDebitoChange: (String) -> Unit = {},
    onValorCreditoChange: (String) -> Unit = {},
    onObservacaoChange: (String, Boolean) -> Unit = { _, _ -> },
    focusManager: FocusManager = LocalFocusManager.current,
    // Nonce: incrementado a cada "Avançar" com validação inválida → rola até o 1º erro (mais acima).
    scrollParaErro: Int = 0,
    onClickVoltar: () -> Unit = {},
    onClickAvancar: () -> Unit = {},
) {
    CommonScreenNoBottom(
        titleTopAppBar = R.string.title_top_passagem,
        titleTopContent = statePassagem.titleForm,
        isShowRightIcon = false,
        hasRefresh = false,
        isRefreshing = false,
        onClickVoltar = onClickVoltar,
    ) { modifier, titulo ->
        val scrollState = rememberScrollState()
        // Âncoras de validação, na ordem vertical do form (topo → base).
        val ancoraData = remember { BringIntoViewRequester() }
        val ancoraHora = remember { BringIntoViewRequester() }
        val ancoraAreaPassageiro = remember { BringIntoViewRequester() }
        val ancoraPagamento = remember { BringIntoViewRequester() }
        val ancoraBloqueio = remember { BringIntoViewRequester() }

        // Ao falhar a validação (nonce muda), rola até a primeira âncora com erro — a mais acima.
        LaunchedEffect(scrollParaErro) {
            if (scrollParaErro == 0) return@LaunchedEffect
            val erroPassageiroOuVeiculo = if (statePassagem.isVeiculoChecked) {
                with(stateVeiculo) {
                    isTipoVeiculoError || isModeloVeiculoError || isPlacaVeiculoError || isCorVeiculoError ||
                        isNomeResponsavelRetiradaError || isDocumentoResponsavelRetiradaError ||
                        isTipoDocumentoResponsavelRetiradaError
                }
            } else {
                with(statePassageiro) {
                    isTipoPassagemError || isAcomodacaoError || isTipoGratuidadeError ||
                        isTipoDocumentoPassageiro1Error || isDocumentoPassageiro1Error ||
                        isNomePassageiro1Error || isDataNascimentoPassageiro1Error ||
                        isTipoDocumentoPassageiro2Error || isDocumentoPassageiro2Error ||
                        isNomePassageiro2Error || isDataNascimentoPassageiro2Error ||
                        isTipoDocumentoPassageiro3Error || isDocumentoPassageiro3Error ||
                        isNomePassageiro3Error || isDataNascimentoPassageiro3Error
                }
            }
            val erroPagamento = with(statePassagem) {
                isFormaPagamentoError || isValorPixError ||
                    isValorDinheiroError || isValorDebitoError || isValorCreditoError
            }
            when {
                // Bloqueio de emissão (fail-closed) tem prioridade: rola até o banner com a causa.
                statePassagem.emissaoBloqueadaMsg != 0 -> ancoraBloqueio.bringIntoView()
                statePassagem.isDataViagemError -> ancoraData.bringIntoView()
                statePassagem.isHoraViagemError -> ancoraHora.bringIntoView()
                erroPassageiroOuVeiculo -> ancoraAreaPassageiro.bringIntoView()
                erroPagamento -> ancoraPagamento.bringIntoView()
            }
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CommonTopRow(modifier = modifier, titulo = titulo)

            ViagemCard(modifier, statePassagem)

            if (!statePassagem.isLoading) {
                FormFieldCalendario(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(10.dp, 0.dp)
                        .bringIntoViewRequester(ancoraData),
                    value = statePassagem.dataViagem,
                    onValueChange = onDataViagemChange,
                    label = R.string.label_data_viagem,
                    isError = statePassagem.isDataViagemError,
                    textoErro = statePassagem.textDataViagemError,
                    focusManager = focusManager
                )

                FormFieldRelogio(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(10.dp, 0.dp)
                        .bringIntoViewRequester(ancoraHora),
                    focusManager = focusManager,
                    label = R.string.label_hora_viagem,
                    value = statePassagem.horaViagem,
                    onValueChange = onHoraViagemChange,
                    isError = statePassagem.isHoraViagemError,
                )

                CommonCheckboxField(
                    modifier = modifier,
                    label = R.string.label_checkbox_veiculo,
                    checked = statePassagem.isVeiculoChecked,
                    onCheck = onCheckVeiculo
                )

                if (statePassagem.isVeiculoChecked) {
                    CommonAreaForm(
                        modifier = modifier.bringIntoViewRequester(ancoraAreaPassageiro),
                        titleArea = R.string.form_area_title_veiculo
                    ) {
                        ContentAreaVeiculoForm(
                            modifier = it,
                            statePassagem = statePassagem,
                            stateVeiculo = stateVeiculo,
                            onNomeResponsavelRetiradaChange = onNomeResponsavelRetiradaChange,
                            onTipoDocumentoResponsavelRetiradaChange = onTipoDocumentoResponsavelRetiradaChange,
                            onClickLimparTipoDocumentoResponsavelRetirada = onClickLimparTipoDocumentoResponsavelRetirada,
                            onDocumentoResponsavelRetiradaChange = onDocumentoResponsavelRetiradaChange,
                            onTipoVeiculoChange = onTipoVeiculoChange,
                            onModeloVeiculoChange = onModeloVeiculoChange,
                            onPlacaVeiculoChange = onPlacaVeiculoChange,
                            onCorVeiculoChange = onCorVeiculoChange,
                            onCilindradaChange = onCilindradaChange,
                        )
                    }
                } else {
                    CommonAreaForm(
                        modifier = modifier.bringIntoViewRequester(ancoraAreaPassageiro),
                        titleArea = R.string.form_area_title_passageiro
                    ) {
                        ContentPassageiroAreaForm(
                            modifier = it,
                            statePassagem = statePassagem,
                            statePassageiro = statePassageiro,
                            onAcomodacaoChange = onAcomodacaoChange,
                            onTipoPassagemChange = onTipoPassagemChange,
                            onTipoGratuidadeChange = onTipoGratuidadeChange,
                            onNomePassageiro1Change = onNomePassageiro1Change,
                            onTipoDocumentoPassageiro1Change = onTipoDocumentoPassageiro1Change,
                            onClickLimparDocumentoPassageiro1 = onClickLimparDocumentoPassageiro1,
                            onDocumentoPassageiro1Change = onDocumentoPassageiro1Change,
                            onDataNascimentoPassageiro1Change = onDataNascimentoPassageiro1Change,
                            onCheckPassageiro2 = onCheckPassageiro2,
                            onNomePassageiro2Change = onNomePassageiro2Change,
                            onTipoDocumentoPassageiro2Change = onTipoDocumentoPassageiro2Change,
                            onClickLimparDocumentoPassageiro2 = onClickLimparDocumentoPassageiro2,
                            onDocumentoPassageiro2Change = onDocumentoPassageiro2Change,
                            onDataNascimentoPassageiro2Change = onDataNascimentoPassageiro2Change,
                            onCheckPassageiro3 = onCheckPassageiro3,
                            onNomePassageiro3Change = onNomePassageiro3Change,
                            onTipoDocumentoPassageiro3Change = onTipoDocumentoPassageiro3Change,
                            onClickLimparDocumentoPassageiro3 = onClickLimparDocumentoPassageiro3,
                            onDocumentoPassageiro3Change = onDocumentoPassageiro3Change,
                            onDataNascimentoPassageiro3Change = onDataNascimentoPassageiro3Change,
                            focusManager = focusManager
                        )
                    }
                }


                CommonAreaForm(
                    modifier = modifier.bringIntoViewRequester(ancoraPagamento),
                    titleArea = R.string.form_area_title_pagamento
                ) {
                    ContentPagamentoAreaForm(
                        modifier = it,
                        state = statePassagem,
                        statePassageiro = statePassageiro,
                        stateVeiculo = stateVeiculo,
                        onCheckPix = onCheckPix,
                        onCheckDinheiro = onCheckDinheiro,
                        onCheckDebito = onCheckDebito,
                        onCheckCredito = onCheckCredito,
                        onValorPixChange = onValorPixChange,
                        onValorDinheiroChange = onValorDinheiroChange,
                        onValorDebitoChange = onValorDebitoChange,
                        onValorCreditoChange = onValorCreditoChange,
                        onObservacaoChange = onObservacaoChange,
                        focusManager = focusManager
                    )
                }

                if (statePassagem.emissaoBloqueadaMsg != 0) {
                    AvisoBloqueioEmissao(
                        modifier = modifier.bringIntoViewRequester(ancoraBloqueio),
                        texto = if (statePassagem.emissaoBloqueadaArg.isBlank()) {
                            stringResource(statePassagem.emissaoBloqueadaMsg)
                        } else {
                            stringResource(statePassagem.emissaoBloqueadaMsg, statePassagem.emissaoBloqueadaArg)
                        }
                    )
                }

                Column(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CommonIconButton(
                        modifier = modifier,
                        onClick = { onClickAvancar() },
                        text = stringResource(id = R.string.btn_avancar),
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = stringResource(id = R.string.btn_avancar)
                            )
                        },
                        color = MaterialTheme.colorScheme.primary,
                        isProcessing = statePassagem.isSaving
                    )
                }
            } else {
                Column(
                    modifier = modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = modifier.padding(10.dp))
                    TextSubTitleBrownItalic(text = stringResource(id = R.string.msg_carreg_info))
                }
            }
        }
    }
}

/** Banner persistente do bloqueio de emissão fail-closed (ADR-0013): aponta a causa; substitui o toast. */
@Composable
private fun AvisoBloqueioEmissao(
    modifier: Modifier,
    texto: String,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(10.dp, 0.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = texto,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NovaPassagemFormScreenPreview() {
    FormPassagemScreen(
        statePassagem = FormPassagemUiState(
            navioViagem = "F/B Regional",
            horaViagem = "18:00",
            origemViagem = "Belém - Santana",
            codigoViagem = "BEL-SAN-101",
            listaFormaPagamento = listaFormaPagamentoSample
        ),
        statePassageiro = FormPassageiroUiState(
            listaAcomodacao = listaAcomodacaoSample,
            acomodacao = listaAcomodacaoSample[1].descricaoNome,
            isPassageiro2Checked = true
        ),
        stateVeiculo = FormVeiculoUiState()
    )
}

@Preview(showBackground = true)
@Composable
private fun NovaPassagemFormScreenLoadingPreview() {
    FormPassagemScreen(
        statePassagem = FormPassagemUiState(
            navioViagem = "F/B Regional",
            horaViagem = "18:00",
            origemViagem = "Belém - Santana",
            codigoViagem = "BEL-SAN-101",
            listaFormaPagamento = listaFormaPagamentoSample,
            isLoading = true
        ),
        statePassageiro = FormPassageiroUiState(
            listaAcomodacao = listaAcomodacaoSample,
            acomodacao = listaAcomodacaoSample[1].descricaoNome,
            isPassageiro2Checked = true
        ),
        stateVeiculo = FormVeiculoUiState()
    )
}