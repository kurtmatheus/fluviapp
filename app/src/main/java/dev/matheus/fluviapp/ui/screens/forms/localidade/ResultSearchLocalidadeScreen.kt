package dev.matheus.fluviapp.ui.screens.forms.localidade

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.components.contents.CommonTopRow
import dev.matheus.fluviapp.ui.components.dialogs.CommonInformativeDialog
import dev.matheus.fluviapp.ui.components.forms.divider.FormDashedDivider
import dev.matheus.fluviapp.ui.components.forms.fields.FormTextFieldBrownNoIcon
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrown
import dev.matheus.fluviapp.ui.components.texts.TextTitleBrownRegular
import dev.matheus.fluviapp.ui.screens.forms.CommonScreenNoBottom
import dev.matheus.fluviapp.ui.states.LocalidadeResultado
import dev.matheus.fluviapp.ui.states.PesquisaLocalidadeUiState
import dev.matheus.fluviapp.ui.theme.FluviAppTheme

@Composable
fun ResultSearchLocalidadeScreen(
    uiState: PesquisaLocalidadeUiState,
    onMunicipioChange: (String) -> Unit = {},
    onClickVoltar: () -> Unit = {},
    onNavegaParaEditor: (String) -> Unit = {},
    onDeletar: (String) -> Unit = {},
) {
    CommonScreenNoBottom(
        titleTopAppBar = R.string.title_top_localidade,
        titleTopContent = R.string.subtitle_pesquisar_localidades,
        isShowRightIcon = false,
        hasRefresh = false,
        isRefreshing = false,
        onClickVoltar = onClickVoltar,
    ) { modifier, titulo ->
        var localidadeParaExcluir by remember { mutableStateOf<LocalidadeResultado?>(null) }

        Column {
            CommonTopRow(modifier = modifier, titulo = titulo)

            Column(
                modifier = modifier.padding(10.dp, 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FormTextFieldBrownNoIcon(
                    modifier = modifier.fillMaxWidth(),
                    value = uiState.municipio,
                    label = R.string.label_municipio,
                    onValueChange = onMunicipioChange,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Search,
                    ),
                )
            }
            FormDashedDivider(modifier = modifier.fillMaxWidth())

            LazyColumn {
                items(uiState.resultados) { localidade ->
                    CardResultLocalidade(
                        modifier = modifier,
                        localidade = localidade,
                        onEditar = onNavegaParaEditor,
                        onExcluir = { localidadeParaExcluir = it },
                    )
                }
            }
        }

        localidadeParaExcluir?.let { localidade ->
            CommonInformativeDialog(
                modifier = Modifier,
                // A mensagem é a de exclusão comum: para quem cadastra, o gesto é o mesmo. O que muda é
                // o que o repositório faz com ele — inativar, não apagar (ADR-0016 §5).
                textMensagem = R.string.msg_confirmar_exclusao,
                textConfirm = R.string.btn_excluir,
                textDismiss = R.string.btn_cancelar,
                onConfirm = {
                    onDeletar(localidade.id)
                    localidadeParaExcluir = null
                },
                onDismiss = { localidadeParaExcluir = null },
            )
        }
    }
}

@Composable
fun CardResultLocalidade(
    modifier: Modifier,
    localidade: LocalidadeResultado,
    onEditar: (String) -> Unit,
    onExcluir: (LocalidadeResultado) -> Unit,
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
                TextTitleBrownRegular(text = localidade.rotulo)
                // O código aparece porque é a chave natural: é por ele que se confere se duas linhas
                // parecidas são a mesma cidade ou duas homônimas de estados diferentes.
                TextRegularBrown(text = localidade.codigoIbge)
            }

            IconButton(onClick = { onEditar(localidade.id) }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.description_editar),
                )
            }
            IconButton(onClick = { onExcluir(localidade) }) {
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
private fun ResultSearchLocalidadeScreenPreview() {
    FluviAppTheme {
        ResultSearchLocalidadeScreen(
            uiState = PesquisaLocalidadeUiState(
                resultados = listOf(
                    LocalidadeResultado("1", "Belém/PA", "1501402"),
                    LocalidadeResultado("2", "Parintins/AM", "1303205"),
                ),
            )
        )
    }
}