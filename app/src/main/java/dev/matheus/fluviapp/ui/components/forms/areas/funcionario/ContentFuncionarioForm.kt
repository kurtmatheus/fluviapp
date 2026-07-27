package dev.matheus.fluviapp.ui.components.forms.areas.funcionario

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
import dev.matheus.fluviapp.ui.states.FormFuncionarioUiState

@Composable
fun ContentFuncionarioForm(
    modifier: Modifier,
    state: FormFuncionarioUiState,
    onAgenciaChange: (String) -> Unit,
    onFuncionarioChange: (String) -> Unit,
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
        value = state.funcionario,
        onValueChange = onFuncionarioChange,
        label = R.string.label_agente,
        isError = state.isFuncionarioError,
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
private fun ContentFuncionarioFormPreview() {
    CommonAreaForm(
        modifier = Modifier,
        titleArea = R.string.form_area_title_agencia,
    ) {
        ContentFuncionarioForm(
            modifier = it,
            state = FormFuncionarioUiState(agencia = "MATRIZ", funcionario = "Agente Modelo", lotacao = "PORTO NORTE"),
            onAgenciaChange = {},
            onFuncionarioChange = {},
            onLotacaoChange = {},
        )
    }
}
