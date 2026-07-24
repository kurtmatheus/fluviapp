package dev.matheus.fluviapp.ui.components.forms.areas.viagem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.model.mapDescricao
import dev.matheus.fluviapp.ui.components.forms.areas.CommonAreaForm
import dev.matheus.fluviapp.ui.components.forms.dropdowns.DropDownFormField
import dev.matheus.fluviapp.ui.components.forms.fields.FormTextFieldBrownLeadingIconLabelText
import dev.matheus.fluviapp.ui.states.FormViagemUiState

@Composable
fun ContentViagemAreaForm(
    modifier: Modifier,
    state: FormViagemUiState,
    onEmpresaChange: (String) -> Unit,
    onNavioChange: (String) -> Unit,
    onTrechoOrigemChange: (String) -> Unit,
    onLimparTrechoOrigem: () -> Unit,
    onTrechoDestinoChange: (String) -> Unit,
    onLimparTrechoDestino: () -> Unit,
    onTarifaChange: (String, String) -> Unit,
    focusManager: FocusManager = LocalFocusManager.current,
) {

    DropDownFormField(
        listaItens = state.listaEmpresas.map { it.nome },
        label = R.string.label_empresa,
        modifier = modifier.fillMaxWidth(),
        value = state.empresa,
        onValueChange = onEmpresaChange,
        isError = state.isEmpresaError,
        focusManager = focusManager
    )

    DropDownFormField(
        listaItens = state.listaNavios.mapDescricao(),
        label = R.string.label_navio,
        modifier = modifier.fillMaxWidth(),
        value = state.navio,
        onValueChange = onNavioChange,
        isError = state.isNavioError,
        readOnly = state.navioDesabilitado,
        focusManager = focusManager
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DropDownFormField(
            listaItens = state.listaMunicipios.mapDescricao().filter { it != state.trechoDestino },
            label = R.string.label_trecho_origem,
            modifier = modifier,
            value = state.trechoOrigem,
            onValueChange = onTrechoOrigemChange,
            isError = state.isTrechoOrigemError,
            focusManager = focusManager
        )

        IconButton(onClick = onLimparTrechoOrigem) {
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
            listaItens = state.listaMunicipios.mapDescricao().filter { it != state.trechoOrigem },
            label = R.string.label_trecho_destino,
            modifier = modifier,
            value = state.trechoDestino,
            onValueChange = onTrechoDestinoChange,
            isError = state.isTrechoDestinoError,
            readOnly = state.trechoDestinoDesabilitado,
            focusManager = focusManager
        )

        IconButton(onClick = onLimparTrechoDestino) {
            Icon(
                imageVector = Icons.Filled.Clear,
                contentDescription = stringResource(id = R.string.description_limpar),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }

    // Tarifa da inteira por acomodação (ADR-0013). Um campo por acomodação do catálogo; branco = não
    // ofertada (não vira célula). O valor é a base da qual meia/gratuidade e desconto derivam.
    if (state.tarifas.isNotEmpty()) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(id = R.string.label_tarifas_titulo),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )

            state.tarifas.forEachIndexed { index, tarifa ->
                // Subcabeçalho por grupo (Passageiro / Veículo) quando o grupo muda.
                if (index == 0 || state.tarifas[index - 1].grupoTitulo != tarifa.grupoTitulo) {
                    Text(
                        text = stringResource(id = tarifa.grupoTitulo),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                val ultima = index == state.tarifas.lastIndex
                FormTextFieldBrownLeadingIconLabelText(
                    modifier = modifier.fillMaxWidth(),
                    value = tarifa.valor,
                    label = tarifa.chave,
                    onValueChange = { onTarifaChange(tarifa.chave, it) },
                    isError = tarifa.isError,
                    textoErro = R.string.error_valor_invalido,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = if (ultima) ImeAction.Done else ImeAction.Next,
                    ),
                    focusManager = focusManager,
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.AttachMoney, contentDescription = null)
                    },
                )
            }
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
        ContentViagemAreaForm(
            modifier = it,
            state = FormViagemUiState(),
            onEmpresaChange = {},
            onNavioChange = {},
            onTrechoOrigemChange = {},
            onLimparTrechoOrigem = {},
            onTrechoDestinoChange = {},
            onLimparTrechoDestino = {},
            onTarifaChange = { _, _ -> },
        )
    }
}
