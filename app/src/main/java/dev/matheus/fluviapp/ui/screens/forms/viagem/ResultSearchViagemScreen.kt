package dev.matheus.fluviapp.ui.screens.forms.viagem

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
import dev.matheus.fluviapp.ui.states.PesquisaViagemUiState
import dev.matheus.fluviapp.ui.states.ViagemResultado
import dev.matheus.fluviapp.ui.theme.FluviAppTheme

@Composable
fun ResultSearchViagemScreen(
    uiState: PesquisaViagemUiState,
    onFiltroChange: (String) -> Unit = {},
    onClickVoltar: () -> Unit = {},
    onInativar: (String) -> Unit = {},
) {
    CommonScreenNoBottom(
        titleTopAppBar = R.string.title_top_viagens,
        titleTopContent = R.string.subtitle_pesquisar_viagens,
        isShowRightIcon = false,
        hasRefresh = false,
        isRefreshing = false,
        onClickVoltar = onClickVoltar,
    ) { modifier, titulo ->
        var viagemParaInativar by remember { mutableStateOf<ViagemResultado?>(null) }

        Column {
            CommonTopRow(modifier = modifier, titulo = titulo)

            if (uiState.semConcessao) {
                // O vazio que a plataforma resolve, distinto do vazio que o botão de criar resolve.
                Column(modifier = modifier.padding(10.dp, 10.dp)) {
                    TextRegularBrown(text = stringResource(R.string.msg_viagem_sem_concessao))
                }
            } else {
                Column(
                    modifier = modifier.padding(10.dp, 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Um campo só, casando contra a rota inteira e contra a embarcação: é como se
                    // procura uma saída ("o que sai daqui", "o que o Modelo faz").
                    FormTextFieldBrownNoIcon(
                        modifier = modifier.fillMaxWidth(),
                        value = uiState.filtro,
                        label = R.string.label_rota_ou_embarcacao,
                        onValueChange = onFiltroChange,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Search,
                        ),
                    )
                }
                FormDashedDivider(modifier = modifier.fillMaxWidth())

                LazyColumn {
                    items(uiState.resultados) { viagem ->
                        CardResultViagem(
                            modifier = modifier,
                            viagem = viagem,
                            podeInativar = uiState.podeInativar,
                            onInativar = { viagemParaInativar = it },
                        )
                    }
                }
            }
        }

        viagemParaInativar?.let { viagem ->
            CommonInformativeDialog(
                modifier = Modifier,
                // A mensagem diz o que inativar significa: some dos seletores, continua valendo para o
                // bilhete já emitido. Sem isso, "inativar" seria lido como "apagar".
                textMensagem = R.string.msg_confirmar_inativar_viagem,
                textConfirm = R.string.btn_inativar,
                textDismiss = R.string.btn_cancelar,
                onConfirm = {
                    onInativar(viagem.id)
                    viagemParaInativar = null
                },
                onDismiss = { viagemParaInativar = null },
            )
        }
    }
}

@Composable
fun CardResultViagem(
    modifier: Modifier,
    viagem: ViagemResultado,
    podeInativar: Boolean,
    onInativar: (ViagemResultado) -> Unit,
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
                // A partida vem primeiro: é ela que identifica a saída, e é por ela que se procura.
                TextTitleBrownRegular(text = viagem.partida)
                TextRegularBrown(text = viagem.rota)
                TextRegularBrown(text = viagem.embarcacao)
                if (viagem.chegada.isNotBlank()) {
                    TextSubTitleBrownItalic(
                        text = stringResource(R.string.msg_chegada_estimada, viagem.chegada),
                    )
                }
                // A inativa **fica na lista**, marcada: o descartado é registro.
                if (!viagem.ativa) {
                    TextSubTitleBrownItalic(text = stringResource(R.string.msg_viagem_inativa))
                }
            }

            // Inativar é ato de plataforma (ADR-0022 D3): para quem não é, o botão não existe.
            if (podeInativar && viagem.ativa) {
                IconButton(onClick = { onInativar(viagem) }) {
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
private fun ResultSearchViagemScreenPreview() {
    FluviAppTheme {
        ResultSearchViagemScreen(
            uiState = PesquisaViagemUiState(
                podeInativar = true,
                resultados = listOf(
                    ViagemResultado(
                        id = "1",
                        rota = "Porto de Val-de-Cães · Belém/PA → Porto de Parintins · Parintins/AM",
                        embarcacao = "F/B Modelo",
                        partida = "Terça-feira · 18:00",
                        chegada = "Qui 00:00",
                        ativa = true,
                    ),
                    ViagemResultado(
                        id = "2",
                        rota = "Porto de Parintins · Parintins/AM → Porto de Val-de-Cães · Belém/PA",
                        embarcacao = "F/B Modelo",
                        partida = "Sexta-feira · 06:00",
                        chegada = "12:00",
                        ativa = false,
                    ),
                ),
            ),
        )
    }
}

@Preview
@Composable
private fun ResultSearchViagemSemConcessaoPreview() {
    FluviAppTheme {
        ResultSearchViagemScreen(uiState = PesquisaViagemUiState(semConcessao = true))
    }
}