package dev.matheus.fluviapp.ui.components.forms.areas.localidade

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedButton
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
        //
        // O resultado da consulta aparece como **texto de apoio do próprio campo** — é dele que a
        // mensagem fala, e o Material já reserva essa linha logo abaixo. Antes ela vivia solta entre os
        // campos, o que a fazia parecer comentário da tela em vez de resposta àquele valor.
        FormTextFieldBrownNoIcon(
            modifier = modifier.fillMaxWidth(),
            value = state.codigoIbge,
            label = R.string.label_codigo_ibge,
            onValueChange = onCodigoIbgeChange,
            isError = state.isCodigoIbgeError,
            textoErro = R.string.error_codigo_ibge_invalido,
            textoApoio = textoDeApoioDaBusca(state.buscaIbge),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        )

        BotaoBuscarNoIbge(
            modifier = modifier,
            habilitado = state.podeConsultarIbge && state.buscaIbge != BuscaIbge.Consultando,
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
 * O botão de preencher pelo IBGE.
 *
 * **Contorno, e alinhado à direita** (pedido do analista, rc.2): como `TextButton` ele era texto solto
 * entre dois campos e se lia como rótulo — ninguém identificava que dava para tocar. O contorno é o que
 * diz *isto é acionável*, e a borda direita é onde a ação de uma linha de formulário é procurada.
 *
 * Fica **desabilitado** enquanto o código não tem sete dígitos — consultar meio código é gastar rede para
 * receber "não encontrado" e assustar quem ainda está digitando.
 */
@Composable
private fun BotaoBuscarNoIbge(
    modifier: Modifier,
    habilitado: Boolean,
    onConsultarIbge: () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = onConsultarIbge,
            enabled = habilitado,
        ) {
            Text(text = stringResource(R.string.btn_buscar_no_ibge))
        }
    }
}

/**
 * A mensagem que acompanha o campo do código — **uma linha, quatro sentidos**.
 *
 * O sucesso passou a ser dito (antes o preenchimento acontecia em silêncio, e não havia como saber se o
 * que estava na tela veio do IBGE ou já estava lá). Os dois insucessos continuam distintos: *não é
 * município* fala do código, *não deu para consultar* fala da rede, e só o primeiro pede correção.
 * Nenhum dos dois impede salvar.
 */
@Composable
private fun textoDeApoioDaBusca(busca: BuscaIbge): String? = when (busca) {
    BuscaIbge.Ociosa -> null
    BuscaIbge.Consultando -> stringResource(R.string.msg_consultando_ibge)
    is BuscaIbge.Encontrado -> stringResource(R.string.msg_ibge_encontrado, busca.rotulo)
    BuscaIbge.NaoEncontrado -> stringResource(R.string.msg_ibge_nao_encontrado)
    BuscaIbge.Indisponivel -> stringResource(R.string.msg_ibge_indisponivel)
}