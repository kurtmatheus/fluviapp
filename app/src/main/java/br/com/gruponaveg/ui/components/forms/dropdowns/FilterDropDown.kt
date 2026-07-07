package br.com.gruponaveg.ui.components.forms.dropdowns

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.gruponaveg.R
import br.com.gruponaveg.ui.components.forms.fields.FormTextFieldBrownNoIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropDownForm(
    modifier: Modifier,
    listaItens: List<String>,
    label: Int,
    value: String,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    isError: Boolean = false,
    onValueChange: (String) -> Unit = {},
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        Column {
            FormTextFieldBrownNoIcon(
                modifier = modifier
                    .menuAnchor(MenuAnchorType.PrimaryEditable),
                value = value,
                onValueChange = {
                    onValueChange(it)
                    expanded = true
                },
                label = label,
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = ImeAction.Next,
                    capitalization = KeyboardCapitalization.Characters
                ),
                readOnly = readOnly,
                enabled = enabled,
                isError = isError,
                visualTransformation = visualTransformation
            )
        }
        // filter options based on text field value
        val filteringOptions = listaItens.filter { it.startsWith(value, ignoreCase = true) }
        if (filteringOptions.isNotEmpty()) {
            ExposedDropdownMenu(
                modifier = modifier
                    .exposedDropdownSize(true),
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                filteringOptions.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = {
                            onValueChange(selectionOption)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FilterDropDownFormPreview() {
    Surface {
        FilterDropDownForm(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            listaItens = listOf("Americano", "Cappuccino", "Espresso", "Latte", "Mocha"),
            label = R.string.label_documento,
            value = "",
            onValueChange = {},
            keyboardType = KeyboardType.Number
        )
    }
}