package dev.matheus.fluviapp.ui.components.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.matheus.fluviapp.ui.theme.NavyBlue

@Composable
fun CommonDialog(
    modifier: Modifier,
    onDismiss: () -> Unit,
    containerColor: Color = NavyBlue,
    contentColor: Color = Color.White,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = modifier
                .padding(20.dp)
                .fillMaxWidth()
                .height(200.dp),
            onClick = { /*TODO*/ },
            colors = CardDefaults.cardColors(
                disabledContainerColor = containerColor,
                disabledContentColor = contentColor
            ),
            enabled = false
        ) {
            content()
        }
    }
}

@Preview
@Composable
private fun CommonDialogPreview() {
    CommonDialog(
        modifier = Modifier,
        onDismiss = {}
    ) { }
}