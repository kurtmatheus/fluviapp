package br.com.gruponaveg.extensions

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import br.com.gruponaveg.navigation.destinations.NavegAppGraphDestinations
import br.com.gruponaveg.navigation.destinations.NavegAppNavComposableDestinations

fun NavHostController.navegaDireto(rota: String) = this.navigate(rota) {
    popUpTo(this@navegaDireto.graph.findStartDestination().id) {
        saveState = true
    }
    launchSingleTop = true
    restoreState = true
}

fun NavHostController.navegaLimpo(rota: String) = this.navigate(rota) {
    popUpTo(0)
}

fun NavHostController.navegaParaLoginGraph() {
    navegaLimpo(NavegAppGraphDestinations.LoginGraph.route)
}

fun NavHostController.navegaParaMainScreenGraph() {
    navegaLimpo(NavegAppGraphDestinations.MainScreenGraph.route)
}

fun NavHostController.navegaParaPesquisarViagemGraph() {
    navegaDireto(NavegAppGraphDestinations.PesquisarViagemGraph.route)
}

fun NavHostController.navegaParaPesquisarPassagemGraph() {
    navegaDireto(NavegAppGraphDestinations.PesquisarPassagemGraph.route)
}

fun NavHostController.navegaParaFormularioViagem(idViagem: String? = null) {
    navegaDireto("${NavegAppNavComposableDestinations.FormViagemNavComposable.route}/$idViagem")
}

fun NavHostController.navegaParaResultadosPesquisarViagem() {
    navegaDireto(NavegAppNavComposableDestinations.ResultPesquisarViagemNavComposable.route)
}

fun NavHostController.navegaParaDetalhesViagem(idViagem: String) {
    navegaDireto("${NavegAppNavComposableDestinations.DetalhesViagemNavComposable.route}/$idViagem")
}

fun NavHostController.navegarParaFormularioPassagemComViagem(idViagem: String, idPassagem: String? = null) {
    navegaDireto("${NavegAppNavComposableDestinations.FormPassagemNavComposable.route}/$idViagem/$idPassagem")
}

fun NavHostController.navegaParaResultadosPesquisarPassagem() {
    navegaDireto(NavegAppNavComposableDestinations.ResultPesquisarPassagemNavComposable.route)
}

fun NavHostController.navegaParaDetalhesPassagem(idPassagem: String) {
    navegaDireto("${NavegAppNavComposableDestinations.DetalhesPassagemNavComposable.route}/$idPassagem")
}

fun NavHostController.navegaParaBalancos() {
    navegaDireto(NavegAppNavComposableDestinations.BalancoNavComposable.route)
}

fun NavHostController.navegaParaFormularioAgente(idAgente: String? = null) {
    navegaDireto("${NavegAppNavComposableDestinations.FormAgenteNavComposable.route}/$idAgente")
}

fun NavHostController.navegaParaResultPesquisarAgente() {
    navegaDireto("${NavegAppNavComposableDestinations.ResultPesquisarAgenteNavComposable.route}/null")
}

