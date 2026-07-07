package dev.matheus.fluviapp.ui.components.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrownItalic
import dev.matheus.fluviapp.ui.components.texts.TextTitleBrownItalic
import dev.matheus.fluviapp.ui.states.passagem.FormPassagemUiState

@Composable
fun ViagemCard(
    modifier: Modifier,
    state: FormPassagemUiState
) {
    CommonCard(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary,
        enable = false,
        onClick = {}
    ) {
        Column(
            modifier = modifier
                .padding(20.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            TextTitleBrownItalic(
                text = stringResource(id = R.string.card_title_viagem)
            )
            Row(
                modifier = modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_navio_24),
                    contentDescription = stringResource(id = R.string.description_icon_navio),
                    tint = MaterialTheme.colorScheme.onBackground
                )
                TextRegularBrownItalic(text = ":   ${state.navioViagem}")
            }

            Row(
                modifier = modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = stringResource(id = R.string.description_codigo),
                    tint = MaterialTheme.colorScheme.onBackground
                )
                TextRegularBrownItalic(text = ":   ${state.codigoViagem}")
            }

            Row(
                modifier = modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = stringResource(id = R.string.description_local),
                    tint = MaterialTheme.colorScheme.onBackground
                )
                TextRegularBrownItalic(text = ":   ${state.origemViagem} - ${state.destinoViagem}")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ViagemCardPreview() {
    ViagemCard(
        modifier = Modifier,
        state = FormPassagemUiState(
            navioViagem = "F/B Regional",
            horaViagem = "16:00",
            origemViagem = "BELEM - PA",
            destinoViagem = "SANTANA - AP",
            codigoViagem = "BEL-SAN-REGI"
    )
    )
}