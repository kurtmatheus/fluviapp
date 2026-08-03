package dev.matheus.fluviapp.ui.components.forms.areas.funcionario

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.operacoes.Funcionario
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
    onEmailChange: (String) -> Unit,
    onLotacaoChange: (String) -> Unit,
    onCargoChange: (String) -> Unit,
) {
    // Dois recortes na mesma tela (ADR-0015 §2.1): a plataforma escolhe a agência; para o supervisor
    // ela é a dele — mostrada, e não escondida, para que o cadastrante veja em nome de quem cadastra.
    if (state.podeEscolherAgencia) {
        FilterDropDownForm(
            modifier = modifier.fillMaxWidth(),
            listaItens = state.listaAgencia,
            label = R.string.label_agencia,
            value = state.agencia,
            isError = state.isAgenciaError,
            onValueChange = onAgenciaChange,
            keyboardType = KeyboardType.Text,
        )
    } else {
        FormTextFieldBrownNoIcon(
            modifier = modifier,
            value = state.agencia,
            onValueChange = {},
            label = R.string.label_agencia,
            readOnly = true,
            enabled = false,
            isError = state.isAgenciaError,
        )
    }

    FormTextFieldBrownNoIcon(
        modifier = modifier,
        value = state.funcionario,
        onValueChange = onFuncionarioChange,
        label = R.string.label_agente,
        isError = state.isFuncionarioError,
        keyboardOptions = KeyboardOptions(KeyboardCapitalization.Characters),
    )

    // O e-mail é o que liga este cadastro à conta do Auth no primeiro acesso (§2.1) — sem
    // capitalização automática, que estragaria a chave.
    FormTextFieldBrownNoIcon(
        modifier = modifier,
        value = state.email,
        onValueChange = onEmailChange,
        label = R.string.label_email,
        isError = state.isEmailError,
        textoErro = R.string.error_email_invalido,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
    )

    DropDownFormField(
        modifier = modifier.fillMaxWidth(),
        listaItens = state.listaMunicipios,
        label = R.string.label_lotacao,
        value = state.lotacao,
        isError = state.isLotacaoError,
        onValueChange = onLotacaoChange,
    )

    // Cargo só aparece para a plataforma (§8.5). Para o supervisor não é campo desabilitado: é campo
    // ausente — ele não decide cargo, então nem a pergunta faz sentido na tela dele.
    if (state.podeDefinirCargo) {
        DropDownFormField(
            modifier = modifier.fillMaxWidth(),
            listaItens = state.listaCargo,
            label = R.string.label_cargo,
            value = state.cargo,
            onValueChange = onCargoChange,
        )
    }
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
            state = FormFuncionarioUiState(
                agencia = "MATRIZ",
                funcionario = "Agente Modelo",
                email = "agente.modelo@fluviapp.com.br",
                lotacao = "PORTO NORTE",
                listaCargo = Funcionario.Cargo.entries.map(Funcionario.Cargo::name),
            ),
            onAgenciaChange = {},
            onFuncionarioChange = {},
            onEmailChange = {},
            onLotacaoChange = {},
            onCargoChange = {},
        )
    }
}