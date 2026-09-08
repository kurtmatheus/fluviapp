package dev.matheus.fluviapp.ui.components.cards

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.operacoes.MetricasDeAcesso
import dev.matheus.fluviapp.ui.theme.FluviAppTheme

/**
 * **O acesso da plataforma, em cinco números** — o Início do painel do `ADM` ([ADR-0032] D6, decisão do
 * analista em 2026-09-08).
 *
 * ### Não é um painel de indicadores; é uma lista de pendências

 * Cada número tem um gesto atrás, e é isso que decide o desenho. O total fica em cima, grande, porque
 * responde *quantas pessoas entram no app* — a pergunta que o console respondia. Embaixo, só o que pede
 * ação: desativados (reativar), expirados (prazo novo), convites pendentes (cobrar quem não veio) e a
 * vencer (renovar antes de doer).
 *
 * **Zero não aparece.** Um card com quatro zeros ensina a não olhar o card; um card com um número ensina
 * onde olhar. Quando não há pendência nenhuma, o rodapé é uma linha dizendo isso — que é informação, e não
 * ausência dela.
 *
 * ### Por que ele é clicável inteiro, e não por número
 *
 * Porque o destino é o mesmo: a seção Usuários, onde os quatro gestos moram. Quatro áreas de toque levando
 * ao mesmo lugar dariam a impressão de quatro filtros, e filtro por situação é coisa que a lista não tem —
 * prometer navegação que não existe é pior do que não oferecer atalho.
 */
@Composable
fun CardDeAcesso(
    modifier: Modifier,
    acesso: MetricasDeAcesso,
    onClick: () -> Unit = {},
) {
    val esquema = MaterialTheme.colorScheme

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (acesso.temPendencia) esquema.primary.copy(alpha = 0.14f)
            else esquema.surfaceVariant.copy(alpha = 0.35f),
        ),
        border = if (acesso.temPendencia) BorderStroke(1.dp, esquema.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (acesso.temPendencia) 3.dp else 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = acesso.comAcesso.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = esquema.onSurface,
                )
                Text(
                    modifier = Modifier.padding(bottom = 4.dp),
                    text = stringResource(R.string.label_acessos_no_app),
                    style = MaterialTheme.typography.bodyMedium,
                    color = esquema.onSurfaceVariant,
                )
            }

            // A linha dos ativos vem separada do total: ela é a que não pede nada, e por isso não entra na
            // lista de pendências abaixo.
            Text(
                text = stringResource(R.string.label_acessos_ativos, acesso.ativos),
                style = MaterialTheme.typography.bodyMedium,
                color = esquema.onSurfaceVariant,
            )

            HorizontalDivider()

            if (!acesso.temPendencia) {
                Text(
                    text = stringResource(R.string.msg_acesso_sem_pendencia),
                    style = MaterialTheme.typography.bodyMedium,
                    color = esquema.onSurfaceVariant,
                )
                return@Column
            }

            // Zero não vira linha: ver "0 desativados" todos os dias ensina a não ler o card.
            if (acesso.desativados > 0) {
                Pendencia(R.string.label_acessos_desativados, acesso.desativados)
            }
            if (acesso.expirados > 0) {
                Pendencia(R.string.label_acessos_expirados, acesso.expirados)
            }
            if (acesso.convitesPendentes > 0) {
                Pendencia(R.string.label_convites_pendentes, acesso.convitesPendentes)
            }
            // O único preventivo, e o último da lista: os outros três já doeram.
            if (acesso.aVencer > 0) {
                Pendencia(R.string.label_acessos_a_vencer, acesso.aVencer)
            }
        }
    }
}

@Composable
private fun Pendencia(rotulo: Int, quantos: Int) {
    Text(
        text = stringResource(rotulo, quantos),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Start,
    )
}

/** O caso que justifica o destaque: há gente esperando gesto de alguém. */
@Preview(showBackground = true)
@Composable
private fun CardDeAcessoComPendenciaPreview() {
    FluviAppTheme {
        CardDeAcesso(
            modifier = Modifier,
            acesso = MetricasDeAcesso(
                ativos = 9,
                desativados = 2,
                expirados = 1,
                convitesPendentes = 3,
                aVencer = 2,
            ),
        )
    }
}

/** E o caso oposto: nada a fazer é informação, não card vazio. */
@Preview(showBackground = true)
@Composable
private fun CardDeAcessoTranquiloPreview() {
    FluviAppTheme {
        CardDeAcesso(modifier = Modifier, acesso = MetricasDeAcesso(ativos = 12))
    }
}

/** As duas leituras no escuro — é aí que a distinção depende da borda, e não do fundo. */
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun CardDeAcessoEscuroPreview() {
    FluviAppTheme {
        CardDeAcesso(
            modifier = Modifier,
            acesso = MetricasDeAcesso(ativos = 9, expirados = 1, aVencer = 2),
        )
    }
}
