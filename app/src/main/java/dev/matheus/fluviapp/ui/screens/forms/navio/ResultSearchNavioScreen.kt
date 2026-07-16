package dev.matheus.fluviapp.ui.screens.forms.navio

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
import dev.matheus.fluviapp.ui.states.NavioResultado
import dev.matheus.fluviapp.ui.states.PesquisaNavioUiState
import dev.matheus.fluviapp.ui.theme.FluviAppTheme

@Composable
fun ResultSearchNavioScreen(
    uiState: PesquisaNavioUiState,
    onEmpresaChange: (String) -> Unit = {},
    onClickVoltar: () -> Unit = {},
    onNavegaParaEditor: (String) -> Unit = {},
    onDeletar: (String) -> Unit = {},
) {
    CommonScreenNoBottom(
        titleTopAppBar = R.string.title_top_navio,
        titleTopContent = R.string.subtitle_pesquisar_navios,
        isShowRightIcon = false,
        hasRefresh = false,
        isRefreshing = false,
        onClickVoltar = onClickVoltar,
    ) { modifier, titulo ->
        var navioParaDeletar by remember { mutableStateOf<NavioResultado?>(null) }

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
                items(uiState.resultados) { navio ->
                    CardResultNavio(
                        modifier = modifier,
                        navio = navio,
                        onEditar = onNavegaParaEditor,
                        onDeletar = { navioParaDeletar = it },
                    )
                }
            }
        }

        navioParaDeletar?.let { navio ->
            CommonInformativeDialog(
                modifier = Modifier,
                textMensagem = R.string.msg_confirmar_exclusao,
                textConfirm = R.string.btn_excluir,
                textDismiss = R.string.btn_cancelar,
                onConfirm = {
                    onDeletar(navio.id)
                    navioParaDeletar = null
                },
                onDismiss = { navioParaDeletar = null },
            )
        }
    }
}

@Composable
fun CardResultNavio(
    modifier: Modifier,
    navio: NavioResultado,
    onEditar: (String) -> Unit,
    onDeletar: (NavioResultado) -> Unit,
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
                TextTitleBrownRegular(text = navio.nome)
                TextRegularBrown(text = navio.empresaNome)
            }

            IconButton(onClick = { onEditar(navio.id) }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.description_editar),
                )
            }
            IconButton(onClick = { onDeletar(navio) }) {
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
private fun ResultSearchNavioScreenPreview() {
    FluviAppTheme {
        ResultSearchNavioScreen(
            uiState = PesquisaNavioUiState(
                empresa = "",
                listaEmpresas = listOf("NAVEGA MODELO", "TRANSPORTE ILHA"),
                resultados = listOf(
                    NavioResultado("1", "F/B Modelo", "NAVEGA MODELO"),
                    NavioResultado("2", "F/B Litoral", "TRANSPORTE ILHA"),
                ),
            )
        )
    }
}
