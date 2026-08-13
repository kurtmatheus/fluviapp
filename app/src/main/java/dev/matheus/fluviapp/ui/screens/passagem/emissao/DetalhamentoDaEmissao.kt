package dev.matheus.fluviapp.ui.screens.passagem.emissao

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.matheus.fluviapp.ui.components.forms.buttons.CommonIconButton
import dev.matheus.fluviapp.ui.components.texts.FluviWordmark
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrown
import dev.matheus.fluviapp.ui.components.texts.TextRegularBrownItalic
import dev.matheus.fluviapp.ui.components.texts.TextSubTitleBrownBold
import dev.matheus.fluviapp.ui.states.passagem.ConfirmacaoDaEmissao
import dev.matheus.fluviapp.ui.theme.HeaderNavy
import dev.matheus.fluviapp.ui.theme.SteelTeal
import dev.matheus.fluviapp.ui.theme.marcaDaAgencia

/**
 * **O detalhamento que precede a emissão**: o que vai ser gravado, para conferir antes de gravar.
 *
 * Ele **não é um passo** — não entra no roteiro nem na trilha —, e é isso que o distingue das telas de
 * pergunta: aqui não se responde nada, se **lê**. O que o justifica é a irreversibilidade: cancelar depois
 * mantém o número e o registro, então conferir antes sai mais barato.
 *
 * ### A marca da agência aparece aqui, e não por enfeite
 *
 * O bilhete é assinado por **quem vendeu** (ADR-0015 §5), e o detalhamento é a prévia dele: mostrar a mesma
 * marca nos dois é o que faz o operador reconhecer, antes de emitir, que está vendendo pela agência certa —
 * quem opera em duas empresas troca de contexto no meio do expediente. Quem não tem marca própria cai na do
 * **FluviApp** (o `FluviWordmark`), e o documento nunca sai sem assinatura.
 */
@Composable
fun DetalhamentoDaEmissao(
    confirmacao: ConfirmacaoDaEmissao,
    onConfirmar: () -> Unit,
    onRevisar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AssinaturaDaAgencia(confirmacao.agencia)

                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onBackground)

                Linha("Travessia", confirmacao.cabecalho.travessia)
                Linha("Partida", confirmacao.cabecalho.partida)
                confirmacao.cabecalho.embarcacao.takeIf { it.isNotBlank() }?.let { Linha("Embarcação", it) }
                Linha("Bilhete", confirmacao.bilhete)

                confirmacao.pessoas.forEach { pessoa ->
                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    TextRegularBrownItalic(text = pessoa.papel)
                    Linha("Nome", pessoa.nome)
                    Linha("Documento", pessoa.documento)
                    Linha("Nascimento", pessoa.nascimento)
                }

                confirmacao.veiculo?.let { veiculo ->
                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    Linha("Placa", veiculo.placa)
                    Linha("Tipo", veiculo.classe)
                    veiculo.modelo?.let { Linha("Modelo", it) }
                    veiculo.cilindrada?.let { Linha("Cilindrada", it) }
                    veiculo.cor?.let { Linha("Cor", it) }
                }

                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

                // Gratuidade não tem lançamento, e a ausência precisa **dizer isso** — uma lista vazia com um
                // total de R$ 0,00 pareceria um pagamento que não foi registrado.
                if (confirmacao.lancamentos.isEmpty()) {
                    TextRegularBrownItalic(text = "Sem pagamento (gratuidade)")
                } else {
                    confirmacao.lancamentos.forEach { Linha(it.forma, it.valor) }
                    Linha("Total", confirmacao.total, destaque = true)
                }

                confirmacao.observacao?.let { Linha("Observação", it) }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CommonIconButton(
                modifier = Modifier,
                onClick = onRevisar,
                text = "Corrigir",
                width = 160,
                color = MaterialTheme.colorScheme.secondary,
                icon = { Icon(Icons.Filled.Edit, contentDescription = null) },
            )
            CommonIconButton(
                modifier = Modifier,
                onClick = onConfirmar,
                text = "Emitir",
                width = 160,
                icon = { Icon(Icons.Filled.ConfirmationNumber, contentDescription = null) },
            )
        }
    }
}

/** A assinatura do documento: a marca da agência quando existe, a do FluviApp quando não. */
@Composable
private fun AssinaturaDaAgencia(agencia: String) {
    val marca = marcaDaAgencia(agencia)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (marca != null) {
            Image(
                painter = painterResource(marca.logoTopo),
                contentDescription = agencia,
                modifier = Modifier.height(40.dp),
            )
        } else {
            FluviWordmark(
                modifier = Modifier.height(40.dp),
                fontSize = 20.sp,
                // Superfície clara: gradiente escuro para legibilidade — o mesmo ajuste que o bilhete fazia.
                fluviColor = SteelTeal,
                appGradient = listOf(SteelTeal, HeaderNavy, SteelTeal),
                strokeWidth = 3f,
            )
        }
        TextRegularBrownItalic(text = "Conferência")
    }
}

@Composable
private fun Linha(rotulo: String, valor: String, destaque: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextRegularBrownItalic(text = rotulo)
        if (destaque) TextSubTitleBrownBold(text = valor) else TextRegularBrown(text = valor)
    }
}