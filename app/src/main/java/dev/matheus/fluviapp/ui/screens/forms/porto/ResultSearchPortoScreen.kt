package dev.matheus.fluviapp.ui.screens.forms.porto

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
import dev.matheus.fluviapp.ui.states.PesquisaPortoUiState
import dev.matheus.fluviapp.ui.states.PortoResultado
import dev.matheus.fluviapp.ui.theme.FluviAppTheme

@Composable
fun ResultSearchPortoScreen(
    uiState: PesquisaPortoUiState,
    onNomeChange: (String) -> Unit = {},
    onClickVoltar: () -> Unit = {},
    onNavegaParaEditor: (String) -> Unit = {},
    onDeletar: (String) -> Unit = {},
) {
    CommonScreenNoBottom(
        titleTopAppBar = R.string.title_top_porto,
        titleTopContent = R.string.subtitle_pesquisar_portos,
        isShowRightIcon = false,
        hasRefresh = false,
        isRefreshing = false,
        onClickVoltar = onClickVoltar,
    ) { modifier, titulo ->
        var portoParaExcluir by remember { mutableStateOf<PortoResultado?>(null) }

        Column {
            CommonTopRow(modifier = modifier, titulo = titulo)

            Column(
                modifier = modifier.padding(10.dp, 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FormTextFieldBrownNoIcon(
                    modifier = modifier.fillMaxWidth(),
                    value = uiState.nome,
                    label = R.string.label_nome_porto,
                    onValueChange = onNomeChange,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Search,
                    ),
                )
            }
            FormDashedDivider(modifier = modifier.fillMaxWidth())

            LazyColumn {
                items(uiState.resultados) { porto ->
                    CardResultPorto(
                        modifier = modifier,
                        porto = porto,
                        onEditar = onNavegaParaEditor,
                        onExcluir = { portoParaExcluir = it },
                    )
                }
            }
        }

        portoParaExcluir?.let { porto ->
            CommonInformativeDialog(
                modifier = Modifier,
                // O gesto é o mesmo das outras seções; o que muda é o que o repositório faz com ele —
                // inativar, não apagar (ADR-0016 §5).
                textMensagem = R.string.msg_confirmar_exclusao,
                textConfirm = R.string.btn_excluir,
                textDismiss = R.string.btn_cancelar,
                onConfirm = {
                    onDeletar(porto.id)
                    portoParaExcluir = null
                },
                onDismiss = { portoParaExcluir = null },
            )
        }
    }
}

@Composable
fun CardResultPorto(
    modifier: Modifier,
    porto: PortoResultado,
    onEditar: (String) -> Unit,
    onExcluir: (PortoResultado) -> Unit,
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
                TextTitleBrownRegular(text = porto.nome)
                // A localidade é a segunda linha porque é o que distingue homônimos — "Porto Central" só
                // quer dizer alguma coisa depois de "Belém/PA". Em branco quando não houve o que
                // resolver: linha ausente, não linha inventada.
                if (porto.rotuloLocalidade.isNotBlank()) {
                    TextRegularBrown(text = porto.rotuloLocalidade)
                }
            }

            IconButton(onClick = { onEditar(porto.id) }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.description_editar),
                )
            }
            IconButton(onClick = { onExcluir(porto) }) {
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
private fun ResultSearchPortoScreenPreview() {
    FluviAppTheme {
        ResultSearchPortoScreen(
            uiState = PesquisaPortoUiState(
                resultados = listOf(
                    PortoResultado("1", "Porto de Val-de-Cães", "Belém/PA"),
                    PortoResultado("2", "Porto de Parintins", "Parintins/AM"),
                ),
            )
        )
    }
}