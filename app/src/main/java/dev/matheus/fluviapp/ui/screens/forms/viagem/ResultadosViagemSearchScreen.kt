package dev.matheus.fluviapp.ui.screens.forms.viagem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.sampledata.listaDadosDadosViagemHomeSampleCards
import dev.matheus.fluviapp.ui.components.cards.DetalhesViagemPreviewCard
import dev.matheus.fluviapp.ui.components.contents.CommonTopRow
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrownItalic
import dev.matheus.fluviapp.ui.screens.forms.CommonScreenNoBottom
import dev.matheus.fluviapp.ui.states.PesquisarViagemUiState

@Composable
fun ResultadosViagemSearchScreen(
    state: PesquisarViagemUiState,
    onClickVoltar: () -> Unit = {},
    onClickViagem: (String) -> Unit = {},
) {
    CommonScreenNoBottom(
        titleTopAppBar = R.string.title_top_viagem,
        titleTopContent = R.string.subtitle_resultados,
        isShowRightIcon = false,
        hasRefresh = false,
        isRefreshing = false,
        onClickVoltar = onClickVoltar,
    ) { modifier, title ->
        Column {
            CommonTopRow(modifier = modifier, titulo = title)

            if (state.listaResultadoViagens.isNotEmpty()) {
                LazyColumn {
                    items(state.listaResultadoViagens) {
                        DetalhesViagemPreviewCard(
                            modifier = modifier,
                            dadosViagemCard = it,
                            onClick = onClickViagem
                        )
                    }
                }
            } else {
                TextRegularBrownItalic(
                    modifier = modifier
                        .padding(20.dp)
                        .align(Alignment.CenterHorizontally),
                    text = stringResource(id = R.string.msg_nenhum_resultado_pesquisa)
                )
            }
        }

    }
}

@Preview
@Composable
private fun ResultadosViagemSearchScreenPreview() {
    ResultadosViagemSearchScreen(
        state = PesquisarViagemUiState(
            listaResultadoViagens = listaDadosDadosViagemHomeSampleCards
        )
    )
}

@Preview
@Composable
private fun ResultadosViagemSearchScreenDialogPreview() {
    ResultadosViagemSearchScreen(
        state = PesquisarViagemUiState(
            listaResultadoViagens = emptyList(),
        )
    )
}