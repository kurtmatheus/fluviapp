package dev.matheus.fluviapp.ui.components.forms.areas.passagem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.cadastro.constantes.Constante.Descricao.MOTO
import dev.matheus.fluviapp.extensions.keyboardType
import dev.matheus.fluviapp.extensions.visualTransformation
import dev.matheus.fluviapp.domain.mapDescricao
import dev.matheus.fluviapp.ui.components.forms.areas.CommonAreaForm
import dev.matheus.fluviapp.ui.components.forms.dropdowns.DropDownFormField
import dev.matheus.fluviapp.ui.components.forms.dropdowns.FilterDropDownForm
import dev.matheus.fluviapp.ui.components.forms.fields.FormTextFieldBrownNoIcon
import dev.matheus.fluviapp.ui.components.texts.SupportingText
import dev.matheus.fluviapp.ui.states.passagem.FormPassagemUiState
import dev.matheus.fluviapp.ui.states.passagem.FormVeiculoUiState

@Composable
fun ContentAreaVeiculoForm(
    modifier: Modifier,
    statePassagem: FormPassagemUiState,
    stateVeiculo: FormVeiculoUiState,
    onNomeResponsavelRetiradaChange: (String) -> Unit = {},
    onTipoDocumentoResponsavelRetiradaChange: (String) -> Unit = {},
    onClickLimparTipoDocumentoResponsavelRetirada: () -> Unit = {},
    onDocumentoResponsavelRetiradaChange: (String) -> Unit = {},
    onTipoVeiculoChange: (String) -> Unit = {},
    onModeloVeiculoChange: (String) -> Unit = {},
    onPlacaVeiculoChange: (String) -> Unit = {},
    onCorVeiculoChange: (String) -> Unit = {},
    onCilindradaChange: (String) -> Unit = {},
    focusManager: FocusManager = LocalFocusManager.current
) {
    FilterDropDownForm(
        modifier = modifier.fillMaxWidth(),
        listaItens = stateVeiculo.listaNomeResponsavelRetirada.filter { it.startsWith(stateVeiculo.nomeResponsavelRetirada) },
        value = stateVeiculo.nomeResponsavelRetirada,
        onValueChange = onNomeResponsavelRetiradaChange,
        label = R.string.label_nome_responsavel,
        isError = stateVeiculo.isNomeResponsavelRetiradaError
    )



    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DropDownFormField(
            listaItens = statePassagem.listaTipoDocumento.mapDescricao(),
            label = R.string.label_documento_responsavel,
            modifier = modifier,
            value = stateVeiculo.tipoDocumentoResponsavelRetirada,
            onValueChange = onTipoDocumentoResponsavelRetiradaChange,
            isError = stateVeiculo.isTipoDocumentoResponsavelRetiradaError,
            focusManager = focusManager
        )

        IconButton(onClick = onClickLimparTipoDocumentoResponsavelRetirada) {
            Icon(
                imageVector = Icons.Filled.Clear,
                contentDescription = stringResource(id = R.string.description_limpar),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }

    FormTextFieldBrownNoIcon(
        modifier = modifier.fillMaxWidth(),
        label = R.string.label_numero_documento_responsavel,
        value = stateVeiculo.documentoResponsavelRetirada,
        readOnly = stateVeiculo.isDocumentoResponsavelRetiradaReadOnly,
        isError = stateVeiculo.isDocumentoResponsavelRetiradaError,
        onValueChange = onDocumentoResponsavelRetiradaChange,
        visualTransformation = visualTransformation(stateVeiculo.tipoDocumentoResponsavelRetirada),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            imeAction = ImeAction.Next,
            keyboardType = keyboardType(stateVeiculo.tipoDocumentoResponsavelRetirada)
        )
    )
    SupportingText(
        modifier = modifier
            .padding(10.dp, 0.dp)
            .offset(y = (-15).dp),
        text = stringResource(id = R.string.sup_somente_letra_num)
    )

    DropDownFormField(
        listaItens = stateVeiculo.listaTipoVeiculo.mapDescricao(),
        label = R.string.label_tipo_veiculo,
        modifier = modifier.fillMaxWidth(),
        value = stateVeiculo.tipoVeiculo,
        onValueChange = onTipoVeiculoChange,
        isError = stateVeiculo.isTipoVeiculoError,
        focusManager = focusManager
    )

    // Cilindrada só para moto (ADR-0013): numérica, filtro de dígito no handler. Alimenta a tarifa por cc.
    if (stateVeiculo.tipoVeiculo == MOTO.name) {
        FormTextFieldBrownNoIcon(
            modifier = modifier.fillMaxWidth(),
            value = stateVeiculo.cilindrada,
            onValueChange = onCilindradaChange,
            label = R.string.label_cilindrada,
            isError = stateVeiculo.isCilindradaError,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, keyboardType = KeyboardType.Number),
        )
    }

    FormTextFieldBrownNoIcon(
        modifier = modifier.fillMaxWidth(),
        value = stateVeiculo.modeloVeiculo,
        onValueChange = onModeloVeiculoChange,
        label = R.string.label_modelo_veiculo,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Next,
            capitalization = KeyboardCapitalization.Characters
        ),
        isError = stateVeiculo.isModeloVeiculoError
    )

    FormTextFieldBrownNoIcon(
        modifier = modifier.fillMaxWidth(),
        value = stateVeiculo.corVeiculo,
        onValueChange = onCorVeiculoChange,
        label = R.string.label_cor_veículo,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Next,
            capitalization = KeyboardCapitalization.Characters
        ),
        isError = stateVeiculo.isCorVeiculoError
    )

    FormTextFieldBrownNoIcon(
        modifier = modifier.fillMaxWidth(),
        value = stateVeiculo.placaVeiculo,
        onValueChange = onPlacaVeiculoChange,
        label = R.string.label_placa_veículo,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Next,
            capitalization = KeyboardCapitalization.Characters
        ),
        isError = stateVeiculo.isPlacaVeiculoError
    )
}

@Preview(showBackground = true)
@Composable
private fun ContentAreaVeiculoPreview() {
    CommonAreaForm(
        modifier = Modifier,
        titleArea = R.string.form_area_title_veiculo
    ) {
        ContentAreaVeiculoForm(
            modifier = Modifier,
            statePassagem = FormPassagemUiState(),
            stateVeiculo = FormVeiculoUiState()
        )
    }
}