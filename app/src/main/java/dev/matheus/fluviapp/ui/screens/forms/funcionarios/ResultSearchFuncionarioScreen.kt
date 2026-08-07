package dev.matheus.fluviapp.ui.screens.forms.funcionarios

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.components.contents.CommonTopRow
import dev.matheus.fluviapp.ui.components.dialogs.CommonInformativeDialog
import dev.matheus.fluviapp.ui.components.forms.divider.FormDashedDivider
import dev.matheus.fluviapp.ui.components.forms.dropdowns.FilterDropDownForm
import dev.matheus.fluviapp.ui.components.forms.fields.FormTextFieldBrownNoIcon
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrown
import dev.matheus.fluviapp.ui.components.texts.TextTitleBrownRegular
import dev.matheus.fluviapp.ui.screens.forms.CommonScreenNoBottom
import dev.matheus.fluviapp.ui.states.EmpresaOpcao
import dev.matheus.fluviapp.ui.states.FuncionarioResultado
import dev.matheus.fluviapp.ui.states.PesquisaFuncionarioUiState
import dev.matheus.fluviapp.ui.theme.FluviAppTheme

@Composable
fun ResultSearchFuncionarioScreen(
    uiState: PesquisaFuncionarioUiState,
    onNomeChange: (String) -> Unit = {},
    onEmpresaChange: (String) -> Unit = {},
    onClickVoltar: () -> Unit = {},
    onNavegaParaEditor: (String) -> Unit = {},
    onDeletar: (String) -> Unit = {},
) {
    CommonScreenNoBottom(
        titleTopAppBar = R.string.title_top_agente,
        titleTopContent = R.string.subtitle_pesquisar_agentes,
        isShowRightIcon = false,
        hasRefresh = false,
        isRefreshing = false,
        onClickVoltar = onClickVoltar,
    ) { modifier, titulo ->
        // Membro marcado para exclusão (estado local de UI); != null abre o diálogo de confirmação.
        var membroParaDeletar by remember { mutableStateOf<FuncionarioResultado?>(null) }

        Column {
            CommonTopRow(modifier = modifier, titulo = titulo)

            Column(
                modifier = modifier.padding(10.dp, 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FormTextFieldBrownNoIcon(
                    modifier = modifier.fillMaxWidth(),
                    value = uiState.nome,
                    label = R.string.label_agente,
                    onValueChange = onNomeChange,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Search,
                    ),
                )

                // Sem filtro de empresa para quem só enxerga a própria (ADR-0015 §2.2) — a lista já vem
                // recortada, então o campo não teria o que filtrar.
                if (uiState.podeFiltrarPorEmpresa) {
                    FilterDropDownForm(
                        modifier = modifier.fillMaxWidth(),
                        listaItens = uiState.empresas.map { it.nome },
                        label = R.string.label_empresa,
                        value = uiState.empresa,
                        onValueChange = onEmpresaChange,
                        keyboardType = KeyboardType.Text,
                    )
                }
            }
            FormDashedDivider(modifier = modifier.fillMaxWidth())

            LazyColumn {
                items(uiState.resultados) { membro ->
                    CardResultFuncionario(
                        modifier = modifier,
                        membro = membro,
                        onEditar = onNavegaParaEditor,
                        onDeletar = { membroParaDeletar = it },
                        podeDeletar = uiState.podeDeletar,
                    )
                }
            }
        }

        membroParaDeletar?.let { membro ->
            CommonInformativeDialog(
                modifier = Modifier,
                textMensagem = R.string.msg_confirmar_exclusao,
                textConfirm = R.string.btn_excluir,
                textDismiss = R.string.btn_cancelar,
                onConfirm = {
                    onDeletar(membro.id)
                    membroParaDeletar = null
                },
                onDismiss = { membroParaDeletar = null },
            )
        }
    }
}

@Composable
fun CardResultFuncionario(
    modifier: Modifier,
    membro: FuncionarioResultado,
    onEditar: (String) -> Unit,
    onDeletar: (FuncionarioResultado) -> Unit,
    podeDeletar: Boolean = true,
) {
    Column {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(10.dp, 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                modifier = Modifier,
                painter = painterResource(id = R.drawable.ic_user_75),
                contentDescription = stringResource(R.string.description_icon_user),
            )

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                TextTitleBrownRegular(text = membro.nome)
                TextRegularBrown(text = membro.email)
                // Uma linha por vínculo: quem serve a duas empresas aparece com as duas, e é isto que o
                // cadastro antigo não conseguia dizer — havia uma agência por pessoa.
                membro.vinculos.forEach { vinculo ->
                    TextRegularBrown(text = vinculo)
                }
            }

            IconButton(onClick = { onEditar(membro.id) }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.description_editar),
                )
            }
            // Remover membro é da plataforma (§8.5): para o supervisor o botão não existe — a regra do
            // servidor também nega, então mostrá-lo só produziria erro depois do clique.
            if (podeDeletar) {
                IconButton(onClick = { onDeletar(membro) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.description_deletar),
                    )
                }
            }
        }
        HorizontalDivider(modifier = Modifier)
    }
}

@Preview
@Composable
private fun ResultSearchFuncionarioScreenPreview() {
    FluviAppTheme {
        ResultSearchFuncionarioScreen(
            uiState = PesquisaFuncionarioUiState(
                empresas = listOf(EmpresaOpcao("e1", "Navegação Norte")),
                resultados = listOf(
                    FuncionarioResultado(
                        id = "1",
                        nome = "Ana Ribeiro",
                        email = "ana.ribeiro@fluviapp.com.br",
                        vinculos = listOf("Navegação Norte · SUPERVISOR", "Rio Sul · AGENTE"),
                    ),
                    FuncionarioResultado(
                        id = "2",
                        nome = "Bruno Costa",
                        email = "bruno.costa@fluviapp.com.br",
                        vinculos = listOf("Navegação Norte · AGENTE"),
                    ),
                ),
            ),
        )
    }
}