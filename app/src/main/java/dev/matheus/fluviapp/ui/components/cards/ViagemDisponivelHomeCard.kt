package dev.matheus.fluviapp.ui.components.cards

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.states.ViagemDisponivelCard
import dev.matheus.fluviapp.ui.theme.FluviAppTheme

/**
 * Uma saída disponível no Início da empresa — a herdeira do `HomeCard` que a F8.0 demoliu.
 *
 * A **partida vem primeiro e em destaque**, e é a mudança de fundo em relação ao card antigo: aquele
 * abria pelo nome da embarcação, porque a Viagem-trecho não tinha data nem hora — o horário era digitado
 * na emissão. Agora a ocorrência é datada, e é a data que responde a pergunta de quem abre o app.
 *
 * ### As duas leituras
 *
 * A lista é ordenada por instante de partida, o que já põe a saída mais próxima no topo — mas *primeiro*
 * não é a mesma informação que *hoje*, e era só a primeira que o card sabia dar. Numa semana sem saída
 * hoje, o topo da lista continua sendo o topo, e quem lê rápido conclui o contrário.
 *
 * Então o card tem duas formas: a de **hoje** ganha selo, borda de acento, elevação e fundo tonal; as
 * demais ficam em plano secundário, num fundo neutro e sem borda. Não é hierarquia decorativa — é a
 * diferença entre *o que se opera agora* e *o que está por vir*.
 *
 * ### O que mudou no desenho, e por quê
 *
 * Sai a **altura fixa de 150 dp**, que obrigava toda saída a ocupar o mesmo espaço mesmo sem chegada
 * estimada, e sai o par ícone-de-75 dp + divisória vertical, que gastava um terço da largura para dizer
 * "isto é uma embarcação" — coisa que o card inteiro já diz. No lugar, hierarquia tipográfica:
 * partida → trajeto → embarcação → chegada, que é a ordem das perguntas de quem está na doca.
 */
@Composable
fun ViagemDisponivelHomeCard(
    modifier: Modifier,
    viagem: ViagemDisponivelCard,
    onClick: (String) -> Unit = {},
) {
    val esquema = MaterialTheme.colorScheme
    val destacada = viagem.ehHoje

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        // A **ocorrência**, não a viagem: é a saída datada que se vende, e é dela que a data do bilhete sai
        // (ADR-0028 D5). Entregar `viagemId` levaria a emissão a perguntar "qual terça?" — que é a pergunta
        // que o card acabou de responder.
        onClick = { onClick(viagem.id) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (destacada) esquema.primary.copy(alpha = 0.14f)
            else esquema.surfaceVariant.copy(alpha = 0.35f),
        ),
        border = if (destacada) BorderStroke(1.dp, esquema.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (destacada) 3.dp else 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (destacada) SeloDeHoje()

            Text(
                text = viagem.partida,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                // O acento fica **no texto que responde a pergunta**, e não no card inteiro: pintar tudo
                // de destaque não destaca nada.
                color = if (destacada) esquema.primary else esquema.onSurface,
            )

            Text(
                text = viagem.rota,
                style = MaterialTheme.typography.bodyMedium,
                color = esquema.onSurface,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    modifier = Modifier.size(16.dp),
                    painter = painterResource(id = R.drawable.ic_embarcacao_75),
                    contentDescription = stringResource(id = R.string.description_icon_embarcacao),
                    tint = esquema.onSurfaceVariant,
                )
                Text(
                    text = viagem.embarcacao,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = esquema.onSurfaceVariant,
                )
            }

            // O dia só entra quando a travessia o atravessa (decisão do mapper): repetir a chegada numa
            // viagem que chega no mesmo dia seria ruído, e omiti-la numa que não chega seria engano.
            if (viagem.chegada.isNotBlank()) {
                Text(
                    text = stringResource(R.string.msg_chegada_estimada, viagem.chegada),
                    style = MaterialTheme.typography.bodySmall,
                    color = esquema.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * O selo que diz o que a ordenação não dizia.
 *
 * Só a palavra, sem a data: o cabeçalho da lista já escreve *"Hoje: dd/MM/aaaa"* logo acima
 * (`CommonTopRow`), e repeti-la aqui responderia de novo *que dia é hoje* em vez de **qual saída é a de
 * hoje**, que é a pergunta que sobrava.
 */
@Composable
private fun SeloDeHoje() {
    Text(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        text = stringResource(id = R.string.label_info_hoje).uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimary,
    )
}

private val saidaDeHoje = ViagemDisponivelCard(
    id = "v1@2026-08-11",
    viagemId = "v1",
    partida = "Terça-feira, 11/08 · 18:00",
    rota = "Porto de Val-de-Cães · Belém/PA → Porto de Parintins · Parintins/AM",
    embarcacao = "F/B Modelo",
    chegada = "Qui 00:00",
    ehHoje = true,
)

@Preview(name = "Hoje", showBackground = true)
@Composable
private fun ViagemDeHojePreview() {
    FluviAppTheme {
        ViagemDisponivelHomeCard(modifier = Modifier, viagem = saidaDeHoje)
    }
}

@Preview(name = "Próximos dias", showBackground = true)
@Composable
private fun ViagemDeOutroDiaPreview() {
    FluviAppTheme {
        ViagemDisponivelHomeCard(
            modifier = Modifier,
            viagem = saidaDeHoje.copy(
                partida = "Sexta-feira, 14/08 · 06:00",
                chegada = "",
                ehHoje = false,
            ),
        )
    }
}

/**
 * As duas formas **no escuro**, que é onde a distinção corre mais risco: sobre o navy, um fundo tonal de
 * baixa opacidade quase não se separa da superfície, e é a borda de acento que sustenta a leitura.
 */
@Preview(name = "Hoje (escuro)", uiMode = UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun ViagemDeHojeEscuroPreview() {
    FluviAppTheme {
        ViagemDisponivelHomeCard(modifier = Modifier, viagem = saidaDeHoje)
    }
}

@Preview(name = "Próximos dias (escuro)", uiMode = UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun ViagemDeOutroDiaEscuroPreview() {
    FluviAppTheme {
        ViagemDisponivelHomeCard(
            modifier = Modifier,
            viagem = saidaDeHoje.copy(partida = "Sexta-feira, 14/08 · 06:00", ehHoje = false),
        )
    }
}
