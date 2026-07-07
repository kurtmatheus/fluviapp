package dev.matheus.fluviapp.ui.components.dialogs


import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.extensions.convertMillisToLocalDateToString
import dev.matheus.fluviapp.ui.components.texts.TextRegularWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    displayMode: DisplayMode = DisplayMode.Picker
) {
    val state = rememberDatePickerState(
        initialDisplayMode = displayMode
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    state.selectedDateMillis?.apply {
                        onConfirm(convertMillisToLocalDateToString())
                    }
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
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun DateDialogPreview() {
    DateDialog(
        onDismiss = {},
        onConfirm = {},
    )
}