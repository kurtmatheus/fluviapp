package dev.matheus.fluviapp.ui.components.texts

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.theme.AquaAccent
import dev.matheus.fluviapp.ui.theme.SteelTeal
import dev.matheus.fluviapp.ui.theme.Yellow

/**
 * Wordmark de texto que substitui a logo em imagem — reproduz o mini logo "fluviApp": "fluvi"
 * preenchido (teal) e "App" **vazado** (contorno) com **gradiente** teal→amarelo→ciano.
 *
 * O efeito do "App" é feito só com `SpanStyle` (sem Canvas): `brush` (gradiente) + `drawStyle =
 * Stroke` (contorno). O [Modifier] preserva o slot que a imagem ocupava; [fontSize] ajusta o tamanho.
 *
 * `fluviColor` segue o tema (claro/escuro) por default; o gradiente do "App" é fixo da marca. Em
 * superfícies de cor fixa (AppBar navy, ticket branco) passe [fluviColor]/[appGradient] explícitos.
 */
@Composable
fun FluviWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 24.sp,
    alpha: Float = 1f,
    fluviColor: Color = MaterialTheme.colorScheme.primary,
    appGradient: List<Color> = listOf(SteelTeal, Yellow, AquaAccent),
    strokeWidth: Float = 2.5f,
) {
    val texto = buildAnnotatedString {
        withStyle(SpanStyle(color = fluviColor.copy(alpha = alpha))) { append("fluvi") }
        withStyle(
            SpanStyle(
                brush = Brush.linearGradient(appGradient),
                alpha = alpha,
                drawStyle = Stroke(width = strokeWidth),
            )
        ) { append("App") }
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
