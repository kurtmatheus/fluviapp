package dev.matheus.fluviapp.ui.screens.forms.embarcacao

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.components.contents.CommonTopRow
import dev.matheus.fluviapp.ui.components.dialogs.CommonInformativeDialog
import dev.matheus.fluviapp.ui.components.forms.divider.FormDashedDivider
import dev.matheus.fluviapp.ui.components.forms.dropdowns.FilterDropDownForm
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrown
import dev.matheus.fluviapp.ui.components.texts.TextTitleBrownRegular
import dev.matheus.fluviapp.ui.screens.forms.CommonScreenNoBottom
import dev.matheus.fluviapp.ui.states.EmbarcacaoResultado
import dev.matheus.fluviapp.ui.states.PesquisaEmbarcacaoUiState
import dev.matheus.fluviapp.ui.theme.FluviAppTheme

@Composable
fun ResultSearchEmbarcacaoScreen(
    uiState: PesquisaEmbarcacaoUiState,
    onEmpresaChange: (String) -> Unit = {},
    onClickVoltar: () -> Unit = {},
    onNavegaParaEditor: (String) -> Unit = {},
    onDeletar: (String) -> Unit = {},
) {
    CommonScreenNoBottom(
        titleTopAppBar = R.string.title_top_embarcacao,
        titleTopContent = R.string.subtitle_pesquisar_embarcacoes,
        isShowRightIcon = false,
        hasRefresh = false,
        isRefreshing = false,
        onClickVoltar = onClickVoltar,
    ) { modifier, titulo ->
        var embarcacaoParaDeletar by remember { mutableStateOf<EmbarcacaoResultado?>(null) }

        Column {
            CommonTopRow(modifier = modifier, titulo = titulo)

            // Filtro único e fixo: empresa (dropdown).
            Column(
                modifier = modifier.padding(10.dp, 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FilterDropDownForm(
                    modifier = modifier.fillMaxWidth(),
                    listaItens = uiState.listaEmpresas,
                    label = R.string.label_empresa,
                    value = uiState.empresa,
                    onValueChange = onEmpresaChange,
                    keyboardType = KeyboardType.Text,
                )
            }
            FormDashedDivider(modifier = modifier.fillMaxWidth())

            LazyColumn {
                items(uiState.resultados) { embarcacao ->
                    CardResultEmbarcacao(
                        modifier = modifier,
                        embarcacao = embarcacao,
                        onEditar = onNavegaParaEditor,
                        onDeletar = { embarcacaoParaDeletar = it },
                    )
                }
            }
        }

        embarcacaoParaDeletar?.let { embarcacao ->
            CommonInformativeDialog(
                modifier = Modifier,
                textMensagem = R.string.msg_confirmar_exclusao,
                textConfirm = R.string.btn_excluir,
                textDismiss = R.string.btn_cancelar,
                onConfirm = {
                    onDeletar(embarcacao.id)
                    embarcacaoParaDeletar = null
                },
                onDismiss = { embarcacaoParaDeletar = null },
            )
        }
    }
}

@Composable
fun CardResultEmbarcacao(
    modifier: Modifier,
    embarcacao: EmbarcacaoResultado,
    onEditar: (String) -> Unit,
    onDeletar: (EmbarcacaoResultado) -> Unit,
) {
    Column {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(10.dp, 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                TextTitleBrownRegular(text = embarcacao.nome)
                TextRegularBrown(text = embarcacao.empresaNome)
            }

            IconButton(onClick = { onEditar(embarcacao.id) }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.description_editar),
                )
            }
            IconButton(onClick = { onDeletar(embarcacao) }) {
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
private fun ResultSearchEmbarcacaoScreenPreview() {
    FluviAppTheme {
        ResultSearchEmbarcacaoScreen(
            uiState = PesquisaEmbarcacaoUiState(
                empresa = "",
                listaEmpresas = listOf("NAVEGA MODELO", "TRANSPORTE ILHA"),
                resultados = listOf(
                    EmbarcacaoResultado("1", "F/B Modelo", "NAVEGA MODELO"),
                    EmbarcacaoResultado("2", "F/B Litoral", "TRANSPORTE ILHA"),
                ),
            )
        )
    }
}
