package dev.matheus.fluviapp.ui.screens.faturamento

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.sampledata.listaDadosBalancoPassagems
import dev.matheus.fluviapp.ui.components.cards.BalancoNavioCard
import dev.matheus.fluviapp.ui.components.contents.CommonTopRow
import dev.matheus.fluviapp.ui.components.forms.buttons.CommonIconButton
import dev.matheus.fluviapp.ui.components.forms.fields.FormFieldCalendario
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrownItalic
import dev.matheus.fluviapp.ui.screens.forms.CommonScreenNoBottom
import dev.matheus.fluviapp.ui.states.faturamento.BalancoState

@Composable
fun BalancoScreen(
    state: BalancoState,
    focusManager: FocusManager = LocalFocusManager.current,
    onClickVoltar: () -> Unit = {},
    onClickPesquisar: (String) -> Unit = {},
) {
    CommonScreenNoBottom(
        titleTopAppBar = R.string.title_top_passagem,
        titleTopContent = R.string.subtitle_balanco,
        isShowRightIcon = false,
        hasRefresh = false,
        isRefreshing = false,
        onClickVoltar = onClickVoltar
    ) { modifier, title ->

        Column(
            modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CommonTopRow(modifier = modifier, titulo = title)

            Column(
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FormFieldCalendario(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    focusManager = focusManager,
                    value = state.dataViagem,
                    label = R.string.label_data_viagem,
                    onValueChange = state.onDataViagemChange,
                    isError = state.isDataViagemError,
                    textoErro = R.string.error_camp_obrig
                )

                CommonIconButton(
                    modifier = modifier.padding(bottom = 25.dp),
                    text = "Gerar",
                    onClick = { onClickPesquisar(state.dataViagem) },
                    isProcessing = state.isProcessing
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onBackground
            )

            if (!state.isProcessing && state.listaDadosBalancoPassagens.isEmpty()) {
                if (state.jaFoiGerado) {
                    TextRegularBrownItalic(
                        modifier = modifier.padding(20.dp),
                        text = stringResource(id = R.string.msg_nenhum_resultado_pesquisa)
                    )
                }
            } else {
                LazyColumn(
                    modifier = modifier.padding(top = 20.dp, start = 10.dp, end = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.listaDadosBalancoPassagens) { dados ->
                        BalancoNavioCard(
                            modifier = modifier,
                            dadosBalancoPassagem = dados
                        )
                    }
                }
            }
        }
    }
}


@Preview
@Composable
private fun ListaRelatoriosScreenPreview() {
    BalancoScreen(
        state = BalancoState(
            listaDadosBalancoPassagens = listaDadosBalancoPassagems
        )
    )
}

@Preview
@Composable
private fun ListaRelatoriosScreenComErroPreview() {
    BalancoScreen(
        state = BalancoState(
            isDataViagemError = true
        )
    )
}

@Preview
@Composable
private fun ListaRelatoriosScreenProcessandoPreview() {
    BalancoScreen(
        state = BalancoState(
            isDataViagemError = true,
            isProcessing = true
        )
    )
}