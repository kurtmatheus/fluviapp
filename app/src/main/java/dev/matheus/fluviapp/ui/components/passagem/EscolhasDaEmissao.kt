package dev.matheus.fluviapp.ui.components.passagem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.ui.components.texts.TextSubTitleBrownBold
import dev.matheus.fluviapp.ui.theme.FluviAppTheme

/**
 * **O átomo do totem** ([ADR-0029] D1): um alvo grande, com ícone, que responde uma pergunta com um toque.
 *
 * Não é um botão comum estilizado — é um `Card` clicável, e a diferença importa em dois lugares: no tamanho
 * do alvo (bilheteria de beira de rio, aparelho modesto, gente com pressa na fila) e na **leitura sem ler**,
 * que é o que o ícone compra. Quem opera dez horas por dia passa a reconhecer a forma antes da palavra.
 */
@Composable
fun BotaoDeEscolha(
    rotulo: String,
    icone: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    descricao: String? = null,
) {
    Card(
        modifier = modifier.heightIn(min = 120.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(imageVector = icone, contentDescription = null, modifier = Modifier.size(48.dp))
            TextSubTitleBrownBold(text = rotulo)
            descricao?.let { TextSubTitleBrownBold(text = it) }
        }
    }
}

/**
 * As escolhas **lado a lado**, duas por linha — a forma das perguntas curtas (categoria, acomodação, tipo,
 * quantidade), em que o operador compara as opções de relance.
 *
 * A última ímpar ocupa meia largura em vez de esticar: um alvo do tamanho da tela sugeriria que ele é a
 * resposta certa, e o totem não induz escolha.
 */
@Composable
fun GradeDeEscolhas(
    escolhas: List<EscolhaVisual>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        escolhas.chunked(2).forEach { linha ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                linha.forEach { escolha ->
                    BotaoDeEscolha(
                        rotulo = escolha.rotulo,
                        icone = escolha.icone,
                        onClick = escolha.aoEscolher,
                        descricao = escolha.descricao,
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                    )
                }
                // Preenche o vazio da linha ímpar sem esticar o botão que sobrou.
                if (linha.size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            }
        }
    }
}

/**
 * As escolhas **empilhadas**, uma por linha — a forma pedida para a classe do veículo, que é a lista mais
 * longa do fluxo (seis classes) e a única em que o rótulo é o que distingue, não a figura.
 */
@Composable
fun ListaDeEscolhas(
    escolhas: List<EscolhaVisual>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        escolhas.forEach { escolha ->
            Card(
                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                onClick = escolha.aoEscolher,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(imageVector = escolha.icone, contentDescription = null, modifier = Modifier.size(32.dp))
                    TextSubTitleBrownBold(text = escolha.rotulo)
                }
            }
        }
    }
}

/**
 * Uma opção como a tela a mostra: rótulo, ícone e o que fazer.
 *
 * Ela carrega o **rótulo já resolvido**, e não o valor de domínio, porque é isso que permite a mesma grade
 * servir a categoria, acomodação, tipo, quantidade e classe — cinco perguntas com uma implementação.
 */
data class EscolhaVisual(
    val rotulo: String,
    val icone: ImageVector,
    val aoEscolher: () -> Unit,
    val descricao: String? = null,
)

@Preview
@Composable
private fun GradeDeEscolhasPreview() {
    FluviAppTheme {
        GradeDeEscolhas(
            escolhas = listOf(
                EscolhaVisual("Passageiro", Icons.Filled.Person, {}),
                EscolhaVisual("Veículo", Icons.Filled.DirectionsCar, {}),
            ),
        )
    }
}