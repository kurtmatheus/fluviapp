package dev.matheus.fluviapp.ui.components.forms.areas.passagem

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.model.mapDescricao
import dev.matheus.fluviapp.ui.components.forms.areas.CommonAreaForm
import dev.matheus.fluviapp.ui.components.forms.dropdowns.FilterDropDownForm
import dev.matheus.fluviapp.ui.states.passagem.FormPassagemUiState

@Composable
fun ContentAgenciaAreaPassagemForm(
    modifier: Modifier,
    state: FormPassagemUiState
) {
    FilterDropDownForm(
        modifier = modifier.fillMaxWidth(),
        listaItens = state.listaAgencia,
        label = R.string.label_agencia,
        value = state.agencia,
        isError = state.isAgenciaError,
        onValueChange = state.onAgenciaChange,
        keyboardType = KeyboardType.Text
    )

    FilterDropDownForm(
        modifier = modifier.fillMaxWidth(),
        listaItens = state.listaAgente.mapDescricao(),
        label = R.string.label_agente,
        value = state.agente,
        readOnly = state.isAgenteDisabled,
        enabled = !state.agencia.isBlank(),
        isError = state.isAgenteError,
        onValueChange = state.onAgenteChange,
        keyboardType = KeyboardType.Text
    )
}

@Preview(showBackground = true)
@Composable
private fun ContentViagemAreaFormPreview() {
    CommonAreaForm(
        modifier = Modifier,
        titleArea = R.string.form_area_title_agencia
    ) {
        ContentAgenciaAreaPassagemForm(
            modifier = it,
            state = FormPassagemUiState(),
        )
    }
}
