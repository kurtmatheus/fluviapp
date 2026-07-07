package br.com.gruponaveg.ui.components.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.gruponaveg.R
import br.com.gruponaveg.model.screendata.DadosViagemCard
import br.com.gruponaveg.sampledata.listaDadosDadosViagemHomeSampleCards
import br.com.gruponaveg.ui.components.texts.TextTitleBrownItalic
import br.com.gruponaveg.ui.components.texts.TextTitleBrownRegular

@Composable
fun DetalhesViagemPreviewCard(
    modifier: Modifier,
    dadosViagemCard: DadosViagemCard,
    onClick: (String) -> Unit =  {}
) {
    CommonCard(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondary,
        borderStroke = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.onBackground),
        alturaCard = 150,
        onClick = { onClick(dadosViagemCard.idViagem) },
        conteudo = {
            Column(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(20.dp, 0.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        modifier = modifier
                            .height(100.dp)
                            .padding(top = 10.dp, end = 10.dp),
                        painter = painterResource(id = R.drawable.ic_navio_75),
                        contentDescription = stringResource(id = R.string.description_icon_navio),
                        tint = MaterialTheme.colorScheme.onBackground
                    )

                    Box(
                        modifier = modifier.padding(end = 15.dp)
                    ) {
                        VerticalDivider(
                            modifier = modifier
                                .height(110.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        TextTitleBrownItalic(
                            text = dadosViagemCard.navio
                        )
                        TextTitleBrownRegular(
                            text = "${dadosViagemCard.origem} - ${dadosViagemCard.destino}"
                        )
                        TextTitleBrownItalic(
                            text = dadosViagemCard.codigo
                        )
                    }
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun DetalhesViagemCardPreview() {
    DetalhesViagemPreviewCard(
        modifier = Modifier,
        dadosViagemCard = listaDadosDadosViagemHomeSampleCards.first()
    )
}