package dev.matheus.fluviapp.extensions

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import dev.matheus.fluviapp.navigation.destinations.ARG_EMAIL_PREFILL
import dev.matheus.fluviapp.navigation.destinations.FluviAppGraphDestinations
import dev.matheus.fluviapp.navigation.destinations.FluviAppNavComposableDestinations

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
    navegaLimpo(FluviAppGraphDestinations.LoginGraph.route)
}

fun NavHostController.navegaParaMainScreenGraph() {
    navegaLimpo(FluviAppGraphDestinations.MainScreenGraph.route)
}

fun NavHostController.navegaParaRecuperarSenha(email: String) {
    navigate("${FluviAppGraphDestinations.RecuperarSenha.route}?$ARG_EMAIL_PREFILL=$email")
}

fun NavHostController.navegaParaPesquisarViagemGraph() {
    navegaDireto(FluviAppGraphDestinations.PesquisarViagemGraph.route)
}

fun NavHostController.navegaParaPesquisarPassagemGraph() {
    navegaDireto(FluviAppGraphDestinations.PesquisarPassagemGraph.route)
}

fun NavHostController.navegaParaFormularioViagem(idViagem: String? = null) {
    navegaDireto("${FluviAppNavComposableDestinations.FormViagemNavComposable.route}/$idViagem")
}

fun NavHostController.navegaParaResultadosPesquisarViagem() {
    navegaDireto(FluviAppNavComposableDestinations.ResultPesquisarViagemNavComposable.route)
}

fun NavHostController.navegaParaDetalhesViagem(idViagem: String) {
    navegaDireto("${FluviAppNavComposableDestinations.DetalhesViagemNavComposable.route}/$idViagem")
}

fun NavHostController.navegarParaFormularioPassagemComViagem(idViagem: String, idPassagem: String? = null) {
    navegaDireto("${FluviAppNavComposableDestinations.FormPassagemNavComposable.route}/$idViagem/$idPassagem")
}

fun NavHostController.navegaParaResultadosPesquisarPassagem() {
    navegaDireto(FluviAppNavComposableDestinations.ResultPesquisarPassagemNavComposable.route)
}

fun NavHostController.navegaParaDetalhesPassagem(idPassagem: String) {
    navegaDireto("${FluviAppNavComposableDestinations.DetalhesPassagemNavComposable.route}/$idPassagem")
}

fun NavHostController.navegaParaBalancos() {
    navegaDireto(FluviAppNavComposableDestinations.BalancoNavComposable.route)
}

fun NavHostController.navegaParaFormularioAgente(idAgente: String? = null) {
    navegaDireto("${FluviAppNavComposableDestinations.FormAgenteNavComposable.route}/$idAgente")
}

fun NavHostController.navegaParaResultPesquisarAgente() {
    navegaDireto("${FluviAppNavComposableDestinations.ResultPesquisarAgenteNavComposable.route}/null")
}

