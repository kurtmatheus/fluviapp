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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.extensions.keyboardType
import dev.matheus.fluviapp.extensions.visualTransformation
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.GRATUIDADE
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.MEIA
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.REDE
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.SUITE
import dev.matheus.fluviapp.model.mapDescricao
import dev.matheus.fluviapp.ui.components.forms.areas.CommonAreaForm
import dev.matheus.fluviapp.ui.components.forms.buttons.CommonCheckboxField
import dev.matheus.fluviapp.ui.components.forms.dropdowns.DropDownFormField
import dev.matheus.fluviapp.ui.components.forms.dropdowns.FilterDropDownForm
import dev.matheus.fluviapp.ui.components.forms.fields.FormFieldCalendario
import dev.matheus.fluviapp.ui.components.forms.fields.FormTextFieldBrownNoIcon
import dev.matheus.fluviapp.ui.components.texts.SupportingText
import dev.matheus.fluviapp.ui.states.passagem.FormPassageiroUiState
import dev.matheus.fluviapp.ui.states.passagem.FormPassagemUiState

@Composable
fun ContentPassageiroAreaForm(
    modifier: Modifier,
    statePassagem: FormPassagemUiState,
    statePassageiro: FormPassageiroUiState,
    onAcomodacaoChange: (String) -> Unit = {},
    onTipoPassagemChange: (String) -> Unit = {},
    onTipoGratuidadeChange: (String) -> Unit = {},
    onNomePassageiro1Change: (String) -> Unit = {},
    onTipoDocumentoPassageiro1Change: (String) -> Unit = {},
    onClickLimparDocumentoPassageiro1: () -> Unit = {},
    onDocumentoPassageiro1Change: (String) -> Unit = {},
    onDataNascimentoPassageiro1Change: (String) -> Unit = {},
    onCheckPassageiro2: (Boolean) -> Unit = {},
    onNomePassageiro2Change: (String) -> Unit = {},
    onTipoDocumentoPassageiro2Change: (String) -> Unit = {},
    onClickLimparDocumentoPassageiro2: () -> Unit = {},
    onDocumentoPassageiro2Change: (String) -> Unit = {},
    onDataNascimentoPassageiro2Change: (String) -> Unit = {},
    onCheckPassageiro3: (Boolean) -> Unit = {},
    onNomePassageiro3Change: (String) -> Unit = {},
    onTipoDocumentoPassageiro3Change: (String) -> Unit = {},
    onClickLimparDocumentoPassageiro3: () -> Unit = {},
    onDocumentoPassageiro3Change: (String) -> Unit = {},
    onDataNascimentoPassageiro3Change: (String) -> Unit = {},
    focusManager: FocusManager = LocalFocusManager.current,
) {
    DropDownFormField(
        modifier = modifier.fillMaxWidth(),
        listaItens = statePassageiro.listaAcomodacao.mapDescricao(),
        value = statePassageiro.acomodacao,
        onValueChange = onAcomodacaoChange,
        label = R.string.label_acomodacao,
        isError = statePassageiro.isAcomodacaoError
    )

    if (statePassageiro.acomodacao.isNotBlank()) {
        DropDownFormField(
            modifier = modifier,
            listaItens = statePassageiro.listaTipoPassagem.mapDescricao().filter {
                if (!statePassageiro.ehAcomodacaoRede) it != MEIA.name else true
            },
            label = R.string.label_tipo_passagem,
            value = statePassageiro.tipoPassagem,
            onValueChange = onTipoPassagemChange,
            isError = statePassageiro.isTipoPassagemError
        )

        if (statePassageiro.isGratuidade) {
            DropDownFormField(
                modifier = modifier,
                listaItens = statePassageiro.listaTipoGratuidade.mapDescricao(),
                label = R.string.label_tipo_gratuidade,
                value = statePassageiro.tipoGratuidade,
                onValueChange = onTipoGratuidadeChange,
                isError = statePassageiro.isTipoGratuidadeError
            )
        }

        PassageirosFormField(
            modifier = modifier,
            focusManager = focusManager,
            listaTipoDocumentos = statePassagem.listaTipoDocumento.mapDescricao(),
            labelTipoDocumento = R.string.label_documento,
            valueTipoDocumento = statePassageiro.tipoDocumentoPassageiro1,
            onValueChangeTipoDocumento = onTipoDocumentoPassageiro1Change,
            isErrorTipoDocumento = statePassageiro.isTipoDocumentoPassageiro1Error,
            onClickLimparDocumento = onClickLimparDocumentoPassageiro1,
            listaNome = statePassageiro.listaNomePassageiro,
            labelDocumento = R.string.label_numero_documento,
            valueDocumento = statePassageiro.documentoPassageiro1,
            onValueChangeDocumento = onDocumentoPassageiro1Change,
            readOnlyDocumento = statePassageiro.isDocumentoPassageiro1Disabled,
            isErrorDocumento = statePassageiro.isDocumentoPassageiro1Error,
            valueNomePassageiro = statePassageiro.nomePassageiro1,
            onValueChangeNomePassageiro = onNomePassageiro1Change,
            labelNomePassageiro = R.string.label_nome_passageiro,
            isErrorNomePassageiro = statePassageiro.isNomePassageiro1Error,
            valueDataNascimento = statePassageiro.dataNascimentoPassageiro1,
            onValueChangeDataNascimento = onDataNascimentoPassageiro1Change,
            isErrorDataNascimento = statePassageiro.isDataNascimentoPassageiro1Error,
            textoErroData = statePassageiro.textDataNascimentoError
        )

        if (!statePassageiro.ehAcomodacaoRede) {
            CommonCheckboxField(
                modifier = modifier,
                label = R.string.label_checkbox_passageiro2,
                checked = statePassageiro.isPassageiro2Checked,
                onCheck = onCheckPassageiro2
            )
        }

        if (statePassageiro.isPassageiro2Checked) {
            PassageirosFormField(
                modifier = modifier,
                focusManager = focusManager,
                listaTipoDocumentos = statePassagem.listaTipoDocumento.mapDescricao(),
                labelTipoDocumento = R.string.label_documento,
                valueTipoDocumento = statePassageiro.tipoDocumentoPassageiro2,
                onValueChangeTipoDocumento = onTipoDocumentoPassageiro2Change,
                isErrorTipoDocumento = statePassageiro.isTipoDocumentoPassageiro2Error,
                onClickLimparDocumento = onClickLimparDocumentoPassageiro2,
                listaNome = statePassageiro.listaNomePassageiro,
                labelDocumento = R.string.label_numero_documento,
                valueDocumento = statePassageiro.documentoPassageiro2,
                onValueChangeDocumento = onDocumentoPassageiro2Change,
                readOnlyDocumento = statePassageiro.isDocumentoPassageiro2Disabled,
                isErrorDocumento = statePassageiro.isDocumentoPassageiro2Error,
                valueNomePassageiro = statePassageiro.nomePassageiro2,
                onValueChangeNomePassageiro = onNomePassageiro2Change,
                labelNomePassageiro = R.string.label_nome_passageiro,
                isErrorNomePassageiro = statePassageiro.isNomePassageiro2Error,
                valueDataNascimento = statePassageiro.dataNascimentoPassageiro2,
                onValueChangeDataNascimento = onDataNascimentoPassageiro2Change,
                isErrorDataNascimento = statePassageiro.isDataNascimentoPassageiro2Error,
                textoErroData = R.string.error_camp_obrig
            )

            CommonCheckboxField(
                modifier = modifier,
                label = R.string.label_checkbox_passageiro3,
                checked = statePassageiro.isPassageiro3Checked,
                onCheck = onCheckPassageiro3
            )
        }

        if (statePassageiro.isPassageiro3Checked) {
            PassageirosFormField(
                modifier = modifier,
                focusManager = focusManager,
                listaTipoDocumentos = statePassagem.listaTipoDocumento.mapDescricao(),
                labelTipoDocumento = R.string.label_documento,
                valueTipoDocumento = statePassageiro.tipoDocumentoPassageiro3,
                onValueChangeTipoDocumento = onTipoDocumentoPassageiro3Change,
                isErrorTipoDocumento = statePassageiro.isTipoDocumentoPassageiro3Error,
                onClickLimparDocumento = onClickLimparDocumentoPassageiro3,
                listaNome = statePassageiro.listaNomePassageiro,
                labelDocumento = R.string.label_numero_documento,
                valueDocumento = statePassageiro.documentoPassageiro3,
                onValueChangeDocumento = onDocumentoPassageiro3Change,
                readOnlyDocumento = statePassageiro.isDocumentoPassageiro3Disabled,
                isErrorDocumento = statePassageiro.isDocumentoPassageiro3Error,
                valueNomePassageiro = statePassageiro.nomePassageiro3,
                onValueChangeNomePassageiro = onNomePassageiro3Change,
                labelNomePassageiro = R.string.label_nome_passageiro,
                isErrorNomePassageiro = statePassageiro.isNomePassageiro3Error,
                valueDataNascimento = statePassageiro.dataNascimentoPassageiro3,
                onValueChangeDataNascimento = onDataNascimentoPassageiro3Change,
                isErrorDataNascimento = statePassageiro.isDataNascimentoPassageiro3Error,
                textoErroData = R.string.error_camp_obrig
            )
        }
    }

}

