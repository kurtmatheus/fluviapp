package dev.matheus.fluviapp.ui.components.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.model.screendata.DadosBotoesMenus
import dev.matheus.fluviapp.sampledata.listaBotoesMenuPassagensSample
import dev.matheus.fluviapp.ui.components.texts.TextTitleBrownItalic

@Composable
fun CardBotaoMenu(
    modifier: Modifier,
    dados: DadosBotoesMenus,
) {
    CommonCard(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary,
        onClick = dados.onClick
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = dados.icon),
                contentDescription = "Icone Menu",
                tint = MaterialTheme.colorScheme.onBackground
            )
            HorizontalDivider(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(20.dp, 0.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onBackground
            )
            TextTitleBrownItalic(
                text = stringResource(id = dados.title)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CardPrincipalPreview() {
    CardBotaoMenu(
        modifier = Modifier,
        dados = listaBotoesMenuPassagensSample.first(),
    )
}