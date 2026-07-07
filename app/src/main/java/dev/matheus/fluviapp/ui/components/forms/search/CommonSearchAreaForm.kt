package dev.matheus.fluviapp.ui.components.forms.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.components.forms.buttons.CommonCheckboxField
import dev.matheus.fluviapp.ui.components.forms.divider.FormDashedDivider
import dev.matheus.fluviapp.ui.components.forms.fields.FormTextFieldBrownTrailingIcon

@Composable
fun CommonSearchAreaForm(
    modifier: Modifier,
    labelFiltro: Int,
    checked: Boolean = false,
    onCheck: (Boolean) -> Unit = {},
    formField: @Composable (Modifier) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(10.dp, 0.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        CommonCheckboxField(
            modifier = Modifier,
            label = labelFiltro,
            checked = checked,
            onCheck = onCheck
        )
        if (checked) formField(modifier)
        FormDashedDivider(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CommonSearchAreaFormPreview() {
    CommonSearchAreaForm(
        modifier = Modifier.padding(10.dp),
        labelFiltro = R.string.label_filtro_data_viagem,
        checked = true,
        onCheck = {}
    ) { modifier ->
        FormTextFieldBrownTrailingIcon(
            modifier = modifier,
            value = "",
            label = R.string.label_data_viagem,
            onValueChange = {},
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = stringResource(id = R.string.description_calendario)
                )
            },
            textoErro = R.string.error_camp_obrig
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CommonSearchAreaFormUncheckedPreview() {
    CommonSearchAreaForm(
        modifier = Modifier.padding(10.dp),
        labelFiltro = R.string.label_filtro_data_viagem,
        checked = false,
        onCheck = {}
    ) { modifier ->
        FormTextFieldBrownTrailingIcon(
            modifier = modifier,
            value = "",
            label = R.string.label_data_viagem,
            onValueChange = {},
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = stringResource(id = R.string.description_calendario)
                )
            },
            textoErro = R.string.error_camp_obrig
        )
    }
}