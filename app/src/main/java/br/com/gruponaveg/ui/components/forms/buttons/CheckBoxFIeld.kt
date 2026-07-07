package br.com.gruponaveg.ui.components.forms.buttons

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.gruponaveg.R
import br.com.gruponaveg.ui.components.texts.TextRegularNoColor
import br.com.gruponaveg.ui.theme.Brown

@Composable
fun CommonCheckboxField(
    modifier: Modifier,
    label: Int,
    checked: Boolean = false,
    onCheck: (Boolean) -> Unit = {},
    isError: Boolean = false,
) {

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheck,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = if (isError) Color.Red else Brown
            )
        )

        TextRegularNoColor(
            text = stringResource(id = label),
            color = if (isError) Color.Red else Brown
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CommonCheckboxFieldPreview() {
        CommonCheckboxField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            label = R.string.label_checkbox_veiculo,
            checked = false,
            onCheck = {
            }
        )
}

@Preview(showBackground = true)
@Composable
fun CommonCheckboxFieldErrorPreview() {
    CommonCheckboxField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        label = R.string.label_checkbox_veiculo,
        checked = false,
        onCheck = {
        },
        isError = true
    )
}