package br.com.gruponaveg.ui.components.forms.dropdowns

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import br.com.gruponaveg.R
import br.com.gruponaveg.ui.components.forms.fields.FormTextFieldBrownTrailingIcon
import br.com.gruponaveg.ui.components.texts.TextRegular

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropDownFormField(
    modifier: Modifier,
    listaItens: List<String>,
    label: Int,
    value: String = "",
    focusManager: FocusManager = LocalFocusManager.current,
    readOnly: Boolean = false,
    isError: Boolean = false,
    onValueChange: (String) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    var textFieldSize by remember { mutableStateOf(Size.Zero) }

    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = {}
    ) {
        FormTextFieldBrownTrailingIcon(
            modifier = modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .onGloballyPositioned { coordinates ->
                    textFieldSize = coordinates.size.toSize()
                }
                .onFocusChanged {
                    if (it.isFocused && !readOnly) {
                        expanded = !expanded
                    }
                },
            value = value,
            label = label,
            onValueChange = onValueChange,
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            isError = isError,
            textoErro = R.string.error_camp_obrig,
        )

        DropdownMenu(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .width(
                    with(LocalDensity.current) {
                        textFieldSize.width.toDp()
                    }
                ),
            expanded = expanded,
            onDismissRequest = {
                focusManager.clearFocus(force = true)
                expanded = false
            },
        ) {
            listaItens.forEach { item ->
                DropdownMenuItem(
                    text = { TextRegular(text = item) },
                    onClick = {
                        onValueChange(item)
                        focusManager.clearFocus(force = true)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FormFieldDropDownPreview() {
    DropDownFormField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        listaItens = listOf("Americano", "Cappuccino", "Espresso", "Latte", "Mocha"),
        label = R.string.label_documento,
        value = "",
        onValueChange = {}
    )
}