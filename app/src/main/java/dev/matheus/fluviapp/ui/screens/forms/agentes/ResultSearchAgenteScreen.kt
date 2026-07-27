package dev.matheus.fluviapp.ui.screens.forms.agentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.model.operacoes.Funcionario
import dev.matheus.fluviapp.sampledata.listaFuncionarioSample
import dev.matheus.fluviapp.ui.components.contents.CommonTopRow
import dev.matheus.fluviapp.ui.components.dialogs.CommonInformativeDialog
import dev.matheus.fluviapp.ui.components.forms.divider.FormDashedDivider
import dev.matheus.fluviapp.ui.components.forms.dropdowns.FilterDropDownForm
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrown
import dev.matheus.fluviapp.ui.components.texts.TextSubTitleBrownItalic
import dev.matheus.fluviapp.ui.components.texts.TextTitleBrownRegular
import dev.matheus.fluviapp.ui.screens.forms.CommonScreenNoBottom
import dev.matheus.fluviapp.ui.states.PesquisaAgenteUiState
import dev.matheus.fluviapp.ui.theme.FluviAppTheme

@Composable
fun ResultSearchAgenteScreen(
    uiState: PesquisaAgenteUiState,
    onAgenciaChange: (String) -> Unit = {},
    onLotacaoChange: (String) -> Unit = {},
    onClickVoltar: () -> Unit = {},
    onNavegaParaEditor: (String) -> Unit = {},
    onDeletar: (String) -> Unit = {},
) {
    // sem ícone na top bar (isShowRightIcon = false); filtros fixos acima da lista.
    CommonScreenNoBottom(
        titleTopAppBar = R.string.title_top_agente,
        titleTopContent = R.string.subtitle_pesquisar_agentes,
        isShowRightIcon = false,
        hasRefresh = false,
        isRefreshing = false,
        onClickVoltar = onClickVoltar,
    ) { modifier, titulo ->
        // agente marcado para deleção (estado local de UI); != null abre o diálogo de confirmação.
        var agenteParaDeletar by remember { mutableStateOf<Funcionario?>(null) }

        Column {
            CommonTopRow(modifier = modifier, titulo = titulo)

            Column(
                modifier = modifier.padding(10.dp, 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FilterDropDownForm(
                    modifier = modifier.fillMaxWidth(),
                    listaItens = uiState.listaAgencia,
                    label = R.string.label_agencia,
                    value = uiState.agencia,
                    onValueChange = onAgenciaChange,
                    keyboardType = KeyboardType.Text,
                )
                FilterDropDownForm(
                    modifier = modifier.fillMaxWidth(),
                    listaItens = uiState.listaLotacao,
                    label = R.string.label_lotacao,
                    value = uiState.lotacao,
                    onValueChange = onLotacaoChange,
                    keyboardType = KeyboardType.Text,
                )
            }
            FormDashedDivider(modifier = modifier.fillMaxWidth())

            LazyColumn {
                items(uiState.resultados) { agente ->
                    CardResultAgente(
                        modifier = modifier,
                        agente = agente,
                        onEditar = onNavegaParaEditor,
                        onDeletar = { agenteParaDeletar = it },
                    )
                }
            }
        }

        agenteParaDeletar?.let { agente ->
            CommonInformativeDialog(
                modifier = Modifier,
                textMensagem = R.string.msg_confirmar_exclusao,
                textConfirm = R.string.btn_excluir,
                textDismiss = R.string.btn_cancelar,
                onConfirm = {
                    onDeletar(agente.id)
                    agenteParaDeletar = null
                },
                onDismiss = { agenteParaDeletar = null },
            )
        }
    }
}

@Composable
fun CardResultAgente(
    modifier: Modifier,
    agente: Funcionario,
    onEditar: (String) -> Unit,
    onDeletar: (Funcionario) -> Unit,
) {
    Column {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(10.dp, 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                modifier = modifier,
                painter = painterResource(id = R.drawable.ic_user_75),
                contentDescription = stringResource(R.string.description_icon_user),
            )

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                TextSubTitleBrownItalic(text = agente.agencia)
                TextTitleBrownRegular(text = agente.descricaoNome)
                TextRegularBrown(text = agente.lotacao)
            }

            IconButton(onClick = { onEditar(agente.id) }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.description_editar),
                )
            }
            IconButton(onClick = { onDeletar(agente) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.description_deletar),
                )
            }
        }
        HorizontalDivider(modifier = Modifier)
    }
}

@Preview
@Composable
private fun ResultSearchAgenteScreenPreview() {
    FluviAppTheme {
        ResultSearchAgenteScreen(
            uiState = PesquisaAgenteUiState(
                agencia = "AGENCIA LITORAL",
                listaAgencia = listaFuncionarioSample.map { it.agencia },
                listaLotacao = listaFuncionarioSample.map { it.lotacao }.distinct(),
                resultados = listaFuncionarioSample.filter { it.agencia == "AGENCIA LITORAL" },
            )
        )
    }
}
