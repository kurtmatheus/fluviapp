package br.com.gruponaveg.navigation.graphs

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.navigation
import br.com.gruponaveg.navigation.destinations.NavegAppGraphDestinations
import br.com.gruponaveg.navigation.destinations.NavegAppNavComposableDestinations
import br.com.gruponaveg.navigation.navcomposables.viagem.detalhesViagemNavComposable
import br.com.gruponaveg.navigation.navcomposables.viagem.formPesquisarViagemNavComposable
import br.com.gruponaveg.navigation.navcomposables.viagem.resultPesquisarViagemNavComposable

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
        route = NavegAppGraphDestinations.PesquisarViagemGraph.route,
        startDestination = NavegAppNavComposableDestinations.FormPesquisarViagemNavComposable.route
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