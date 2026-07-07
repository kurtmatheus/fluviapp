package dev.matheus.fluviapp.navigation.graphs

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.navigation
import dev.matheus.fluviapp.navigation.destinations.FluviAppGraphDestinations
import dev.matheus.fluviapp.navigation.destinations.FluviAppNavComposableDestinations
import dev.matheus.fluviapp.navigation.navcomposables.passagem.detalhesPassagemNavComposable
import dev.matheus.fluviapp.navigation.navcomposables.passagem.formPesquisarPassagemNavComposable
import dev.matheus.fluviapp.navigation.navcomposables.passagem.resultPassagemNavComposable

fun NavGraphBuilder.pesquisarPassagemGraph(
    navController: NavController,
    onClickVoltar: () -> Unit,
    onNavegaParaMainScreen: () -> Unit,
    onNavegaParaResultadosPesquisa: () -> Unit,
    onNavegaParaDetalhesPassagem: (String) -> Unit,
    onNavegaParaFormularioNovaPassagem: (String) -> Unit,
    onNavegaParaFormularioEditarPassagem: (String, String) -> Unit
) {
    navigation(
        route = FluviAppGraphDestinations.PesquisarPassagemGraph.route,
        startDestination = FluviAppNavComposableDestinations.FormPesquisarPassagemNavComposable.route
    ) {

        formPesquisarPassagemNavComposable(
            navController = navController,
            onClickVoltar = onClickVoltar,
            onNavegaParaResultadosPesquisa = onNavegaParaResultadosPesquisa
        )

        resultPassagemNavComposable(
            navController = navController,
            onClickVoltar = onClickVoltar,
            onNavegaParaDetalhesPassagem = onNavegaParaDetalhesPassagem
        )

        detalhesPassagemNavComposable(
            onNavegaParaMainScreen = onNavegaParaMainScreen,
            onNavegaParaFormularioNovaPassagem = onNavegaParaFormularioNovaPassagem,
            onNavegaParaFormularioEditarPassagem = onNavegaParaFormularioEditarPassagem
        )
    }
}
