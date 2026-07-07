package br.com.gruponaveg.ui.components.forms.areas.passagem

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.gruponaveg.R
import br.com.gruponaveg.model.cadastro.constantes.Constante.Descricao.CREDITO
import br.com.gruponaveg.model.cadastro.constantes.Constante.Descricao.DEBITO
import br.com.gruponaveg.model.cadastro.constantes.Constante.Descricao.DINHEIRO
import br.com.gruponaveg.model.cadastro.constantes.Constante.Descricao.GRATUIDADE
import br.com.gruponaveg.model.cadastro.constantes.Constante.Descricao.MEIA
import br.com.gruponaveg.model.cadastro.constantes.Constante.Descricao.PIX
import br.com.gruponaveg.model.cadastro.constantes.Constante.Descricao.REDE
import br.com.gruponaveg.model.cadastro.passagem.Agente.Agencia.NAVEG
import br.com.gruponaveg.model.cadastro.passagem.Agente.Nome.ODAIR
import br.com.gruponaveg.model.passagem.Passagem
import br.com.gruponaveg.sampledata.listaFormaPagamentoSample
import br.com.gruponaveg.ui.components.cards.CommonCard
import br.com.gruponaveg.ui.components.forms.areas.CommonAreaForm
import br.com.gruponaveg.ui.components.forms.buttons.CommonCheckboxField
import br.com.gruponaveg.ui.components.forms.fields.FormTextFieldBrownLeadingIcon
import br.com.gruponaveg.ui.components.forms.fields.FormTextFieldBrownTrailingIcon
import br.com.gruponaveg.ui.components.texts.SupportingText
import br.com.gruponaveg.ui.components.texts.TextBoldBrownItalic
import br.com.gruponaveg.ui.components.texts.TextRegularBrown
import br.com.gruponaveg.ui.states.passagem.FormPassageiroUiState
import br.com.gruponaveg.ui.states.passagem.FormPassagemUiState
import br.com.gruponaveg.ui.theme.Yellow

@Composable
fun ContentPagamentoAreaForm(
    modifier: Modifier,
    state: FormPassagemUiState,
    statePassageiro: FormPassageiroUiState,
    focusManager: FocusManager = LocalFocusManager.current,
) {

    if (statePassageiro.ehAcomodacaoRede) {
        CardValor(
            modifier = modifier,
            state = statePassageiro
        )
    }

    if (state.isFormaPagamentoEnabled && !statePassageiro.isGratuidade) {
        AreaFormaPagamento(state, modifier)

        FieldFormaPagamento(
            modifier = modifier.fillMaxWidth(),
            focusManager = focusManager,
            label = R.string.label_valor_pix,
            value = state.valorPix,
            onValueChange = state.onValorPixChange,
            isError = state.isValorPixError,
            isChecked = state.isPixChecked
        )

        FieldFormaPagamento(
            modifier = modifier.fillMaxWidth(),
            focusManager = focusManager,
            label = R.string.label_valor_dinheiro,
            value = state.valorDinheiro,
            onValueChange = state.onValorDinheiroChange,
            isError = state.isValorDinheiroError,
            isChecked = state.isDinheiroChecked
        )

        FieldFormaPagamento(
            modifier = modifier.fillMaxWidth(),
            focusManager = focusManager,
            label = R.string.label_valor_debito,
            value = state.valorDebito,
            onValueChange = state.onValorDebitoChange,
            isError = state.isValorDebitoError,
            isChecked = state.isDebitoChecked
        )

        FieldFormaPagamento(
            modifier = modifier.fillMaxWidth(),
            focusManager = focusManager,
            label = R.string.label_valor_credito,
            value = state.valorCredito,
            onValueChange = state.onValorCreditoChange,
            isError = state.isValorCreditoError,
            isChecked = state.isCreditoChecked
        )

    } else {
        FormTextFieldBrownLeadingIcon(
            modifier = modifier.fillMaxWidth(),
            value = state.valorPago,
            label = R.string.label_valor_pago,
            onValueChange = state.onValorPagoChange,
            isError = state.isValorPagoError,
            enabled = state.isValorPagoEnabled,
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_cifrao_24),
                    contentDescription = stringResource(id = R.string.description_valor),
                    tint = if (state.isValorPagoError) Color.Red else MaterialTheme.colorScheme.onBackground
                )
            },
            focusManager = focusManager,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Decimal
            )
        )
    }

