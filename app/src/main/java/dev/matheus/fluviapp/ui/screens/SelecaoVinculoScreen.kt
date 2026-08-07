package dev.matheus.fluviapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrown
import dev.matheus.fluviapp.ui.components.texts.TextTitleBrownRegular
import dev.matheus.fluviapp.ui.states.SelecaoVinculoUiState
import dev.matheus.fluviapp.ui.states.VinculoOpcao
import dev.matheus.fluviapp.ui.components.texts.FluviWordmark
import dev.matheus.fluviapp.ui.theme.FluviAppTheme

/**
 * A tela da **seleção de contexto** (F6.4).
 *
 * Ela é deliberadamente pobre: marca, uma pergunta e a lista. Não tem barra de topo com voltar, e isso é
 * a decisão em pixels — não há para onde voltar antes de dizer em nome de quem se opera.
 */
@Composable
fun SelecaoVinculoScreen(
    uiState: SelecaoVinculoUiState,
    onEscolher: (String) -> Unit = {},
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FluviWordmark()

            if (uiState.carregando) {
                CircularProgressIndicator()
                return@Column
            }

            TextTitleBrownRegular(text = stringResource(R.string.titulo_selecao_vinculo))
            // O nome de quem está entrando: a pergunta é sobre o contexto dela, não sobre a conta.
            if (uiState.nome.isNotBlank()) {
                TextRegularBrown(text = uiState.nome)
            }

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(uiState.opcoes) { opcao ->
                    OpcaoDeVinculo(opcao = opcao, onEscolher = onEscolher)
                }
            }
        }
    }
}

@Composable
private fun OpcaoDeVinculo(
    opcao: VinculoOpcao,
    onEscolher: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEscolher(opcao.empresaId) }
            .padding(vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                TextTitleBrownRegular(text = opcao.empresa)
                // O cargo é o que muda o que a pessoa poderá fazer depois de escolher.
                TextRegularBrown(text = opcao.cargo)
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun SelecaoVinculoScreenPreview() {
    FluviAppTheme {
        SelecaoVinculoScreen(
            uiState = SelecaoVinculoUiState(
                nome = "Ana Ribeiro",
                carregando = false,
                opcoes = listOf(
                    VinculoOpcao("e1", "Navegação Norte", "SUPERVISOR"),
                    VinculoOpcao("e2", "Rio Sul", "AGENTE"),
                ),
            ),
        )
    }
}