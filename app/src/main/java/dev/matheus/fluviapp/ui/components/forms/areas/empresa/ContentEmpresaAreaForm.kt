package dev.matheus.fluviapp.ui.components.forms.areas.empresa

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.operacoes.Atuacao
import dev.matheus.fluviapp.ui.components.forms.fields.FormTextFieldBrownNoIcon
import dev.matheus.fluviapp.ui.states.FormEmpresaUiState
import dev.matheus.fluviapp.util.visualtransformation.CnpjVisualTransformation

@Composable
fun ContentEmpresaAreaForm(
    modifier: Modifier,
    state: FormEmpresaUiState,
    onNomeChange: (String) -> Unit,
    onRazaoSocialChange: (String) -> Unit,
    onCnpjChange: (String) -> Unit,
    onEnderecoChange: (String) -> Unit,
    onTelefone1Change: (String) -> Unit,
    onTelefone2Change: (String) -> Unit,
    onAtuacaoToggle: (Atuacao) -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FormTextFieldBrownNoIcon(
            modifier = modifier.fillMaxWidth(),
            value = state.nome,
            label = R.string.label_nome_empresa,
            onValueChange = onNomeChange,
            isError = state.isNomeError,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Next,
            ),
        )

        FormTextFieldBrownNoIcon(
            modifier = modifier.fillMaxWidth(),
            value = state.razaoSocial,
            label = R.string.label_razao_social,
            onValueChange = onRazaoSocialChange,
            isError = state.isRazaoSocialError,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Next,
            ),
        )

        FormTextFieldBrownNoIcon(
            modifier = modifier.fillMaxWidth(),
            value = state.cnpj,
            label = R.string.label_cnpj,
            onValueChange = onCnpjChange,
            isError = state.isCnpjError,
            textoErro = R.string.error_cnpj_invalido,
            visualTransformation = CnpjVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
            ),
        )

        FormTextFieldBrownNoIcon(
            modifier = modifier.fillMaxWidth(),
            value = state.endereco,
            label = R.string.label_endereco,
            onValueChange = onEnderecoChange,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
        )

        FormTextFieldBrownNoIcon(
            modifier = modifier.fillMaxWidth(),
            value = state.telefone1,
            label = R.string.label_telefone1,
            onValueChange = onTelefone1Change,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next,
            ),
        )

        FormTextFieldBrownNoIcon(
            modifier = modifier.fillMaxWidth(),
            value = state.telefone2,
            label = R.string.label_telefone2,
            onValueChange = onTelefone2Change,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done,
            ),
        )

        AreaAtuacoes(
            selecionadas = state.atuacoes,
            onAtuacaoToggle = onAtuacaoToggle,
        )
    }
}

/**
 * **O que a parte faz** (ADR-0016 §4). Caixas de seleção, e não escolha única, porque uma empresa
 * exerce várias atuações ao mesmo tempo — é justamente aí que "tipo de empresa" falharia.
 *
 * As dormentes aparecem desabilitadas em vez de sumirem: elas existem no modelo, e escondê-las faria
 * parecer que a plataforma não as conhece (ADR-0016 §5).
 */
@Composable
private fun AreaAtuacoes(
    selecionadas: Set<Atuacao>,
    onAtuacaoToggle: (Atuacao) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.label_atuacoes),
            style = MaterialTheme.typography.titleSmall,
        )

        Atuacao.entries.forEach { atuacao ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = atuacao in selecionadas,
                    onCheckedChange = { onAtuacaoToggle(atuacao) },
                    enabled = atuacao.operante,
                )
                Text(
                    text = atuacao.rotulo,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
