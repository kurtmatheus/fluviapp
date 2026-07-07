package br.com.gruponaveg.ui.components.forms.buttons

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.gruponaveg.ui.components.texts.TextTitleItalic

@Composable
fun CommonIconButton(
    modifier: Modifier,
    onClick: () -> Unit,
    text: String,
    width: Int = 300,
    color: Color = MaterialTheme.colorScheme.primary,
    isProcessing: Boolean = false,
    icon: @Composable () -> Unit = {},
) {
    if (!isProcessing) {
        Button(
            modifier = modifier
                .width(width.dp)
                .heightIn(56.dp)
                .padding(10.dp),
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = color,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            icon()
            Spacer(modifier = modifier.padding(5.dp, 0.dp))
            TextTitleItalic(text = text)
        }
    } else {
        Box(
            modifier = modifier.fillMaxWidth()
        ) {
            CircularProgressIndicator(
                modifier = modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CommonButtonFormPreview() {
    CommonIconButton(
        modifier = Modifier,
        onClick = { /*TODO*/ },
        text = "Emitir",
        icon = {
            Icon(imageVector = Icons.Filled.Email, contentDescription = null)
        },
        color = MaterialTheme.colorScheme.primary
    )
}

@Preview(showBackground = true)
@Composable
private fun CommonButtonFormProcessingPreview() {
    CommonIconButton(
        modifier = Modifier,
        onClick = { /*TODO*/ },
        text = "Emitir",
        color = MaterialTheme.colorScheme.primary,
        isProcessing = true,
        icon = {
            Icon(imageVector = Icons.Filled.Email, contentDescription = null)
        }
    )
}