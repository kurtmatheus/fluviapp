package dev.matheus.fluviapp.ui.screens.forms.passagem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
                isFormaPagamentoError || isValorPagoError || isValorPixError ||
                    isValorDinheiroError || isValorDebitoError || isValorCreditoError
            }
            when {
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
                    onValueChange = statePassagem.onDataViagemChange,
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
                    onValueChange = statePassagem.onHoraViagemChange,
                    isError = statePassagem.isHoraViagemError,
                )

                CommonCheckboxField(
                    modifier = modifier,
                    label = R.string.label_checkbox_veiculo,
                    checked = statePassagem.isVeiculoChecked,
                    onCheck = statePassagem.onCheckVeiculo
                )

                if (statePassagem.isVeiculoChecked) {
                    CommonAreaForm(
                        modifier = modifier.bringIntoViewRequester(ancoraAreaPassageiro),
                        titleArea = R.string.form_area_title_veiculo
                    ) {
                        ContentAreaVeiculoForm(
                            modifier = it,
                            statePassagem = statePassagem,
                            stateVeiculo = stateVeiculo
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
                            focusManager = focusManager
                        )
                    }
                }

//                CommonAreaForm(
//                    modifier = modifier,
//                    titleArea = R.string.form_area_title_agencia
//                ) {
//                    ContentAgenciaAreaPassagemForm(
//                        modifier = it,
//                        state = statePassagem
//                    )
//                }

                CommonAreaForm(
                    modifier = modifier.bringIntoViewRequester(ancoraPagamento),
                    titleArea = R.string.form_area_title_pagamento
                ) {
                    ContentPagamentoAreaForm(
                        modifier = it,
                        state = statePassagem,
                        statePassageiro = statePassageiro,
                        stateVeiculo = stateVeiculo,
                        focusManager = focusManager
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