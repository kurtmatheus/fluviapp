package dev.matheus.fluviapp.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.components.texts.TextTitleBrownItalic

@Composable
fun ProcessDialog(
    isProcessing: Boolean,
    modifier: Modifier
) {
    CommonDialog(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        onDismiss = { }
    ) {
        if (isProcessing) {
            Column(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = modifier,
                    color = MaterialTheme.colorScheme.primary
                )

                TextTitleBrownItalic(text = stringResource(id = R.string.label_processando))
            }
        } else {
            Column(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    modifier = modifier.scale(2f),
                    imageVector = Icons.Filled.Check,
                    contentDescription = stringResource(id = R.string.description_ok),
                    tint = MaterialTheme.colorScheme.primary
                )

                TextTitleBrownItalic(text = stringResource(id = R.string.label_pronto))
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun ProcessDialogProcessandoPreview() {
    ProcessDialog(
        isProcessing = true,
        modifier = Modifier.padding(10.dp)
    )
}

@Preview(showBackground = true)
@Composable
private fun ProcessDialogProntoPreview() {
    ProcessDialog(
        isProcessing = false,
        modifier = Modifier.padding(20.dp)
    )
}