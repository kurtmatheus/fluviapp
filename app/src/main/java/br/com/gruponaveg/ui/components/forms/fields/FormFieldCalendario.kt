package br.com.gruponaveg.ui.components.forms.fields

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.gruponaveg.R
import br.com.gruponaveg.ui.components.dialogs.DateDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormFieldCalendario(
    modifier: Modifier,
    focusManager: FocusManager,
    value: String,
    label: Int,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    textoErro: Int,
    modoCalendario: Boolean = true
) {
    var showDateDialog by remember {
        mutableStateOf(false)
    }

    val displayMode = if (modoCalendario) DisplayMode.Picker else DisplayMode.Input

    FormTextFieldBrownTrailingIcon(
        modifier = modifier,
        value = value,
        label = label,
        onValueChange = onValueChange,
        isError = isError,
        focusManager = focusManager,
        readOnly = true,
        onFocusChange = {
            showDateDialog = true
        },
        trailingIcon = {
            IconButton(onClick = { showDateDialog = true }) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = stringResource(id = R.string.description_calendario)
                )
            }
        },
        textoErro = textoErro
    )

    if (showDateDialog) {
        DateDialog(
            onDismiss = {
                showDateDialog = false
                focusManager.clearFocus(force = true)
            },
            onConfirm = {
                onValueChange(it)
                focusManager.clearFocus(force = true)
            },
            displayMode = displayMode
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FormFieldCalendarioPreview() {
    FormFieldCalendario(
        modifier = Modifier.padding(10.dp),
        focusManager = LocalFocusManager.current,
        value = "",
        label = R.string.label_data_nascimento,
        onValueChange = {},
        isError = false,
        textoErro = R.string.error_camp_obrig
    )
}