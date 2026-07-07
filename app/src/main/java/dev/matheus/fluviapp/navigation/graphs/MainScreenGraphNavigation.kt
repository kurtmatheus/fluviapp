package dev.matheus.fluviapp.navigation.graphs

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.navigation.NavGraphBuilder
import androidx.navigation.navigation
import dev.matheus.fluviapp.navigation.destinations.FluviAppGraphDestinations
import dev.matheus.fluviapp.navigation.destinations.FluviAppNavComposableDestinations
import dev.matheus.fluviapp.navigation.navcomposables.mainScreenNavComposable

@RequiresApi(Build.VERSION_CODES.S)
fun NavGraphBuilder.mainScreenGraph(
    onNavegaParaLogin: () -> Unit,
    onNavegaParaFormularioNovaViagem: () -> Unit,
    onNavegaParaFormularioPesquisaViagem: () -> Unit,
    onNavegaParaFormularioNovaPassagemComViagem: (String) -> Unit,
    onNavegaParaFormularioPesquisaPassagem: () -> Unit,
    onNavegaParaRelatorios: () -> Unit,
    onNavegaParaFormularioNovoAgente: () -> Unit,
    onNavegaParaFormularioPesquisaAgente: () -> Unit
) {
    navigation(
        route = FluviAppGraphDestinations.MainScreenGraph.route,
        startDestination = FluviAppNavComposableDestinations.MainScreenNavComposable.route

    ) {
        mainScreenNavComposable(
            onNavegaParaLogin = onNavegaParaLogin,
            onNavegaParaFormularioNovaViagem = onNavegaParaFormularioNovaViagem,
            onNavegaParaFormularioPesquisaViagem = onNavegaParaFormularioPesquisaViagem,
            onNavegaParaFormularioNovaPassagemComViagem = onNavegaParaFormularioNovaPassagemComViagem,
            onNavegaParaFormularioPesquisaPassagem = onNavegaParaFormularioPesquisaPassagem,
            onNavegaParaBalanco = onNavegaParaRelatorios,
            onNavegaParaFormularioNovoAgente = onNavegaParaFormularioNovoAgente,
            onNavegaParaFormularioPesquisaAgente = onNavegaParaFormularioPesquisaAgente

        )
    }
}