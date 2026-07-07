package br.com.gruponaveg.navigation.graphs

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.navigation.NavGraphBuilder
import androidx.navigation.navigation
import br.com.gruponaveg.navigation.destinations.NavegAppGraphDestinations
import br.com.gruponaveg.navigation.destinations.NavegAppNavComposableDestinations
import br.com.gruponaveg.navigation.navcomposables.mainScreenNavComposable

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
        route = NavegAppGraphDestinations.MainScreenGraph.route,
        startDestination = NavegAppNavComposableDestinations.MainScreenNavComposable.route

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