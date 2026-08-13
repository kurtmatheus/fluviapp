package dev.matheus.fluviapp.ui.components.passagem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrown
import dev.matheus.fluviapp.ui.components.texts.TextSubTitleBrownBold
import dev.matheus.fluviapp.ui.states.passagem.CabecalhoDaViagem
import dev.matheus.fluviapp.ui.theme.FluviAppTheme

/**
 * **O cabeçalho de guia** ([ADR-0028] D5): a saída escolhida, visível em **todos** os passos.
 *
 * Ele corrige dois defeitos do formulário que substituiu, e o segundo é o que motiva a decisão:
 *
 * 1. o card da viagem ficava **dentro da rolagem** e sumia justamente quando se preenchia o que dependia
 *    dele;
 * 2. **data e hora eram campos editáveis**. Dava para digitar uma data que discorda da saída escolhida — o
 *    mesmo defeito que a agência digitada teve até a P2.3. Aqui não há o que digitar: a ocorrência chega
 *    pronta do card de saída, e o cabeçalho **exibe**.
 */
@Composable
fun CabecalhoDaEmissao(
    cabecalho: CabecalhoDaViagem,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.DirectionsBoat, contentDescription = null, modifier = Modifier.size(18.dp))
                TextSubTitleBrownBold(text = cabecalho.travessia)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                TextRegularBrown(text = listOf(cabecalho.partida, cabecalho.embarcacao)
                    .filter { it.isNotBlank() }
                    .joinToString(" · "))
            }
        }
    }
}

/**
 * **A trilha dos passos**: um ponto por passo do roteiro, com o corrente destacado.
 *
 * Ela é numerada e **derivada do roteiro** ([ADR-0029] D3), então "3 de 6" está certo em qualquer fluxo — o
 * caminho do veículo e o de três pessoas na suíte têm tamanhos diferentes, e nenhum deles precisa saber
 * disso. Um indicador de tamanho fixo mentiria em pelo menos um dos dois.
 */
@Composable
fun TrilhaDePassos(
    numeroDoPasso: Int,
    totalDePassos: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(totalDePassos) { indice ->
            val corrente = indice + 1 == numeroDoPasso
            val percorrido = indice + 1 < numeroDoPasso
            Surface(
                modifier = Modifier.weight(1f).height(6.dp),
                shape = CircleShape,
                color = when {
                    corrente -> MaterialTheme.colorScheme.primary
                    percorrido -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
            ) {}
        }
        TextRegularBrown(text = "$numeroDoPasso/$totalDePassos")
    }
}

@Preview
@Composable
private fun CabecalhoDaEmissaoPreview() {
    FluviAppTheme {
        Column {
            CabecalhoDaEmissao(
                CabecalhoDaViagem(
                    travessia = "Porto de Val-de-Cães · Belém/PA → Porto de Parintins · Parintins/AM",
                    partida = "Terça-feira, 18/08 · 18:00",
                    embarcacao = "F/B Modelo",
                ),
            )
            TrilhaDePassos(numeroDoPasso = 3, totalDePassos = 6)
        }
    }
}