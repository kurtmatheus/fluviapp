package br.com.gruponaveg.ui.screens.forms.passagem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.gruponaveg.R
import br.com.gruponaveg.sampledata.listaAcomodacaoSample
import br.com.gruponaveg.sampledata.listaFormaPagamentoSample
import br.com.gruponaveg.ui.components.cards.ViagemCard
import br.com.gruponaveg.ui.components.contents.CommonTopRow
import br.com.gruponaveg.ui.components.forms.areas.CommonAreaForm
import br.com.gruponaveg.ui.components.forms.areas.passagem.ContentAreaVeiculoForm
import br.com.gruponaveg.ui.components.forms.areas.passagem.ContentPagamentoAreaForm
import br.com.gruponaveg.ui.components.forms.areas.passagem.ContentPassageiroAreaForm
import br.com.gruponaveg.ui.components.forms.buttons.CommonCheckboxField
import br.com.gruponaveg.ui.components.forms.buttons.CommonIconButton
import br.com.gruponaveg.ui.components.forms.fields.FormFieldCalendario
import br.com.gruponaveg.ui.components.forms.fields.FormFieldRelogio
import br.com.gruponaveg.ui.components.texts.TextSubTitleBrownItalic
import br.com.gruponaveg.ui.screens.forms.CommonScreenNoBottom
import br.com.gruponaveg.ui.states.passagem.FormPassageiroUiState
import br.com.gruponaveg.ui.states.passagem.FormPassagemUiState
import br.com.gruponaveg.ui.states.passagem.FormVeiculoUiState

@Composable
fun FormPassagemScreen(
    statePassagem: FormPassagemUiState,
    statePassageiro: FormPassageiroUiState,
    stateVeiculo: FormVeiculoUiState,
    focusManager: FocusManager = LocalFocusManager.current,
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
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CommonTopRow(modifier = modifier, titulo = titulo)

            ViagemCard(modifier, statePassagem)

            if (!statePassagem.isLoading) {
                FormFieldCalendario(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(10.dp, 0.dp),
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
                        .padding(10.dp, 0.dp),
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
                        modifier = modifier,
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
                        modifier = modifier,
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
                    modifier = modifier,
                    titleArea = R.string.form_area_title_pagamento
                ) {
                    ContentPagamentoAreaForm(
                        modifier = it,
                        state = statePassagem,
                        statePassageiro = statePassageiro,
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