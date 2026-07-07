package dev.matheus.fluviapp.ui.components.forms.fields

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.components.dialogs.TimeDialog

@Composable
fun FormFieldRelogio(
    modifier: Modifier,
    focusManager: FocusManager,
    value: String,
    onValueChange: (String) -> Unit,
    label: Int,
    isError: Boolean,
) {
    var showTimeDialog by remember {
        mutableStateOf(false)
    }

    FormTextFieldBrownTrailingIcon(
        modifier = modifier,
        value = value,
        label = label,
        onValueChange = onValueChange,
        focusManager = focusManager,
        readOnly = true,
        textoErro = R.string.error_camp_obrig,
        isError = isError,
        onFocusChange = {
            showTimeDialog = true
        },
        trailingIcon = {
            IconButton(
                onClick = {
                    showTimeDialog = true
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_relogio_24),
                    contentDescription = stringResource(id = R.string.description_calendario)
                )
            }
        }
    )

    if (showTimeDialog) {
        TimeDialog(
            modifier = modifier,
            onDismiss = {
                showTimeDialog = false
                focusManager.clearFocus(force = true)
            },
            onConfirm = {
                onValueChange(it)
                focusManager.clearFocus(force = true)
            }
        )
    }
}
