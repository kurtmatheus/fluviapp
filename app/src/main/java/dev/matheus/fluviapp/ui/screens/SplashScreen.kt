package dev.matheus.fluviapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.matheus.fluviapp.ui.components.texts.FluviWordmark
import dev.matheus.fluviapp.ui.theme.FluviAppTheme

/**
 * Tela de abertura: a **marca** enquanto a sessão é resolvida (E1 do roadmap). Substitui o spinner solto
 * que vivia dentro do grafo de navegação — a splash é tela, e tela mora em `ui/screens`.
 *
 * Não tem espera artificial: o [dev.matheus.fluviapp.ui.viewmodel.SplashScreenViewModel] decide o destino
 * na velocidade da checagem, e o indicador só aparece pelo tempo que ela levar.
 */
@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FluviWordmark(fontSize = 44.sp)

        Column(
            modifier = Modifier.height(56.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SplashScreenPreview() {
    FluviAppTheme { SplashScreen() }
}

@Preview(showBackground = true)
@Composable
private fun SplashScreenEscuraPreview() {
    FluviAppTheme(darkTheme = true) { SplashScreen() }
}