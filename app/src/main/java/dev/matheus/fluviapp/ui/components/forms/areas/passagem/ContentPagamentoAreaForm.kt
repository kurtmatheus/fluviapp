package dev.matheus.fluviapp.ui.components.forms.areas.passagem

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
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.extensions.formataParaMoedaBrasileira
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Descricao.CREDITO
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Descricao.DEBITO
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Descricao.DINHEIRO
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Descricao.GRATUIDADE
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Descricao.MEIA
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Descricao.MOTO
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Descricao.PIX
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Descricao.REDE
import dev.matheus.fluviapp.domain.passagem.TipoPassagem
import dev.matheus.fluviapp.domain.passagem.tarifaMotoBase
import dev.matheus.fluviapp.domain.passagem.FormaPagamento
import dev.matheus.fluviapp.ui.components.cards.CommonCard
import dev.matheus.fluviapp.ui.components.forms.areas.CommonAreaForm
import dev.matheus.fluviapp.ui.components.forms.buttons.CommonCheckboxField
import dev.matheus.fluviapp.ui.components.forms.fields.FormTextFieldBrownLeadingIcon
import dev.matheus.fluviapp.ui.components.forms.fields.FormTextFieldBrownTrailingIcon
import dev.matheus.fluviapp.ui.components.texts.SupportingText
import dev.matheus.fluviapp.ui.components.texts.TextBoldBrownItalic
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrown
import dev.matheus.fluviapp.ui.states.passagem.FormPassageiroUiState
import dev.matheus.fluviapp.ui.states.passagem.FormPassagemUiState
import dev.matheus.fluviapp.ui.states.passagem.FormVeiculoUiState
import dev.matheus.fluviapp.ui.theme.Yellow
import java.math.BigDecimal

@Composable
fun ContentPagamentoAreaForm(
    modifier: Modifier,
    state: FormPassagemUiState,
    statePassageiro: FormPassageiroUiState,
    stateVeiculo: FormVeiculoUiState = FormVeiculoUiState(),
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
) {

    // Preview do valor tabelado (ADR-0013): a tarifa da célula da chave escolhida na tabela da viagem —
    // acomodação (passageiro) ou classe (veículo); moto pela regra da cilindrada. Sem tarifa → sem card.
    val tarifaBase: Double? = if (state.isVeiculoChecked) {
        if (stateVeiculo.tipoVeiculo == MOTO.name) {
            stateVeiculo.cilindrada.toIntOrNull()?.let { tarifaMotoBase(it).toDouble() }
        } else {
            state.tarifasViagem[stateVeiculo.tipoVeiculo]
        }
    } else {
        state.tarifasViagem[statePassageiro.acomodacao]
    }
    if (tarifaBase != null) {
        CardValor(
            modifier = modifier,
            tarifaBase = tarifaBase,
            // Veículo é sempre inteira (sem meia/gratuidade); passageiro segue o tipo escolhido.
            tipo = if (state.isVeiculoChecked) TipoPassagem.INTEIRA
            else TipoPassagem.de(statePassageiro.tipoPassagem) ?: TipoPassagem.INTEIRA,
        )
    }

    // Gratuidade não paga: a área de pagamento simplesmente não aparece. Não há mais o gate de capability
    // nem o campo "valor pago" avulso que ficava no lugar dela (ADR-0015 §4a).
    if (!statePassageiro.isGratuidade) {
        AreaFormaPagamento(
            state = state,
            modifier = modifier,
            onCheckPix = onCheckPix,
            onCheckDinheiro = onCheckDinheiro,
            onCheckDebito = onCheckDebito,
            onCheckCredito = onCheckCredito,
        )

        FieldFormaPagamento(
            modifier = modifier.fillMaxWidth(),
            focusManager = focusManager,
            label = R.string.label_valor_pix,
            value = state.valorPix,
            onValueChange = onValorPixChange,
            isError = state.isValorPixError,
            isChecked = state.isPixChecked
        )

        FieldFormaPagamento(
            modifier = modifier.fillMaxWidth(),
            focusManager = focusManager,
            label = R.string.label_valor_dinheiro,
            value = state.valorDinheiro,
            onValueChange = onValorDinheiroChange,
            isError = state.isValorDinheiroError,
            isChecked = state.isDinheiroChecked
        )

        FieldFormaPagamento(
            modifier = modifier.fillMaxWidth(),
            focusManager = focusManager,
            label = R.string.label_valor_debito,
            value = state.valorDebito,
            onValueChange = onValorDebitoChange,
            isError = state.isValorDebitoError,
            isChecked = state.isDebitoChecked
        )

        FieldFormaPagamento(
            modifier = modifier.fillMaxWidth(),
            focusManager = focusManager,
            label = R.string.label_valor_credito,
            value = state.valorCredito,
            onValueChange = onValorCreditoChange,
            isError = state.isValorCreditoError,
            isChecked = state.isCreditoChecked
        )
    }

    // Campo de desconto manual removido (ADR-0013): o desconto é DERIVADO (tarifa devida − valor cobrado),
    // não digitado. O operador informa só o que cobrou; o desconto emerge no detalhe/impressão/balanço.

    FormTextFieldBrownTrailingIcon(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(200.dp),
        value = state.observacao,
        label = R.string.label_obs,
        onValueChange = { onObservacaoChange(it, false) },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Next,
            capitalization = KeyboardCapitalization.Characters
        ),
        focusManager = focusManager,
        trailingIcon = {
            IconMicrofone(
                onValueChange = onObservacaoChange
            )
        }
    )
}

