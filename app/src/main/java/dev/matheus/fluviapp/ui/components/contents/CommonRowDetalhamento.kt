package dev.matheus.fluviapp.ui.components.contents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import dev.matheus.fluviapp.ui.components.StatusPassagemBadge
import dev.matheus.fluviapp.ui.components.texts.TextBoldNavyBlue
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrownItalic

@Composable
fun CommonRowDetalhamento(
    modifier: Modifier,
    label: Int,
    valor: String
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextRegularBrownItalic(text = "${stringResource(id = label)}:")
        TextBoldNavyBlue(text = valor)
    }
}

/**
 * Variante do detalhamento cujo valor é o **status do ciclo de vida** como badge colorido
 * (ADR-0012 Fase 5), no lugar do texto puro — o status vira legível de relance também aqui.
 */
@Composable
fun CommonRowDetalhamentoStatus(
    modifier: Modifier,
    label: Int,
    situacao: String
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextRegularBrownItalic(text = "${stringResource(id = label)}:")
        StatusPassagemBadge(situacao = situacao)
    }
}