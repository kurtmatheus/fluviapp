package dev.matheus.fluviapp.navigation.graphs

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.navigation
import dev.matheus.fluviapp.navigation.destinations.FluviAppGraphDestinations
import dev.matheus.fluviapp.navigation.destinations.FluviAppNavComposableDestinations
import dev.matheus.fluviapp.navigation.navcomposables.viagem.detalhesViagemNavComposable
import dev.matheus.fluviapp.navigation.navcomposables.viagem.formPesquisarViagemNavComposable
import dev.matheus.fluviapp.navigation.navcomposables.viagem.resultPesquisarViagemNavComposable

fun NavGraphBuilder.pesquisarViagemGraph(
    navController: NavController,
    onClickVoltar: () -> Unit,
    onNavegaParaMainScreen: () -> Unit,
    onNavegaParaResultadosPesquisa: () -> Unit,
    onNavegaParaFormularioViagem: (String) -> Unit,
    onNavegaParaFormularioPassagem: (String) -> Unit,
    onNavegaParaDetalhesViagem: (String) -> Unit
) {
    navigation(
        route = FluviAppGraphDestinations.PesquisarViagemGraph.route,
        startDestination = FluviAppNavComposableDestinations.FormPesquisarViagemNavComposable.route
    ) {
        formPesquisarViagemNavComposable(
            navController = navController,
            onClickVoltar = onNavegaParaMainScreen,
            onNavegaParaResultadosPesquisa = onNavegaParaResultadosPesquisa
        )
        resultPesquisarViagemNavComposable(
            navController = navController,
            onClickVoltar = onClickVoltar,
            onNavegParaDetalhesViagem = onNavegaParaDetalhesViagem
        )
        detalhesViagemNavComposable(
            navController = navController,
            onClickVoltar = onClickVoltar,
            onNavegaParaMainScreen = onNavegaParaMainScreen,
            onNavegaParaFormularioViagem = onNavegaParaFormularioViagem,
            onNavegaParaFormularioPassagem = onNavegaParaFormularioPassagem
        )
    }
}