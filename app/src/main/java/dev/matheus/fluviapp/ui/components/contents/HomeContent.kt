package dev.matheus.fluviapp.ui.components.contents

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.screendata.DadosViagemCard
import dev.matheus.fluviapp.sampledata.listaDadosDadosViagemHomeSampleCards
import dev.matheus.fluviapp.ui.components.cards.HomeCard
import dev.matheus.fluviapp.ui.components.texts.TextSubTitleBrownItalic

@Composable
fun HomeContent(
    modifier: Modifier,
    titulo: Int,
    listaViagens: List<DadosViagemCard> = emptyList(),
    onClickNovaPassagem: (String) -> Unit = {}
) {
    Column {
        CommonTopRow(modifier, titulo)

        if (listaViagens.isNotEmpty()) {
            LazyColumn {
                items(listaViagens) {
                    HomeCard(
                        modifier = modifier,
                        dadosViagemCard = it,
                        onClickNovaPassagem = onClickNovaPassagem
                    )
                }
            }
        } else {
            TextSubTitleBrownItalic(
                modifier = modifier
                    .fillMaxSize()
                    .padding(75.dp, 30.dp)
                    .align(Alignment.CenterHorizontally)
                    .verticalScroll(state = rememberScrollState()),
                text = stringResource(R.string.msg_nenhuma_viagem)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConteudoHomePreview() {
        HomeContent(
            modifier = Modifier,
            titulo = R.string.subtitle_viagens_disponiveis,
            listaViagens = listaDadosDadosViagemHomeSampleCards
        )
}

@Preview(showBackground = true)
@Composable
private fun ConteudoHomeVaziaPreview() {
        HomeContent(
            modifier = Modifier,
            titulo = R.string.subtitle_viagens_disponiveis
        )
}