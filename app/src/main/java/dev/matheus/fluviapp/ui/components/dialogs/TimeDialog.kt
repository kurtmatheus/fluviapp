package dev.matheus.fluviapp.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.extensions.formatarTimeState
import dev.matheus.fluviapp.ui.components.texts.TextRegularWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeDialog(
    modifier: Modifier,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {

    val state = rememberTimePickerState(
        is24Hour = true
    )

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                TimePicker(
                    state = state,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        selectorColor = MaterialTheme.colorScheme.primary,
                        timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        timeSelectorSelectedContentColor = Color.White
                    )
                )
                Button(
                    onClick = {
                        onConfirm(state.formatarTimeState())
                        onDismiss()
                    },
                    colors = ButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        disabledContainerColor = Color.Gray,
                        disabledContentColor = Color.White
                    )
                ) {
                    TextRegularWhite(text = stringResource(id = R.string.btn_ok))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TimeDialogPreview() {
    TimeDialog(modifier = Modifier, onDismiss = {}, onConfirm = {})
}