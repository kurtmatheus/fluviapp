package br.com.gruponaveg.navigation.graphs

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.navigation
import br.com.gruponaveg.navigation.destinations.NavegAppGraphDestinations
import br.com.gruponaveg.navigation.destinations.NavegAppNavComposableDestinations
import br.com.gruponaveg.navigation.navcomposables.passagem.detalhesPassagemNavComposable
import br.com.gruponaveg.navigation.navcomposables.passagem.formPesquisarPassagemNavComposable
import br.com.gruponaveg.navigation.navcomposables.passagem.resultPassagemNavComposable

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
        route = NavegAppGraphDestinations.PesquisarPassagemGraph.route,
        startDestination = NavegAppNavComposableDestinations.FormPesquisarPassagemNavComposable.route
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
