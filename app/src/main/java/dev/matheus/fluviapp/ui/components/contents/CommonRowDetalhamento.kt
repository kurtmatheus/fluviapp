package dev.matheus.fluviapp.ui.components.contents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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