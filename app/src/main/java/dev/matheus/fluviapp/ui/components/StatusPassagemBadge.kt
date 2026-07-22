package dev.matheus.fluviapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.model.passagem.StatusPassagem
import dev.matheus.fluviapp.ui.theme.AbyssNavy
import dev.matheus.fluviapp.ui.theme.AquaAccent
import dev.matheus.fluviapp.ui.theme.Brown
import dev.matheus.fluviapp.ui.theme.MistGray
import dev.matheus.fluviapp.ui.theme.SteelTeal
import dev.matheus.fluviapp.ui.theme.Yellow

/**
 * Badge de status do ciclo de vida da passagem (ADR-0012 Fase 5). Torna o status **legível de relance**
 * — antes era só texto puro (nos Detalhes) ou ausente (no card da lista). A cor conta a história do
 * ciclo com tokens da própria paleta (nada fora do `Color.kt`):
 *  - **A EMITIR** (amarelo/marrom) → atenção: falta emitir;
 *  - **EMITIDA** (aqua/navy) → pronta, o estado acionável (é a que o QR embarca);
 *  - **EMBARCADA** (steel/mist) → consumida, estado terminal assentado.
 *
 * A pílula é preenchida (cor + cor-de-texto próprias), então lê bem tanto sobre o card navy da lista
 * quanto sobre a superfície clara dos Detalhes. Recebe a `situacao` já rotulada (`DadosPassagem`) e a
 * reparseia com `StatusPassagem.de` (tolerante à grafia legada); grafia desconhecida cai num tom neutro.
 */
@Composable
fun StatusPassagemBadge(
    situacao: String,
    modifier: Modifier = Modifier,
) {
    if (situacao.isBlank()) return

    val status = StatusPassagem.de(situacao)
    val (fundo, texto) = when (status) {
        StatusPassagem.A_EMITIR -> Yellow to Brown
        StatusPassagem.EMITIDA -> AquaAccent to AbyssNavy
        StatusPassagem.EMBARCADA -> SteelTeal to MistGray
        null -> MistGray to AbyssNavy
    }
    val rotulo = status?.rotulo() ?: situacao

    Text(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(fundo)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        text = rotulo,
        color = texto,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.labelSmall,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF042630)
@Composable
private fun StatusPassagemBadgePreview() {
    androidx.compose.foundation.layout.Column(
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .background(Color(0xFF042630))
            .padding(16.dp),
    ) {
        StatusPassagemBadge(situacao = "A EMITIR")
        StatusPassagemBadge(situacao = "EMITIDA")
        StatusPassagemBadge(situacao = "EMBARCADA")
    }
}