//    FormTextFieldBrownLeadingIcon(
//        modifier = modifier.fillMaxWidth(),
//        value = state.desconto,
//        label = R.string.label_desconto,
//        onValueChange = state.onDescontoChange,
//        enabled = state.isDescontoEnabled,
//        leadingIcon = {
//            Icon(
//                painter = painterResource(id = R.drawable.ic_cifrao_24),
//                contentDescription = stringResource(id = R.string.description_valor),
//            )
//        },
//        focusManager = focusManager,
//        keyboardOptions = KeyboardOptions(
//            imeAction = ImeAction.Next,
//            keyboardType = KeyboardType.Decimal
//        )
//    )

    FormTextFieldBrownTrailingIcon(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(200.dp),
        value = state.observacao,
        label = R.string.label_obs,
        onValueChange = { state.onObservacaoChange(it, false) },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Next,
            capitalization = KeyboardCapitalization.Characters
        ),
        focusManager = focusManager,
        trailingIcon = {
            IconMicrofone(
                onValueChange = state.onObservacaoChange
            )
        }
    )
}

@Composable
private fun CardValor(
    modifier: Modifier,
    state: FormPassageiroUiState,
) {
    val tarifa = Passagem.TARIFA_ANTAC.formatarValoresMeia(state.isMeiaPassagem, state.isGratuidade)
    val desconto = Passagem.DESCONTO_ANTAC.formatarValoresMeia(state.isMeiaPassagem, state.isGratuidade)

    CommonCard(
        modifier = modifier.fillMaxWidth(),
        color = Yellow,
        onClick = {},
        enable = false,
        alturaCard = 75
    ) {
        Row(
            modifier = modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CardValorComponent(
                modifier = modifier,
                label = R.string.label_tarifa,
                valor = tarifa
            )
            CardValorComponent(
                modifier = modifier,
                label = R.string.label_desconto,
                valor = desconto
            )
            CardValorComponent(
                modifier = modifier,
                label = R.string.label_valor_total,
                valor = (tarifa.toInt() - desconto.toInt()).toString()
            )
        }
    }
}

private fun String.formatarValoresMeia(isMeia: Boolean, isGratuidade: Boolean): String {
    return if (isMeia) {
        toInt().div(2).toString()
    } else if (isGratuidade) {
        "0"
    } else {
        this
    }
}

@Composable
private fun CardValorComponent(
    modifier: Modifier,
    label: Int,
    valor: String,
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextBoldBrownItalic(text = stringResource(label))
        Spacer(modifier = modifier.padding(3.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = modifier.size(20.dp),
                imageVector = Icons.Outlined.AttachMoney,
                contentDescription = stringResource(id = R.string.description_valor)
            )
            TextRegularBrown(text = valor)
        }
    }
}

