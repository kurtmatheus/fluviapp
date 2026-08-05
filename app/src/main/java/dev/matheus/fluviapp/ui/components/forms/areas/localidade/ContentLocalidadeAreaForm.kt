package dev.matheus.fluviapp.ui.components.forms.areas.localidade

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.localidade.Uf
import dev.matheus.fluviapp.ui.components.forms.dropdowns.DropDownFormField
import dev.matheus.fluviapp.ui.components.forms.fields.FormTextFieldBrownNoIcon
import dev.matheus.fluviapp.ui.states.BuscaIbge
import dev.matheus.fluviapp.ui.states.FormLocalidadeUiState

@Composable
fun ContentLocalidadeAreaForm(
    modifier: Modifier,
    state: FormLocalidadeUiState,
    onCodigoIbgeChange: (String) -> Unit,
    onMunicipioChange: (String) -> Unit,
    onUfChange: (String) -> Unit,
    onConsultarIbge: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // O código vem PRIMEIRO porque é ele que preenche o resto. A ordem dos campos é a ordem do
        // trabalho: informa-se a chave, busca-se, confere-se o que veio.
        FormTextFieldBrownNoIcon(
            modifier = modifier.fillMaxWidth(),
            value = state.codigoIbge,
            label = R.string.label_codigo_ibge,
            onValueChange = onCodigoIbgeChange,
            isError = state.isCodigoIbgeError,
            textoErro = R.string.error_codigo_ibge_invalido,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        )

        AreaBuscaIbge(
            modifier = modifier,
            state = state,
            onConsultarIbge = onConsultarIbge,
        )

        FormTextFieldBrownNoIcon(
            modifier = modifier.fillMaxWidth(),
            value = state.municipio,
            label = R.string.label_municipio,
            onValueChange = onMunicipioChange,
            isError = state.isMunicipioError,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
        )

        // A UF é lista fechada (27 valores por constituição): dropdown, não texto — não há grafia a errar.
        DropDownFormField(
            listaItens = Uf.entries.map { it.rotulo() },
            label = R.string.label_uf,
            modifier = modifier.fillMaxWidth(),
            value = state.uf?.rotulo().orEmpty(),
            onValueChange = onUfChange,
            isError = state.isUfError,
        )
    }
}

/**
 * O botão de preencher pelo IBGE e o que ele tem a dizer.
 *
 * Fica **desabilitado** enquanto o código não tem sete dígitos — consultar meio código é gastar rede para
 * receber "não encontrado" e assustar quem ainda está digitando.
 *
 * As duas mensagens de insucesso são distintas de propósito: *não é município* fala do código,
 * *não deu para consultar* fala da rede, e só a primeira pede correção. Nenhuma das duas impede salvar.
 */
@Composable
private fun AreaBuscaIbge(
    modifier: Modifier,
    state: FormLocalidadeUiState,
    onConsultarIbge: () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = onConsultarIbge,
                enabled = state.podeConsultarIbge && state.buscaIbge != BuscaIbge.Consultando,
            ) {
                Text(text = stringResource(R.string.btn_buscar_no_ibge))
            }

            if (state.buscaIbge == BuscaIbge.Consultando) {
                Text(
                    text = stringResource(R.string.msg_consultando_ibge),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        when (state.buscaIbge) {
            BuscaIbge.NaoEncontrado -> Text(
                text = stringResource(R.string.msg_ibge_nao_encontrado),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )

            BuscaIbge.Indisponivel -> Text(
                text = stringResource(R.string.msg_ibge_indisponivel),
                style = MaterialTheme.typography.bodySmall,
            )

            BuscaIbge.Consultando, BuscaIbge.Ociosa -> Unit
        }
    }
}