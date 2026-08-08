package dev.matheus.fluviapp.ui.screens.forms.rota

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.components.forms.areas.CommonAreaForm
import dev.matheus.fluviapp.ui.components.forms.buttons.CommonIconButton
import dev.matheus.fluviapp.ui.components.forms.dropdowns.DropDownFormField
import dev.matheus.fluviapp.ui.components.forms.fields.FormTextFieldBrownNoIcon
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrown
import dev.matheus.fluviapp.ui.screens.forms.CommonScreenNoBottom
import dev.matheus.fluviapp.ui.states.FormRotaUiState
import dev.matheus.fluviapp.ui.states.PortoOpcao

@Composable
fun FormRotaScreen(
    uiState: FormRotaUiState,
    onPortoOrigemChange: (String) -> Unit = {},
    onPortoDestinoChange: (String) -> Unit = {},
    onDistanciaChange: (String) -> Unit = {},
    onTempoChange: (String) -> Unit = {},
    onClickSalvar: () -> Unit = {},
    onClickVoltar: () -> Unit = {},
) {
    CommonScreenNoBottom(
        titleTopAppBar = R.string.title_top_rotas,
        titleTopContent = 0,
        isShowRightIcon = false,
        hasRefresh = false,
        isRefreshing = false,
        onClickVoltar = onClickVoltar,
    ) { modifier, _ ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CommonAreaForm(modifier = modifier, titleArea = R.string.subtitle_nova_rota) {
                // Os dois portos são um par: o erro é do conjunto, e por isso a mensagem aparece uma
                // vez — no destino, que é onde a escolha se completa.
                DropDownFormField(
                    modifier = it.fillMaxWidth(),
                    listaItens = uiState.portos.map { porto -> porto.rotulo },
                    label = R.string.label_porto_origem,
                    value = uiState.portoOrigem,
                    isError = uiState.erroPar.existe,
                    onValueChange = onPortoOrigemChange,
                )
                DropDownFormField(
                    modifier = it.fillMaxWidth(),
                    listaItens = uiState.portos.map { porto -> porto.rotulo },
                    label = R.string.label_porto_destino,
                    value = uiState.portoDestino,
                    isError = uiState.erroPar.existe,
                    onValueChange = onPortoDestinoChange,
                )
                if (uiState.erroPar.existe) {
                    TextRegularBrown(text = stringResource(uiState.erroPar.mensagem))
                }

                // Os dois fatos que justificam a Rota existir em vez de sair dos portos (§7.1).
                FormTextFieldBrownNoIcon(
                    modifier = it.fillMaxWidth(),
                    value = uiState.distanciaMn,
                    label = R.string.label_distancia_mn,
                    onValueChange = onDistanciaChange,
                    isError = uiState.isDistanciaError,
                    textoErro = R.string.error_medida_invalida,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                FormTextFieldBrownNoIcon(
                    modifier = it.fillMaxWidth(),
                    value = uiState.tempoMedioH,
                    label = R.string.label_tempo_medio_h,
                    onValueChange = onTempoChange,
                    isError = uiState.isTempoError,
                    textoErro = R.string.error_medida_invalida,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )

                // A tela diz que não há edição — quem procurar por ela precisa saber o que fazer no lugar.
                TextRegularBrown(text = stringResource(R.string.msg_rota_imutavel))
            }

            Column(
                modifier = modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CommonIconButton(
                    modifier = modifier,
                    onClick = onClickSalvar,
                    text = stringResource(id = R.string.btn_salvar),
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = stringResource(id = R.string.description_confirmacao),
                        )
                    },
                    color = MaterialTheme.colorScheme.primary,
                    isProcessing = uiState.isProcessing,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FormRotaScreenPreview() {
    FormRotaScreen(
        uiState = FormRotaUiState(
            portos = listOf(
                PortoOpcao("p1", "Porto de Val-de-Cães · Belém/PA"),
                PortoOpcao("p2", "Porto de Parintins · Parintins/AM"),
            ),
            portoOrigem = "Porto de Val-de-Cães · Belém/PA",
            portoDestino = "Porto de Parintins · Parintins/AM",
            distanciaMn = "420.5",
            tempoMedioH = "30",
        ),
    )
}