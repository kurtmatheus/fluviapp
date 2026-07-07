package br.com.gruponaveg.ui.components.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import br.com.gruponaveg.ui.theme.NavyBlue

@Composable
fun CommonExpansiveDialog(
    modifier: Modifier,
    onDismiss: () -> Unit,
    containerColor: Color = NavyBlue,
    contentColor: Color = Color.White,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = modifier
                .padding(20.dp)
                .fillMaxWidth()
                .heightIn(200.dp),
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