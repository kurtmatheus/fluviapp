package dev.matheus.fluviapp.navigation.navcomposables.passagem

import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.matheus.fluviapp.navigation.destinations.ARG_ID_PASSAGEM
import dev.matheus.fluviapp.navigation.destinations.FluviAppNavComposableDestinations
import dev.matheus.fluviapp.ui.screens.passagem.BilheteScreen
import dev.matheus.fluviapp.ui.viewmodel.passagem.BilheteViewModel

/**
 * O bilhete na navegação — **um destino, alcançado de dois lugares** (decisão do analista: *mesmo bilhete*).
 *
 * Hoje quem chega aqui é a emissão, logo depois de emitir. Quando a consulta de passagens voltar, ela aponta
 * para este mesmo destino: o documento é o mesmo, e duas telas desenhando o mesmo documento é como elas
 * passam a divergir.
 *
 * O `ACTION_SEND` mora **aqui**, e não no ViewModel: compartilhar é gesto de plataforma, e o ViewModel desta
 * casa não conhece `Context` (ADR-0026 D3). Ele fornece a URI; quem abre o seletor é a navegação.
 */
fun NavGraphBuilder.bilheteNavComposable(
    onClickVoltar: () -> Unit,
) {
    composable(
        route = FluviAppNavComposableDestinations.BilheteNavComposable.route,
        arguments = listOf(navArgument(ARG_ID_PASSAGEM) { type = NavType.StringType }),
    ) { entrada ->
        val viewModel = hiltViewModel<BilheteViewModel>()
        val state by viewModel.uiState.collectAsState()
        val contexto = LocalContext.current
        val idPassagem = entrada.arguments?.getString(ARG_ID_PASSAGEM)

        LaunchedEffect(idPassagem) { viewModel.carregar(idPassagem) }

        BilheteScreen(
            state = state,
            onClickVoltar = onClickVoltar,
            onCapturar = viewModel::aoCapturar,
            onCompartilhar = {
                val uri = viewModel.uriParaCompartilhar() ?: return@BilheteScreen
                val envio = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                contexto.startActivity(Intent.createChooser(envio, "Enviar bilhete"))
            },
        )
    }
}