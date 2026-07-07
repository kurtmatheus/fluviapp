package br.com.gruponaveg.ui.screens.forms.passagem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.gruponaveg.R
import br.com.gruponaveg.model.screendata.DadosPassagem
import br.com.gruponaveg.sampledata.dadosPassagemSample
import br.com.gruponaveg.ui.components.cards.PassagemPreviewCard
import br.com.gruponaveg.ui.components.contents.CommonTopRow
import br.com.gruponaveg.ui.components.forms.fields.FormTextFieldBrownNoIcon
import br.com.gruponaveg.ui.components.texts.TextRegularBrownItalic
import br.com.gruponaveg.ui.screens.forms.CommonScreenNoBottom
import br.com.gruponaveg.ui.states.passagem.PesquisarPassagemUiState

@Composable
fun ResultadosPassagemSearchScreen(
    state: PesquisarPassagemUiState,
    onClickVoltar: () -> Unit = {},
    onClickSelecionado: (String) -> Unit = {},
    onClickRightIcon: () -> Unit = {},
) {
    CommonScreenNoBottom(
        titleTopAppBar = R.string.title_top_passagem,
        titleTopContent = R.string.subtitle_resultados,
        isShowRightIcon = true,
        hasRefresh = false,
        isRefreshing = false,
        onClickRightIcon = onClickRightIcon,
        onClickVoltar = onClickVoltar,
    ) { modifier, title ->
        Column {
            if (state.isShowBarraPesquisa) {
                FormTextFieldBrownNoIcon(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(5.dp, 0.dp),
                    value = state.pesquisa,
                    onValueChange = state.onPesquisaChange,
                    label = R.string.label_search_pass
                )
            }

            CommonTopRow(modifier = modifier, titulo = title)

            if (state.listaResultadoPassagens.isNotEmpty()) {
                LazyColumn {
                    items(state.listaResultadoPassagens.filter {
                        filtrarPor(it, state)
                    }) {
                        PassagemPreviewCard(
                            modifier = modifier,
                            dadosPassagem = it,
                            onClick = { onClickSelecionado(it.idPassagem) }
                        )
                    }
                }
            } else {
                TextRegularBrownItalic(
                    modifier = modifier
                        .padding(20.dp)
                        .align(Alignment.CenterHorizontally),
                    text = stringResource(id = R.string.msg_nenhum_resultado_pesquisa)
                )
            }
        }
    }
}

private fun filtrarPor(
    it: DadosPassagem,
    state: PesquisarPassagemUiState,
): Boolean {
    return it.nomePassageiro1.startsWith(state.pesquisa, true) ||
            it.nomePassageiro2.startsWith(state.pesquisa, true) ||
            it.nomePassageiro3.startsWith(state.pesquisa, true) ||
            it.documentoPassageiro1.startsWith(state.pesquisa, true) ||
            it.documentoPassageiro2.startsWith(state.pesquisa, true) ||
            it.documentoPassageiro3.startsWith(state.pesquisa, true) ||
            it.nomeResponsavelRetirada.startsWith(state.pesquisa, true) ||
            it.numeroDocumentoResponsavelRetirada.startsWith(state.pesquisa, true) ||
            it.placaVeiculo.startsWith(state.pesquisa, true)
}

@Preview
@Composable
private fun ResultadosViagemSearchScreenPreview() {
    ResultadosPassagemSearchScreen(
        state = PesquisarPassagemUiState(
            listaResultadoPassagens = listOf(dadosPassagemSample)
        )
    )
}

@Preview
@Composable
private fun ResultadosViagemSearchScreenVaziaPreview() {
    ResultadosPassagemSearchScreen(
        state = PesquisarPassagemUiState(
            listaResultadoPassagens = emptyList()
        )
    )
}

@Preview
@Composable
private fun ResultadosViagemSearchProcessPreview() {
    ResultadosPassagemSearchScreen(
        state = PesquisarPassagemUiState(
            listaResultadoPassagens = emptyList(),
            isProcessing = true
        )
    )
}

@Preview
@Composable
private fun ResultadosViagemSearchSearchBarPreview() {
    ResultadosPassagemSearchScreen(
        state = PesquisarPassagemUiState(
            listaResultadoPassagens = emptyList(),
            isShowBarraPesquisa = true
        )
    )
}