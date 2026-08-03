package dev.matheus.fluviapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.ui.components.texts.FluviWordmark
import dev.matheus.fluviapp.ui.theme.FluviAppTheme

/**
 * Tela de abertura: a **marca** enquanto o contexto é carregado (E1 do roadmap; ADR-0020 D9). Substitui o
 * spinner solto que vivia dentro do grafo de navegação — a splash é tela, e tela mora em `ui/screens`.
 *
 * Não tem espera artificial: o [dev.matheus.fluviapp.ui.viewmodel.SplashScreenViewModel] resolve na
 * velocidade da leitura. A diferença é que agora **há leitura** — usuário, funcionário e atuação —, então
 * o indicador tem o que cobrir, e a falha tem onde aparecer em vez de virar espera infinita.
 */
@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    houveErro: Boolean = false,
    onTentarNovamente: () -> Unit = {},
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (houveErro) {
                Text(
                    text = stringResource(R.string.error_carregar_contexto),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
                TextButton(onClick = onTentarNovamente) {
                    Text(stringResource(R.string.btn_tentar_novamente))
                }
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp,
                )
            }
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

@Preview(showBackground = true)
@Composable
private fun SplashScreenComErroPreview() {
    FluviAppTheme { SplashScreen(houveErro = true) }
}