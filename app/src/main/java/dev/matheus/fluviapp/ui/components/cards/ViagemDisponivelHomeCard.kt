package dev.matheus.fluviapp.ui.components.cards

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
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrown
import dev.matheus.fluviapp.ui.components.texts.TextSubTitleBrownItalic
import dev.matheus.fluviapp.ui.components.texts.TextTitleBrownRegular
import dev.matheus.fluviapp.ui.states.ViagemDisponivelCard
import dev.matheus.fluviapp.ui.theme.FluviAppTheme

/**
 * Uma saída disponível no Início da empresa — a herdeira do `HomeCard` que a F8.0 demoliu.
 *
 * A **partida vem primeiro e em destaque**, e é a mudança de fundo em relação ao card antigo: aquele
 * abria pelo nome da embarcação, porque a Viagem-trecho não tinha data nem hora — o horário era digitado
 * na emissão. Agora a ocorrência é datada, e é a data que responde a pergunta de quem abre o app.
 */
@Composable
fun ViagemDisponivelHomeCard(
    modifier: Modifier,
    viagem: ViagemDisponivelCard,
    onClick: (String) -> Unit = {},
) {
    CommonCard(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary,
        onClick = { onClick(viagem.viagemId) },
        alturaCard = 150,
        conteudo = {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(10.dp, 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Icon(
                    modifier = modifier
                        .height(75.dp)
                        .padding(top = 10.dp, end = 10.dp),
                    painter = painterResource(id = R.drawable.ic_embarcacao_75),
                    contentDescription = stringResource(id = R.string.description_icon_embarcacao),
                    tint = MaterialTheme.colorScheme.onBackground,
                )

                VerticalDivider(
                    modifier = modifier
                        .height(125.dp)
                        .padding(end = 15.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TextTitleBrownRegular(text = viagem.partida)
                    TextRegularBrown(text = viagem.rota)
                    TextSubTitleBrownItalic(text = viagem.embarcacao)
                    if (viagem.chegada.isNotBlank()) {
                        TextSubTitleBrownItalic(
                            text = stringResource(R.string.msg_chegada_estimada, viagem.chegada),
                        )
                    }
                }
            }
        },
    )
}

@Preview
@Composable
private fun ViagemDisponivelHomeCardPreview() {
    FluviAppTheme {
        ViagemDisponivelHomeCard(
            modifier = Modifier,
            viagem = ViagemDisponivelCard(
                id = "v1@2026-08-11",
                viagemId = "v1",
                partida = "Terça-feira, 11/08 · 18:00",
                rota = "Porto de Val-de-Cães · Belém/PA → Porto de Parintins · Parintins/AM",
                embarcacao = "F/B Modelo",
                chegada = "Qui 00:00",
            ),
        )
    }
}