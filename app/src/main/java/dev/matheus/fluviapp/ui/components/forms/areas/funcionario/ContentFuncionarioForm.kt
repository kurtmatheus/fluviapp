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
import dev.matheus.fluviapp.ui.components.forms.fields.FormTextFieldBrownNoIcon
import dev.matheus.fluviapp.ui.states.EmpresaOpcao
import dev.matheus.fluviapp.ui.states.FormFuncionarioUiState

@Composable
fun ContentFuncionarioForm(
    modifier: Modifier,
    state: FormFuncionarioUiState,
    onNomeChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onEmpresaChange: (String) -> Unit,
    onCargoChange: (String) -> Unit,
) {
    FormTextFieldBrownNoIcon(
        modifier = modifier,
        value = state.nome,
        onValueChange = onNomeChange,
        label = R.string.label_agente,
        isError = state.isNomeError,
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

    // --- O vínculo: onde a pessoa atua, e como (ADR-0016 §6, ADR-0032 Q2) ---
    //
    // Os dois seletores **são** o vínculo. Antes eles montavam um candidato, que um botão acrescentava a
    // uma lista abaixo — e a lista, o botão de acrescentar e o de remover saíram juntos quando o domínio
    // deixou de admitir o segundo vínculo. O que sobrou é o que a pergunta sempre foi.

    // Dois recortes na mesma tela (§2.1): a plataforma escolhe a empresa; para o supervisor ela é a
    // dele — mostrada, e não escondida, para que quem cadastra veja em nome de quem cadastra.
    DropDownFormField(
        modifier = modifier.fillMaxWidth(),
        listaItens = state.empresas.map { it.nome },
        label = R.string.label_empresa,
        value = state.empresa,
        isError = state.isEmpresaError,
        readOnly = !state.podeEscolherEmpresa,
        onValueChange = onEmpresaChange,
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
        titleArea = R.string.subtitle_cadastrar_novo_agente,
    ) {
        ContentFuncionarioForm(
            modifier = it,
            state = FormFuncionarioUiState(
                nome = "Agente Modelo",
                email = "agente.modelo@fluviapp.com.br",
                empresas = listOf(EmpresaOpcao("e1", "Navegação Norte"), EmpresaOpcao("e2", "Rio Sul")),
                empresa = "Rio Sul",
                cargo = Funcionario.Cargo.SUPERVISOR.name,
                listaCargo = Funcionario.Cargo.entries.map(Funcionario.Cargo::name),
            ),
            onNomeChange = {},
            onEmailChange = {},
            onEmpresaChange = {},
            onCargoChange = {},
        )
    }
}