package dev.matheus.fluviapp.ui.components.forms.buttons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.components.texts.TextRegularNoColor

@Composable
fun FilterButton(
    modifier: Modifier,
    label: Int,
    isChecked: Boolean,
    onCheck: () -> Unit
) {
    val (border, containerColor, textColor) = if (isChecked) {
        Triple(
            null,
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
            MaterialTheme.colorScheme.onPrimary
        )
    } else {
        Triple(
            BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground),
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.onSecondary,
            ),
            MaterialTheme.colorScheme.onBackground
        )
    }
    Card(
        modifier = modifier
            .width(165.dp)
            .padding(20.dp),
        shape = RoundedCornerShape(50),
        colors = containerColor,
        border = border,
        onClick = onCheck
    ) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            TextRegularNoColor(
                modifier = modifier.padding(20.dp, 10.dp),
                text = stringResource(id = label),
                color = textColor
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FilterButtonClicadoPreview() {
    FilterButton(
        modifier = Modifier,
        label = R.string.label_veiculo,
        isChecked = true,
        onCheck = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun FilterButtonNaoClicadoPreview() {
    FilterButton(
        modifier = Modifier,
        label = R.string.label_passageiro,
        isChecked = false,
        onCheck = {}
    )
}