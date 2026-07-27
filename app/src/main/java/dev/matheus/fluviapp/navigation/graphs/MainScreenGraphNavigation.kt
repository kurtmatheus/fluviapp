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
    onNavegaParaEmbarque: () -> Unit,
    onNavegaParaRelatorios: () -> Unit,
    onNavegaParaFormularioNovoFuncionario: () -> Unit,
    onNavegaParaFormularioPesquisaFuncionario: () -> Unit,
    onNavegaParaFormularioNovaEmpresa: () -> Unit,
    onNavegaParaFormularioPesquisaEmpresa: () -> Unit,
    onNavegaParaFormularioNovoNavio: () -> Unit,
    onNavegaParaFormularioPesquisaNavio: () -> Unit
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
            onNavegaParaEmbarque = onNavegaParaEmbarque,
            onNavegaParaBalanco = onNavegaParaRelatorios,
            onNavegaParaFormularioNovoFuncionario = onNavegaParaFormularioNovoFuncionario,
            onNavegaParaFormularioPesquisaFuncionario = onNavegaParaFormularioPesquisaFuncionario,
            onNavegaParaFormularioNovaEmpresa = onNavegaParaFormularioNovaEmpresa,
            onNavegaParaFormularioPesquisaEmpresa = onNavegaParaFormularioPesquisaEmpresa,
            onNavegaParaFormularioNovoNavio = onNavegaParaFormularioNovoNavio,
            onNavegaParaFormularioPesquisaNavio = onNavegaParaFormularioPesquisaNavio
        )
    }
}