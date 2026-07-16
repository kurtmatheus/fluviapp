package dev.matheus.fluviapp.ui.screens.forms.empresa

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.model.viagem.Empresa
import dev.matheus.fluviapp.ui.components.contents.CommonTopRow
import dev.matheus.fluviapp.ui.components.dialogs.CommonInformativeDialog
import dev.matheus.fluviapp.ui.components.forms.divider.FormDashedDivider
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrown
import dev.matheus.fluviapp.ui.components.texts.TextSubTitleBrownItalic
import dev.matheus.fluviapp.ui.components.texts.TextTitleBrownRegular
import dev.matheus.fluviapp.ui.screens.forms.CommonScreenNoBottom
import dev.matheus.fluviapp.ui.states.PesquisaEmpresaUiState
import dev.matheus.fluviapp.ui.theme.FluviAppTheme

@Composable
fun ResultSearchEmpresaScreen(
    uiState: PesquisaEmpresaUiState,
    onNomeChange: (String) -> Unit = {},
    onClickVoltar: () -> Unit = {},
    onNavegaParaEditor: (String) -> Unit = {},
    onDeletar: (String) -> Unit = {},
) {
    CommonScreenNoBottom(
        titleTopAppBar = R.string.title_top_empresa,
        titleTopContent = R.string.subtitle_pesquisar_empresas,
        isShowRightIcon = false,
        hasRefresh = false,
        isRefreshing = false,
        onClickVoltar = onClickVoltar,
    ) { modifier, titulo ->
        var empresaParaDeletar by remember { mutableStateOf<Empresa?>(null) }

        Column {
            CommonTopRow(modifier = modifier, titulo = titulo)

            // Filtro fixo por nome (startsWith, ignore case) — sempre visível acima da lista.
            OutlinedTextField(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(10.dp, 10.dp),
                value = uiState.nome,
                onValueChange = onNomeChange,
                label = { Text(text = stringResource(R.string.label_nome_empresa)) },
                singleLine = true,
            )
            FormDashedDivider(modifier = modifier.fillMaxWidth())

            LazyColumn {
                items(uiState.resultados) { empresa ->
                    CardResultEmpresa(
                        modifier = modifier,
                        empresa = empresa,
                        onEditar = onNavegaParaEditor,
                        onDeletar = { empresaParaDeletar = it },
                    )
                }
            }
        }

        empresaParaDeletar?.let { empresa ->
            CommonInformativeDialog(
                modifier = Modifier,
                textMensagem = R.string.msg_confirmar_exclusao,
                textConfirm = R.string.btn_excluir,
                textDismiss = R.string.btn_cancelar,
                onConfirm = {
                    onDeletar(empresa.id)
                    empresaParaDeletar = null
                },
                onDismiss = { empresaParaDeletar = null },
            )
        }
    }
}

@Composable
fun CardResultEmpresa(
    modifier: Modifier,
    empresa: Empresa,
    onEditar: (String) -> Unit,
    onDeletar: (Empresa) -> Unit,
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
                TextTitleBrownRegular(text = empresa.nome)
                TextSubTitleBrownItalic(text = empresa.razaoSocial)
                TextRegularBrown(text = empresa.cnpj)
            }

            IconButton(onClick = { onEditar(empresa.id) }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.description_editar),
                )
            }
            IconButton(onClick = { onDeletar(empresa) }) {
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
private fun ResultSearchEmpresaScreenPreview() {
    FluviAppTheme {
        ResultSearchEmpresaScreen(
            uiState = PesquisaEmpresaUiState(
                nome = "",
                resultados = listOf(
                    Empresa("1", "NAVEGA MODELO", "Navega Modelo LTDA", "00.000.000/0001-00", "Cais 1", "9999-0001", ""),
                    Empresa("2", "TRANSPORTE ILHA", "Transporte Ilha SA", "11.111.111/0001-11", "Cais 2", "9999-0002", ""),
                ),
            )
        )
    }
}
