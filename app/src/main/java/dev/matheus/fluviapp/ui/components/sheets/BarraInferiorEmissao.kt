package dev.matheus.fluviapp.ui.components.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.components.texts.TextBoldWhiteItalic
import dev.matheus.fluviapp.ui.theme.FluviAppTheme

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun BarraInferiorEmissao(
    modifier: Modifier,
    onDismissSheetEmissao: () -> Unit,
    onClickImpressaoFisica: () -> Unit,
    onClickEmitirPassagemDigital: () -> Unit
) {
    ModalBottomSheet(
        modifier = modifier.fillMaxWidth(),
        onDismissRequest = onDismissSheetEmissao,
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary
    ) {
        Row(
            modifier = modifier
                .padding(vertical = 10.dp, horizontal = 20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(
                modifier = modifier.weight(.5f),
                onClick = onClickImpressaoFisica
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_imprimir_24),
                        contentDescription = stringResource(id = R.string.description_impressora),
                        tint = MaterialTheme.colorScheme.onSecondary
                    )
                    TextBoldWhiteItalic(text = stringResource(R.string.btn_via_fisica))
                }
            }

            IconButton(
                modifier = modifier.weight(.5f),
                onClick = onClickEmitirPassagemDigital
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_report_24),
                        contentDescription = stringResource(id = R.string.description_imagem),
                        tint = MaterialTheme.colorScheme.onSecondary
                    )
                    TextBoldWhiteItalic(text = stringResource(R.string.btn_via_digital))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BarraInferiorEmissaoPreview() {
    FluviAppTheme {
        BarraInferiorEmissao(
            modifier = Modifier,
            onDismissSheetEmissao = {},
            onClickImpressaoFisica = {},
            onClickEmitirPassagemDigital = {}
        )
    }
}

