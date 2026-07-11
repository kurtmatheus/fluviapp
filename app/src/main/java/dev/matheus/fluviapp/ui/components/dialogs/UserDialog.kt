package dev.matheus.fluviapp.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.components.texts.TextBoldWhiteItalic
import dev.matheus.fluviapp.ui.components.texts.TextRegularWhiteItalic
import dev.matheus.fluviapp.ui.theme.AquaAccent

@Composable
fun UserDialog(
    modifier: Modifier,
    username: String,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onClickDeslogar: () -> Unit,
    onDismiss: () -> Unit
) {
    CommonDialog(
        modifier = modifier,
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = modifier.height(50.dp),
                    painter = painterResource(id = R.drawable.ic_user_75),
                    contentDescription = stringResource(id = R.string.description_icon_user),
                    tint = White
                )
                TextRegularWhiteItalic(text = username)
            }

            HorizontalDivider(
                thickness = 1.dp, color = White
            )

            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = null,
                        tint = White
                    )
                    TextBoldWhiteItalic(text = stringResource(id = R.string.label_tema_escuro))
                }
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = { onToggleTheme() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AquaAccent,
                        checkedTrackColor = White
                    )
                )
            }

            HorizontalDivider(
                thickness = 1.dp, color = White
            )

            TextButton(
                modifier = modifier.fillMaxWidth(),
                onClick = onClickDeslogar
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = stringResource(id = R.string.description_icon_user),
                    tint = White
                )
                Spacer(modifier = modifier.padding(5.dp))
                TextBoldWhiteItalic(text = stringResource(id = R.string.btn_deslogar))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UserDialogPreview() {
    UserDialog(
        modifier = Modifier,
        username = "Kurt",
        isDarkTheme = true,
        onClickDeslogar = {},
        onDismiss = {}
    )
}