package dev.matheus.fluviapp.ui.screens.forms.rota

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.components.contents.CommonTopRow
import dev.matheus.fluviapp.ui.components.dialogs.CommonInformativeDialog
import dev.matheus.fluviapp.ui.components.forms.divider.FormDashedDivider
import dev.matheus.fluviapp.ui.components.forms.fields.FormTextFieldBrownNoIcon
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrown
import dev.matheus.fluviapp.ui.components.texts.TextSubTitleBrownItalic
import dev.matheus.fluviapp.ui.components.texts.TextTitleBrownRegular
import dev.matheus.fluviapp.ui.screens.forms.CommonScreenNoBottom
import dev.matheus.fluviapp.ui.states.PesquisaRotaUiState
import dev.matheus.fluviapp.ui.states.RotaResultado
import dev.matheus.fluviapp.ui.theme.FluviAppTheme

@Composable
fun ResultSearchRotaScreen(
    uiState: PesquisaRotaUiState,
    onPortoChange: (String) -> Unit = {},
    onClickVoltar: () -> Unit = {},
    onInativar: (String) -> Unit = {},
) {
    CommonScreenNoBottom(
        titleTopAppBar = R.string.title_top_rotas,
        titleTopContent = R.string.subtitle_pesquisar_rotas,
        isShowRightIcon = false,
        hasRefresh = false,
        isRefreshing = false,
        onClickVoltar = onClickVoltar,
    ) { modifier, titulo ->
        var rotaParaInativar by remember { mutableStateOf<RotaResultado?>(null) }

        Column {
            CommonTopRow(modifier = modifier, titulo = titulo)

            Column(
                modifier = modifier.padding(10.dp, 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Um campo só, casando contra os dois portos: "o que liga daqui" inclui chegar aqui.
                FormTextFieldBrownNoIcon(
                    modifier = modifier.fillMaxWidth(),
                    value = uiState.porto,
                    label = R.string.label_porto,
                    onValueChange = onPortoChange,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Search,
                    ),
                )
            }
            FormDashedDivider(modifier = modifier.fillMaxWidth())

            LazyColumn {
                items(uiState.resultados) { rota ->
                    CardResultRota(
                        modifier = modifier,
                        rota = rota,
                        podeInativar = uiState.podeInativar,
                        onInativar = { rotaParaInativar = it },
                    )
                }
            }
        }

        rotaParaInativar?.let { rota ->
            CommonInformativeDialog(
                modifier = Modifier,
                // A mensagem diz o que inativar significa: some dos seletores, continua valendo para o
                // que já aponta. Sem isso, "inativar" seria lido como "apagar".
                textMensagem = R.string.msg_confirmar_inativar_rota,
                textConfirm = R.string.btn_inativar,
                textDismiss = R.string.btn_cancelar,
                onConfirm = {
                    onInativar(rota.id)
                    rotaParaInativar = null
                },
                onDismiss = { rotaParaInativar = null },
            )
        }
    }
}

@Composable
fun CardResultRota(
    modifier: Modifier,
    rota: RotaResultado,
    podeInativar: Boolean,
    onInativar: (RotaResultado) -> Unit,
) {
    Column {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(10.dp, 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                TextTitleBrownRegular(text = "${rota.origem} → ${rota.destino}")
                TextRegularBrown(text = rota.medidas)
                // A inativa **fica na lista**, marcada: o descartado é registro, e some-la esconderia
                // por que um bilhete antigo aponta para onde aponta.
                if (!rota.ativa) {
                    TextSubTitleBrownItalic(text = stringResource(R.string.msg_rota_inativa))
                }
            }

            // Inativar é ato de plataforma (ADR-0022 D3): para quem não é, o botão não existe.
            if (podeInativar && rota.ativa) {
                IconButton(onClick = { onInativar(rota) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.btn_inativar),
                    )
                }
            }
        }
        HorizontalDivider(modifier = Modifier)
    }
}

@Preview
@Composable
private fun ResultSearchRotaScreenPreview() {
    FluviAppTheme {
        ResultSearchRotaScreen(
            uiState = PesquisaRotaUiState(
                podeInativar = true,
                resultados = listOf(
                    RotaResultado(
                        id = "1",
                        origem = "Porto de Val-de-Cães · Belém/PA",
                        destino = "Porto de Parintins · Parintins/AM",
                        medidas = "420.5 mn · 30.0 h",
                        ativa = true,
                    ),
                    RotaResultado(
                        id = "2",
                        origem = "Porto de Parintins · Parintins/AM",
                        destino = "Porto de Val-de-Cães · Belém/PA",
                        medidas = "420.5 mn · 32.0 h",
                        ativa = false,
                    ),
                ),
            ),
        )
    }
}