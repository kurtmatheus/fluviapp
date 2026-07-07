package br.com.gruponaveg.ui.components.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import br.com.gruponaveg.ui.components.texts.TextSubTitleBrownItalic
import br.com.gruponaveg.ui.components.texts.TextTitleBrownRegular

@Composable
fun HomeCard(
    modifier: Modifier,
    dadosViagemCard: DadosViagemCard,
    onClickNovaPassagem: (String) -> Unit = {},
) {
    CommonCard(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary,
        onClick = { onClickNovaPassagem(dadosViagemCard.idViagem) },
        alturaCard = 150,
        conteudo = {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(10.dp, 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Icon(
                    modifier = modifier
                        .height(75.dp)
                        .padding(top = 10.dp, end = 10.dp),
                    painter = painterResource(id = R.drawable.ic_navio_75),
                    contentDescription = stringResource(id = R.string.description_icon_navio),
                    tint = MaterialTheme.colorScheme.onBackground
                )

                VerticalDivider(
                    modifier = modifier
                        .height(125.dp)
                        .padding(end = 15.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TextTitleBrownRegular(
                        modifier = modifier.padding(top = 5.dp),
                        text = dadosViagemCard.navio
                    )
                    Row {
                        TextSubTitleBrownItalic(text = "${dadosViagemCard.origem} - ${dadosViagemCard.destino}")
                    }

                    TextSubTitleBrownItalic(text = dadosViagemCard.codigo)
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun CardPrincipalPreview() {
        HomeCard(
            modifier = Modifier,
            dadosViagemCard = listaDadosDadosViagemHomeSampleCards.first(),
        )
}