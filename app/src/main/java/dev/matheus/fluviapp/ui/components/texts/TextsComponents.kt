package dev.matheus.fluviapp.ui.components.texts

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.matheus.fluviapp.R

@Composable
fun TextTitleBrownItalic(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = text,
        fontFamily = FontFamily(Font(R.font.roboto)),
        fontStyle = FontStyle.Italic,
        fontWeight = FontWeight(700),
        fontSize = 18.sp,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun TextTitleBrownRegular(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = text,
        fontFamily = FontFamily(Font(R.font.roboto)),
        fontWeight = FontWeight(700),
        fontSize = 18.sp,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun TextTitleWhiteItalic(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = text,
        fontFamily = FontFamily(Font(R.font.roboto)),
        fontStyle = FontStyle.Italic,
        fontWeight = FontWeight(700),
        fontSize = 18.sp,
        color = White
    )
}

@Composable
fun TextTitleItalic(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = text,
        fontFamily = FontFamily(Font(R.font.roboto)),
        fontStyle = FontStyle.Italic,
        fontWeight = FontWeight(700),
        fontSize = 18.sp
    )
}

@Composable
fun TextSubTitleBrownItalic(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = text,
        fontWeight = FontWeight(400),
        fontStyle = FontStyle.Italic,
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun TextSubTitleBrownBold(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = text,
        fontWeight = FontWeight(700),
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun TextRegularBrownItalic(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = text,
        fontWeight = FontWeight(400),
        fontStyle = FontStyle.Italic,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun TextBoldBrownItalic(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = text,
        fontWeight = FontWeight(700),
        fontStyle = FontStyle.Italic,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun TextBoldWhiteItalic(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = text,
        fontWeight = FontWeight(700),
        fontStyle = FontStyle.Italic,
        fontSize = 14.sp,
        color = White
    )
}

@Composable
fun TextRegularBrown(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = text,
        fontWeight = FontWeight(400),
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun TextRegularNoColor(
    text: String,
    modifier: Modifier = Modifier,
    color: Color
) {
    Text(
        modifier = modifier,
        text = text,
        fontWeight = FontWeight(400),
        fontSize = 14.sp,
        color = color
    )
}

@Composable
fun TextBoldNavyBlue(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = text,
        fontWeight = FontWeight(700),
        fontSize = 14.sp,
        // Roteado pelo tema (accent) — o NavyBlue fixo sumia sobre o fundo navy do tema escuro.
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun TextRegularWhite(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = text,
        fontWeight = FontWeight(400),
        fontSize = 14.sp,
        color = White,
        maxLines = 1
    )
}

@Composable
fun TextRegular(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = text,
        fontWeight = FontWeight(400),
        fontSize = 14.sp,
    )
}

@Composable
fun TextRegularWhiteItalic(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = text,
        fontWeight = FontWeight(400),
        fontStyle = FontStyle.Italic,
        fontSize = 14.sp,
        color = White
    )
}

@Composable
fun SupportingTextRed(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = text,
        fontWeight = FontWeight(400),
        fontStyle = FontStyle.Italic,
        fontSize = 14.sp,
        color = Red
    )
}

@Composable
fun SupportingText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = text,
        fontWeight = FontWeight(400),
        fontSize = 10.sp,
        color = Gray
    )
}