@Composable
private fun CardValor(
    modifier: Modifier,
    tarifaBase: Double,
    tipo: TipoPassagem,
) {
    // Tarifa da inteira (base) e o valor devido da categoria (meia = metade, gratuidade = 0), da tabela
    // real da viagem (ADR-0013). O desconto não entra aqui — só se conhece após o valor cobrado.
    val base = BigDecimal.valueOf(tarifaBase)
    val devida = tipo.tarifaDevida(base)

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
                valor = base.formataParaMoedaBrasileira()
            )
            CardValorComponent(
                modifier = modifier,
                label = R.string.label_valor_total,
                valor = devida.formataParaMoedaBrasileira()
            )
        }
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
    onCheckPix: (Boolean) -> Unit = {},
    onCheckDinheiro: (Boolean) -> Unit = {},
    onCheckDebito: (Boolean) -> Unit = {},
    onCheckCredito: (Boolean) -> Unit = {},
) {
    Row {
        state.listaFormaPagamento.forEach {
            when (it) {
                PIX.name -> {
                    CommonCheckboxField(
                        modifier = modifier,
                        label = R.string.label_pix,
                        checked = state.isPixChecked,
                        onCheck = onCheckPix,
                        isError = state.isFormaPagamentoError
                    )
                }

                DINHEIRO.name -> {
                    CommonCheckboxField(
                        modifier = modifier,
                        label = R.string.label_dinheiro,
                        checked = state.isDinheiroChecked,
                        onCheck = onCheckDinheiro,
                        isError = state.isFormaPagamentoError
                    )
                }

                DEBITO.name -> {
                    CommonCheckboxField(
                        modifier = modifier,
                        label = R.string.label_debito,
                        checked = state.isDebitoChecked,
                        onCheck = onCheckDebito,
                        isError = state.isFormaPagamentoError
                    )
                }

                CREDITO.name -> {
                    CommonCheckboxField(
                        modifier = modifier,
                        label = R.string.label_credito,
                        checked = state.isCreditoChecked,
                        onCheck = onCheckCredito,
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
private fun ContentViagemAreaFormAgenciaFluviPreview() {
    CommonAreaForm(
        modifier = Modifier,
        titleArea = R.string.form_area_title_pagamento
    ) {
        ContentPagamentoAreaForm(
            modifier = it,
            state = FormPassagemUiState(
                // O preview usa o próprio tipo: a lista deixou de vir do catálogo (ADR-0020 F2).
                listaFormaPagamento = FormaPagamento.entries.map { it.name },
                isPixChecked = true,
                isDinheiroChecked = true
            ),
            statePassageiro = FormPassageiroUiState()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ContentViagemAreaFormAgenciaFluviGratuidadePreview() {
    CommonAreaForm(
        modifier = Modifier,
        titleArea = R.string.form_area_title_pagamento
    ) {
        ContentPagamentoAreaForm(
            modifier = it,
            state = FormPassagemUiState(
            ),
            statePassageiro = FormPassageiroUiState(
                acomodacao = REDE.name,
                tipoPassagem = GRATUIDADE.name
            )
        )
    }
}
