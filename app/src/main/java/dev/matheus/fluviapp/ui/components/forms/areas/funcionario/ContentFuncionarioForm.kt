package dev.matheus.fluviapp.ui.components.forms.areas.funcionario

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.operacoes.Funcionario
import dev.matheus.fluviapp.domain.operacoes.Vinculo
import dev.matheus.fluviapp.ui.components.forms.areas.CommonAreaForm
import dev.matheus.fluviapp.ui.components.forms.dropdowns.DropDownFormField
import dev.matheus.fluviapp.ui.components.forms.fields.FormTextFieldBrownNoIcon
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrown
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
    onAdicionarVinculo: () -> Unit,
    onRemoverVinculo: (String) -> Unit,
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

    // --- Vínculos: onde a pessoa atua, e como em cada lugar (ADR-0016 §6) ---

    // Dois recortes na mesma tela (§2.1): a plataforma escolhe a empresa; para o supervisor ela é a
    // dele — mostrada, e não escondida, para que quem cadastra veja em nome de quem cadastra.
    DropDownFormField(
        modifier = modifier.fillMaxWidth(),
        listaItens = state.empresas.map { it.nome },
        label = R.string.label_empresa,
        value = state.empresaEmEdicao,
        isError = state.isVinculosError,
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
            value = state.cargoEmEdicao,
            onValueChange = onCargoChange,
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = onAdicionarVinculo,
            enabled = state.podeAdicionarVinculo,
        ) {
            Text(text = stringResource(R.string.btn_adicionar_vinculo))
        }
    }

    // A lista do que já foi atribuído. Vazia, ela **diz** que está vazia: um espaço em branco entre o
    // botão e o salvar não distingue "ainda não atribuí" de "esta tela não tem essa parte".
    if (state.vinculosNaTela.isEmpty()) {
        TextRegularBrown(
            modifier = modifier.padding(top = 4.dp),
            text = stringResource(R.string.msg_sem_vinculo),
        )
    } else {
        Column(modifier = modifier.fillMaxWidth()) {
            state.vinculosNaTela.forEach { vinculo ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextRegularBrown(
                        modifier = Modifier.weight(1f),
                        text = listOf(vinculo.empresa, vinculo.cargo)
                            .filter { it.isNotBlank() }
                            .joinToString(" · "),
                    )
                    IconButton(onClick = { onRemoverVinculo(vinculo.empresaId) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.description_deletar),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                HorizontalDivider()
            }
        }
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
                empresaEmEdicao = "Rio Sul",
                vinculos = listOf(Vinculo("e1", Funcionario.Cargo.SUPERVISOR)),
                listaCargo = Funcionario.Cargo.entries.map(Funcionario.Cargo::name),
            ),
            onNomeChange = {},
            onEmailChange = {},
            onEmpresaChange = {},
            onCargoChange = {},
            onAdicionarVinculo = {},
            onRemoverVinculo = {},
        )
    }
}