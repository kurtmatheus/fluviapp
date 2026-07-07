package dev.matheus.fluviapp.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.components.texts.TextBoldWhiteItalic
import dev.matheus.fluviapp.ui.components.texts.TextRegularWhiteItalic

@Composable
fun CommonInformativeDialog(
    modifier: Modifier,
    textMensagem: Int,
    textConfirm: Int,
    textDismiss: Int,
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    CommonDialog(
        modifier = modifier,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = modifier
                .padding(20.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = stringResource(id = R.string.description_info)
            )

            TextRegularWhiteItalic(
                modifier = modifier.fillMaxWidth(),
                text = stringResource(id = textMensagem)
            )

            Row(
                modifier = modifier
                    .align(Alignment.End),
            ) {
                TextButton(onClick = onDismiss) {
                    TextBoldWhiteItalic(text = stringResource(id = textDismiss))
                }
                TextButton(onClick = onConfirm) {
                    TextRegularWhiteItalic(text = (stringResource(id = textConfirm)))
                }
            }
        }

    }
}

@Preview
@Composable
fun CommonInformativeDialogPreview() {
    CommonInformativeDialog(
        modifier = Modifier,
        textMensagem = R.string.msg_retornar_emissao,
        textConfirm = R.string.btn_retornar,
        textDismiss = R.string.btn_cancelar
    )
}