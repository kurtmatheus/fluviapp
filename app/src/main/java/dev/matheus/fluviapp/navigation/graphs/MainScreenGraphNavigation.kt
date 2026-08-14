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
    onNavegaParaEmbarque: () -> Unit,
    onNavegaParaEmissao: (String) -> Unit,
    onNavegaParaPesquisaDePassagem: () -> Unit,
    onNavegaParaFormularioNovoFuncionario: () -> Unit,
    onNavegaParaFormularioPesquisaFuncionario: () -> Unit,
    onNavegaParaFormularioNovaEmpresa: () -> Unit,
    onNavegaParaFormularioPesquisaEmpresa: () -> Unit,
    onNavegaParaFormularioNovaEmbarcacao: () -> Unit,
    onNavegaParaFormularioPesquisaEmbarcacao: () -> Unit,
    onNavegaParaFormularioNovaLocalidade: () -> Unit,
    onNavegaParaFormularioPesquisaLocalidade: () -> Unit,
    onNavegaParaFormularioNovoPorto: () -> Unit,
    onNavegaParaFormularioPesquisaPorto: () -> Unit,
    onNavegaParaFormularioNovoUsuario: () -> Unit,
    onNavegaParaFormularioPesquisaUsuario: () -> Unit,
    onNavegaParaFormularioNovaRota: () -> Unit,
    onNavegaParaFormularioPesquisaRota: () -> Unit,
    onNavegaParaFormularioNovaViagem: () -> Unit,
    onNavegaParaFormularioPesquisaViagem: () -> Unit,
) {
    navigation(
        route = FluviAppGraphDestinations.MainScreenGraph.route,
        startDestination = FluviAppNavComposableDestinations.MainScreenNavComposable.route

    ) {
        mainScreenNavComposable(
            onNavegaParaLogin = onNavegaParaLogin,
            onNavegaParaEmbarque = onNavegaParaEmbarque,
            onNavegaParaEmissao = onNavegaParaEmissao,
            onNavegaParaPesquisaDePassagem = onNavegaParaPesquisaDePassagem,
            onNavegaParaFormularioNovoFuncionario = onNavegaParaFormularioNovoFuncionario,
            onNavegaParaFormularioPesquisaFuncionario = onNavegaParaFormularioPesquisaFuncionario,
            onNavegaParaFormularioNovaEmpresa = onNavegaParaFormularioNovaEmpresa,
            onNavegaParaFormularioPesquisaEmpresa = onNavegaParaFormularioPesquisaEmpresa,
            onNavegaParaFormularioNovaEmbarcacao = onNavegaParaFormularioNovaEmbarcacao,
            onNavegaParaFormularioPesquisaEmbarcacao = onNavegaParaFormularioPesquisaEmbarcacao,
            onNavegaParaFormularioNovaLocalidade = onNavegaParaFormularioNovaLocalidade,
            onNavegaParaFormularioPesquisaLocalidade = onNavegaParaFormularioPesquisaLocalidade,
            onNavegaParaFormularioNovoPorto = onNavegaParaFormularioNovoPorto,
            onNavegaParaFormularioPesquisaPorto = onNavegaParaFormularioPesquisaPorto,
            onNavegaParaFormularioNovoUsuario = onNavegaParaFormularioNovoUsuario,
            onNavegaParaFormularioPesquisaUsuario = onNavegaParaFormularioPesquisaUsuario,
            onNavegaParaFormularioNovaRota = onNavegaParaFormularioNovaRota,
            onNavegaParaFormularioPesquisaRota = onNavegaParaFormularioPesquisaRota,
            onNavegaParaFormularioNovaViagem = onNavegaParaFormularioNovaViagem,
            onNavegaParaFormularioPesquisaViagem = onNavegaParaFormularioPesquisaViagem,
        )
    }
}