package br.com.gruponaveg.ui.components.contents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.gruponaveg.R
import br.com.gruponaveg.extensions.formatarDataBarrasBr
import br.com.gruponaveg.ui.components.texts.TextRegularBrownItalic
import br.com.gruponaveg.ui.components.texts.TextTitleBrownItalic
import java.time.LocalDate

@Composable
fun CommonTopRow(modifier: Modifier, titulo: Int) {
    Column {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(10.dp, 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextTitleBrownItalic(
                text = stringResource(id = titulo)
            )

            TextRegularBrownItalic(
                text = "${stringResource(id = R.string.label_info_hoje)}: ${
                    LocalDate.now().formatarDataBarrasBr()
                }"
            )

        }

        HorizontalDivider(
            modifier = modifier
                .fillMaxWidth()
                .padding(10.dp, 0.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.onBackground
        )
    }

}

@Preview(showBackground = true)
@Composable
fun CommonTopRowPreview() {
        CommonTopRow(
            modifier = Modifier.padding(10.dp),
            titulo = R.string.subtitle_nova_passagem
        )
}