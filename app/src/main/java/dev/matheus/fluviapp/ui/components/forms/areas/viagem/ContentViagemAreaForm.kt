package dev.matheus.fluviapp.ui.components.forms.areas.viagem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.model.mapDescricao
import dev.matheus.fluviapp.ui.components.forms.areas.CommonAreaForm
import dev.matheus.fluviapp.ui.components.forms.dropdowns.DropDownFormField
import dev.matheus.fluviapp.ui.states.FormViagemUiState

@Composable
fun ContentViagemAreaForm(
    modifier: Modifier,
    state: FormViagemUiState,
    focusManager: FocusManager = LocalFocusManager.current,
) {

    DropDownFormField(
        listaItens = state.listaEmpresas.map { it.nome },
        label = R.string.label_empresa,
        modifier = modifier.fillMaxWidth(),
        value = state.empresa,
        onValueChange = state.onEmpresaChange,
        isError = state.isEmpresaError,
        focusManager = focusManager
    )

    DropDownFormField(
        listaItens = state.listaNavios.mapDescricao(),
        label = R.string.label_navio,
        modifier = modifier.fillMaxWidth(),
        value = state.navio,
        onValueChange = state.onNavioChange,
        isError = state.isNavioError,
        readOnly = state.isNavioDisable,
        focusManager = focusManager
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DropDownFormField(
            listaItens = state.listaMunicipios.mapDescricao().filter {
                it != state.trechoDestino
            },
            label = R.string.label_trecho_origem,
            modifier = modifier,
            value = state.trechoOrigem,
            onValueChange = state.onTrechoOrigemChange,
            isError = state.isTrechoOrigemError,
            focusManager = focusManager
        )

        IconButton(onClick = state.onClickLimparTrechoOrigem) {
            Icon(
                imageVector = Icons.Filled.Clear,
                contentDescription = stringResource(id = R.string.description_limpar),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DropDownFormField(
            listaItens = state.listaMunicipios.mapDescricao().filter {
                it != state.trechoOrigem
            },
            label = R.string.label_trecho_destino,
            modifier = modifier,
            value = state.trechoDestino,
            onValueChange = state.onTrechoDestinoChange,
            isError = state.isTrechoDestinoError,
            readOnly = state.isTrechoDestinoDisabled,
            focusManager = focusManager
        )

        IconButton(onClick = state.onClickLimparTrechoDestino) {
            Icon(
                imageVector = Icons.Filled.Clear,
                contentDescription = stringResource(id = R.string.description_limpar),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
@Preview(showBackground = true)
@Composable
private fun ContentViagemAreaFormPreview() {
    CommonAreaForm(
        modifier = Modifier,
        titleArea = R.string.form_area_title_viagem
    ) {
        ContentViagemAreaForm(modifier = it, state = FormViagemUiState())
    }
}
