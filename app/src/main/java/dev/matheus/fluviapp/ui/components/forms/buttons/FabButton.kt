package dev.matheus.fluviapp.ui.components.forms.buttons

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R

@Composable
fun FabButton(
    modifier: Modifier,
    onClick: () -> Unit
) {
    FilledIconButton(
        modifier = modifier
            .width(65.dp)
            .heightIn(65.dp)
            .padding(10.dp),
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        )
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = stringResource(R.string.description_adicionar)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FabButtonFormPreview() {
        FabButton(
            modifier = Modifier,
            onClick = { /*TODO*/ },
        )
}