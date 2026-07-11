package dev.matheus.fluviapp.ui.components.texts

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import dev.matheus.fluviapp.R

/**
 * Wordmark de texto que substitui a logo em imagem.
 *
 * Bicolor itálico — "Fluvi" no accent + "App" no tom de texto do tema. O [Modifier] preserva
 * a dimensão do slot que a imagem ocupava (ex.: `size(250.dp)`, `height(65.dp)`, `matchParentSize()`),
 * e o [fontSize] ajusta o tamanho visual dentro dele. Sem `autoSize` (indisponível no Compose 1.7).
 *
 * Cores default seguem o tema (claro/escuro). Em superfícies de cor fixa — AppBar navy, ticket de
 * impressão branco — passe [fluviColor]/[appColor] explícitos.
 */
@Composable
fun FluviWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 24.sp,
    alpha: Float = 1f,
    fluviColor: Color = MaterialTheme.colorScheme.primary,
    appColor: Color = MaterialTheme.colorScheme.onBackground,
) {
    val texto = buildAnnotatedString {
        withStyle(SpanStyle(color = fluviColor.copy(alpha = alpha))) { append("Fluvi") }
        withStyle(SpanStyle(color = appColor.copy(alpha = alpha))) { append("App") }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = texto,
            fontFamily = FontFamily(Font(R.font.roboto)),
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight(800),
            fontSize = fontSize,
            maxLines = 1,
            softWrap = false
        )
    }
}
