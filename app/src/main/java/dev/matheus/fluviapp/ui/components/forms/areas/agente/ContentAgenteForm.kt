package dev.matheus.fluviapp.ui.components.forms.areas.agente

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.components.forms.areas.CommonAreaForm
import dev.matheus.fluviapp.ui.components.forms.dropdowns.DropDownFormField
import dev.matheus.fluviapp.ui.components.forms.dropdowns.FilterDropDownForm
import dev.matheus.fluviapp.ui.components.forms.fields.FormTextFieldBrownNoIcon
import dev.matheus.fluviapp.ui.states.FormAgenteUiState

@Composable
fun ContentAgenteForm(
    modifier: Modifier,
    state: FormAgenteUiState,
    onAgenciaChange: (String) -> Unit,
    onAgenteChange: (String) -> Unit,
    onLotacaoChange: (String) -> Unit,
) {
    FilterDropDownForm(
        modifier = modifier.fillMaxWidth(),
        listaItens = state.listaAgencia,
        label = R.string.label_agencia,
        value = state.agencia,
        isError = state.isAgenciaError,
        onValueChange = onAgenciaChange,
        keyboardType = KeyboardType.Text,
    )

    FormTextFieldBrownNoIcon(
        modifier = modifier,
        value = state.agente,
        onValueChange = onAgenteChange,
        label = R.string.label_agente,
        isError = state.isAgenteError,
        keyboardOptions = KeyboardOptions(KeyboardCapitalization.Characters),
    )

    DropDownFormField(
        modifier = modifier.fillMaxWidth(),
        listaItens = state.listaMunicipios,
        label = R.string.label_lotacao,
        value = state.lotacao,
        isError = state.isLotacaoError,
        onValueChange = onLotacaoChange,
    )
}

@Preview(showBackground = true)
@Composable
private fun ContentAgenteFormPreview() {
    CommonAreaForm(
        modifier = Modifier,
        titleArea = R.string.form_area_title_agencia,
    ) {
        ContentAgenteForm(
            modifier = it,
            state = FormAgenteUiState(agencia = "MATRIZ", agente = "Agente Modelo", lotacao = "PORTO NORTE"),
            onAgenciaChange = {},
            onAgenteChange = {},
            onLotacaoChange = {},
        )
    }
}
