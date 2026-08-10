package dev.matheus.fluviapp.ui.components.contents

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.components.cards.ViagemDisponivelHomeCard
import dev.matheus.fluviapp.ui.states.InicioDaTela
import dev.matheus.fluviapp.ui.states.ViagemDisponivelCard
import dev.matheus.fluviapp.ui.theme.FluviAppTheme

/**
 * A tela inicial, **desenhada a partir do que o domínio decidiu** (F8.4).
 *
 * O `when` é exaustivo sobre [InicioDaTela] de propósito: cada plano de acesso tem o seu Início, e um
 * plano novo não compila até dizer o que mostra. Antes da revitalização havia uma home só, com uma lista
 * de viagens igual para todos — a divisão entre plataforma e empresa agora existe no tipo, não num `if`
 * dentro do composable.
 *
 * As quatro faces, e por que nenhuma pode ser a lista vazia de outra:
 *
 * - **carregando** — o escopo ainda não chegou. Sem este estado, o painel piscaria "não há saídas" antes
 *   de saber se há;
 * - **plataforma** — ela monta o universo e não vende; o sumário dela é a F10;
 * - **empresa** — "Viagens Disponíveis", a lista de saídas da semana. Vazia aqui quer dizer *não há saída*;
 * - **sem concessão** — falta provisionar. Recado oposto ao anterior: um manda esperar, o outro manda
 *   procurar a plataforma.
 */
@Composable
fun InicioContent(
    modifier: Modifier,
    inicio: InicioDaTela,
    onClickViagem: (String) -> Unit = {},
) {
    when (inicio) {
        InicioDaTela.Carregando -> Box(modifier = modifier.fillMaxSize()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary,
            )
        }

        InicioDaTela.DaPlataforma -> RecadoCentral(modifier, R.string.msg_painel_plataforma)

        InicioDaTela.SemConcessao -> RecadoCentral(modifier, R.string.msg_viagem_sem_concessao)

        is InicioDaTela.DaEmpresa -> Column {
            CommonTopRow(modifier, R.string.subtitle_viagens_disponiveis)

            if (inicio.disponiveis.isEmpty()) {
                RecadoCentral(modifier, R.string.msg_nenhuma_viagem)
            } else {
                LazyColumn {
                    items(inicio.disponiveis, key = { it.id }) {
                        ViagemDisponivelHomeCard(
                            modifier = modifier,
                            viagem = it,
                            onClick = onClickViagem,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecadoCentral(modifier: Modifier, mensagem: Int) {
    Box(modifier = modifier.fillMaxSize()) {
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            text = stringResource(mensagem),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
    }
}

private val exemplo = listOf(
    ViagemDisponivelCard(
        id = "v1@2026-08-11",
        viagemId = "v1",
        partida = "Terça-feira, 11/08 · 18:00",
        rota = "Porto de Val-de-Cães · Belém/PA → Porto de Parintins · Parintins/AM",
        embarcacao = "F/B Modelo",
        chegada = "Qui 00:00",
    ),
    ViagemDisponivelCard(
        id = "v2@2026-08-14",
        viagemId = "v2",
        partida = "Sexta-feira, 14/08 · 06:00",
        rota = "Porto de Parintins · Parintins/AM → Porto de Val-de-Cães · Belém/PA",
        embarcacao = "F/B Modelo",
        chegada = "12:00",
    ),
)

@Preview(showBackground = true)
@Composable
private fun InicioDaEmpresaPreview() {
    FluviAppTheme { InicioContent(Modifier, InicioDaTela.DaEmpresa(exemplo)) }
}

@Preview(showBackground = true)
@Composable
private fun InicioDaEmpresaVazioPreview() {
    FluviAppTheme { InicioContent(Modifier, InicioDaTela.DaEmpresa(emptyList())) }
}

@Preview(showBackground = true)
@Composable
private fun InicioSemConcessaoPreview() {
    FluviAppTheme { InicioContent(Modifier, InicioDaTela.SemConcessao) }
}

@Preview(showBackground = true)
@Composable
private fun InicioDaPlataformaPreview() {
    FluviAppTheme { InicioContent(Modifier, InicioDaTela.DaPlataforma) }
}