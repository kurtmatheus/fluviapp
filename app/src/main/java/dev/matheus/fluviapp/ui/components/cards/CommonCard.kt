package dev.matheus.fluviapp.ui.components.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun CommonCard(
    modifier: Modifier,
    color: Color,
    borderStroke: BorderStroke? = null,
    alturaCard: Int = 200,
    shape: RoundedCornerShape = RoundedCornerShape(20),
    onClick: () -> Unit,
    enable: Boolean = true,
    conteudo: @Composable (ColumnScope.() -> Unit)
) {
    Card(
        modifier = modifier
            .padding(20.dp)
            .fillMaxWidth()
            .height(alturaCard.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f),
            disabledContainerColor = color.copy(alpha = 0.1f)
        ),
        shape = shape,
        border = borderStroke,
        content = conteudo,
        enabled = enable
    )
}

@Preview(showBackground = true)
@Composable
private fun CardGenericoPreview() {
    CommonCard(
        modifier = Modifier,
        color = MaterialTheme.colorScheme.primary,
        onClick = {},
        conteudo = {

        }
    )
}

@Preview(showBackground = true)
@Composable
private fun CardGenericoStrokeDisabledPreview() {
    CommonCard(
        modifier = Modifier,
        color = MaterialTheme.colorScheme.primary,
        borderStroke = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.onBackground),
        onClick = {},
        conteudo = {},
        enable = false
    )
}