@Composable
fun IconMicrofone(
    onValueChange: (String, Boolean) -> Unit = { _, _ -> },
) {

    var isListening by remember { mutableStateOf(false) }

    val activityResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)

            if (!results.isNullOrEmpty()) {
                onValueChange(results[0], true)
            }
        }

        isListening = false
    }

    IconButton(onClick = {
        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-Br")
        speechIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale...")

        activityResultLauncher.launch(speechIntent)
        isListening = true
    }) {
        Icon(
            painter = painterResource(id = R.drawable.ic_microfone_24),
            contentDescription = stringResource(id = R.string.description_mic),
            tint = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun AreaFormaPagamento(
    state: FormPassagemUiState,
    modifier: Modifier,
) {
    Row {
        state.listaFormaPagamento.forEach {
            when (it.descricaoNome) {
                PIX.name -> {
                    CommonCheckboxField(
                        modifier = modifier,
                        label = R.string.label_pix,
                        checked = state.isPixChecked,
                        onCheck = state.onCheckPix,
                        isError = state.isFormaPagamentoError
                    )
                }

                DINHEIRO.name -> {
                    CommonCheckboxField(
                        modifier = modifier,
                        label = R.string.label_dinheiro,
                        checked = state.isDinheiroChecked,
                        onCheck = state.onCheckDinheiro,
                        isError = state.isFormaPagamentoError
                    )
                }

                DEBITO.name -> {
                    CommonCheckboxField(
                        modifier = modifier,
                        label = R.string.label_debito,
                        checked = state.isDebitoChecked,
                        onCheck = state.onCheckDebito,
                        isError = state.isFormaPagamentoError
                    )
                }

                CREDITO.name -> {
                    CommonCheckboxField(
                        modifier = modifier,
                        label = R.string.label_credito,
                        checked = state.isCreditoChecked,
                        onCheck = state.onCheckCredito,
                        isError = state.isFormaPagamentoError
                    )
                }
            }
        }
    }

    if (state.isFormaPagamentoError) {
        SupportingText(
            modifier = modifier.padding(10.dp, 0.dp),
            text = stringResource(id = R.string.error_camp_obrig)
        )
    }
}

@Composable
fun FieldFormaPagamento(
    modifier: Modifier,
    focusManager: FocusManager,
    label: Int,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    isChecked: Boolean,
) {
    if (isChecked) {
        FormTextFieldBrownLeadingIcon(
            modifier = modifier,
            value = value,
            label = label,
            onValueChange = onValueChange,
            isError = isError,
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_cifrao_24),
                    contentDescription = stringResource(id = R.string.description_valor),
                    tint = if (isError) Color.Red else MaterialTheme.colorScheme.onBackground
                )
            },
            focusManager = focusManager,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Decimal
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ContentViagemAreaFormPreview() {
    CommonAreaForm(
        modifier = Modifier,
        titleArea = R.string.form_area_title_pagamento
    ) {
        ContentPagamentoAreaForm(
            modifier = it,
            state = FormPassagemUiState(),
            statePassageiro = FormPassageiroUiState(acomodacao = REDE.name)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ContentViagemAreaFormMeiaPassagemPreview() {
    CommonAreaForm(
        modifier = Modifier,
        titleArea = R.string.form_area_title_pagamento
    ) {
        ContentPagamentoAreaForm(
            modifier = it,
            state = FormPassagemUiState(),
            statePassageiro = FormPassageiroUiState(
                acomodacao = REDE.name,
                tipoPassagem = MEIA.name
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ContentViagemAreaFormAgenciaNavegPreview() {
    CommonAreaForm(
        modifier = Modifier,
        titleArea = R.string.form_area_title_pagamento
    ) {
        ContentPagamentoAreaForm(
            modifier = it,
            state = FormPassagemUiState(
                agencia = NAVEG.name,
                agente = ODAIR.name,
                listaFormaPagamento = listaFormaPagamentoSample,
                isPixChecked = true,
                isDinheiroChecked = true
            ),
            statePassageiro = FormPassageiroUiState()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ContentViagemAreaFormAgenciaNavegGratuidadePreview() {
    CommonAreaForm(
        modifier = Modifier,
        titleArea = R.string.form_area_title_pagamento
    ) {
        ContentPagamentoAreaForm(
            modifier = it,
            state = FormPassagemUiState(
                agencia = NAVEG.name,
                agente = ODAIR.name,
                valorPago = "0",
                isValorPagoEnabled = false,
            ),
            statePassageiro = FormPassageiroUiState(
                acomodacao = REDE.name,
                tipoPassagem = GRATUIDADE.name
            )
        )
    }
}