@Composable
private fun PassageirosFormField(
    modifier: Modifier,
    focusManager: FocusManager,
    listaTipoDocumentos: List<String>,
    labelTipoDocumento: Int,
    valueTipoDocumento: String,
    onValueChangeTipoDocumento: (String) -> Unit,
    isErrorTipoDocumento: Boolean,
    onClickLimparDocumento: () -> Unit,
    listaNome: List<String>,
    labelDocumento: Int,
    valueDocumento: String,
    onValueChangeDocumento: (String) -> Unit,
    readOnlyDocumento: Boolean,
    isErrorDocumento: Boolean,
    valueNomePassageiro: String,
    onValueChangeNomePassageiro: (String) -> Unit,
    labelNomePassageiro: Int,
    isErrorNomePassageiro: Boolean,
    valueDataNascimento: String,
    onValueChangeDataNascimento: (String) -> Unit,
    isErrorDataNascimento: Boolean,
    textoErroData: Int,
) {

    FilterDropDownForm(
        modifier = modifier.fillMaxWidth(),
        listaItens = listaNome.filter { it.startsWith(valueNomePassageiro) },
        value = valueNomePassageiro,
        onValueChange = onValueChangeNomePassageiro,
        label = labelNomePassageiro,
        isError = isErrorNomePassageiro
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DropDownFormField(
            modifier = modifier,
            listaItens = listaTipoDocumentos,
            label = labelTipoDocumento,
            value = valueTipoDocumento,
            onValueChange = onValueChangeTipoDocumento,
            isError = isErrorTipoDocumento,
            focusManager = focusManager
        )

        IconButton(onClick = onClickLimparDocumento) {
            Icon(
                imageVector = Icons.Filled.Clear,
                contentDescription = stringResource(id = R.string.description_limpar),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }

    FormTextFieldBrownNoIcon(
        modifier = modifier.fillMaxWidth(),
        label = labelDocumento,
        value = valueDocumento,
        readOnly = readOnlyDocumento,
        isError = isErrorDocumento,
        onValueChange = onValueChangeDocumento,
        visualTransformation = visualTransformation(valueTipoDocumento),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            imeAction = ImeAction.Next,
            keyboardType = keyboardType(valueTipoDocumento)
        )
    )
    SupportingText(
        modifier = modifier
            .padding(10.dp, 0.dp)
            .offset(y = (-15).dp),
        text = stringResource(id = R.string.sup_somente_letra_num)
    )

    FormFieldCalendario(
        modifier = modifier,
        focusManager = focusManager,
        value = valueDataNascimento,
        label = R.string.label_data_nascimento,
        onValueChange = onValueChangeDataNascimento,
        isError = isErrorDataNascimento,
        textoErro = textoErroData,
        modoCalendario = false
    )
}

@Preview(showBackground = true)
@Composable
private fun ContentPassageiroAreaComVeiculoFormPreview() {
    CommonAreaForm(
        modifier = Modifier,
        titleArea = R.string.form_area_title_passageiro
    ) {
        ContentPassageiroAreaForm(
            modifier = it,
            statePassagem = FormPassagemUiState(),
            statePassageiro = FormPassageiroUiState(
                acomodacao = SUITE.name
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ContentPassageiroAreaDataErrorFormPreview() {
    CommonAreaForm(
        modifier = Modifier,
        titleArea = R.string.form_area_title_passageiro
    ) {
        ContentPassageiroAreaForm(
            modifier = it,
            statePassagem = FormPassagemUiState(),
            statePassageiro = FormPassageiroUiState(
                isDataNascimentoPassageiro1Error = true,
                textDataNascimentoError = R.string.error_data_crianca,
                acomodacao = REDE.name,
                tipoPassagem = GRATUIDADE.name
            )
        )
    }
}