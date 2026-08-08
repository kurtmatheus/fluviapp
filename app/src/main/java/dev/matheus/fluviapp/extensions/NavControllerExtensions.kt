package dev.matheus.fluviapp.extensions

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import dev.matheus.fluviapp.navigation.destinations.ARG_EMAIL_PREFILL
import dev.matheus.fluviapp.navigation.destinations.ARG_EMAIL_PRIMEIRO_ACESSO
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

/**
 * A seleção de contexto (F6.4) entra **limpa**, como o login e o painel: ela não é um passo dentro de um
 * fluxo, é a porta — e ter "voltar" para a splash daria a impressão de que dá para pular a pergunta.
 */
fun NavHostController.navegaParaSelecaoVinculo() {
    navegaLimpo(FluviAppGraphDestinations.SelecaoVinculo.route)
}

fun NavHostController.navegaParaRecuperarSenha(email: String) {
    navigate("${FluviAppGraphDestinations.RecuperarSenha.route}?$ARG_EMAIL_PREFILL=$email")
}

fun NavHostController.navegaParaPrimeiroAcesso(email: String) {
    navigate("${FluviAppGraphDestinations.PrimeiroAcesso.route}?$ARG_EMAIL_PRIMEIRO_ACESSO=$email")
}

fun NavHostController.navegaParaPesquisarViagemGraph() {
    navegaDireto(FluviAppGraphDestinations.PesquisarViagemGraph.route)
}

fun NavHostController.navegaParaPesquisarPassagemGraph() {
    navegaDireto(FluviAppGraphDestinations.PesquisarPassagemGraph.route)
}

fun NavHostController.navegaParaFormularioViagem(idViagem: String = "") {
    navegaDireto("${FluviAppNavComposableDestinations.FormViagemNavComposable.route}?idViagem=$idViagem")
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

fun NavHostController.navegaParaContagemPassagem() {
    navegaDireto(FluviAppNavComposableDestinations.ContagemPassagemNavComposable.route)
}

fun NavHostController.navegaParaEmbarque() {
    navegaDireto(FluviAppNavComposableDestinations.EmbarqueNavComposable.route)
}

fun NavHostController.navegaParaFormularioFuncionario(idFuncionario: String = "") {
    navegaDireto("${FluviAppNavComposableDestinations.FormFuncionarioNavComposable.route}?idFuncionario=$idFuncionario")
}

fun NavHostController.navegaParaResultPesquisarFuncionario() {
    navegaDireto(FluviAppNavComposableDestinations.ResultPesquisarFuncionarioNavComposable.route)
}

fun NavHostController.navegaParaFormularioEmpresa(idEmpresa: String = "") {
    navegaDireto("${FluviAppNavComposableDestinations.FormEmpresaNavComposable.route}?idEmpresa=$idEmpresa")
}

fun NavHostController.navegaParaFormularioEmbarcacao(idEmbarcacao: String = "") {
    navegaDireto("${FluviAppNavComposableDestinations.FormEmbarcacaoNavComposable.route}?idEmbarcacao=$idEmbarcacao")
}

fun NavHostController.navegaParaResultPesquisarEmpresa() {
    navegaDireto(FluviAppNavComposableDestinations.ResultPesquisarEmpresaNavComposable.route)
}

fun NavHostController.navegaParaResultPesquisarEmbarcacao() {
    navegaDireto(FluviAppNavComposableDestinations.ResultPesquisarEmbarcacaoNavComposable.route)
}

fun NavHostController.navegaParaFormularioLocalidade(idLocalidade: String = "") {
    navegaDireto("${FluviAppNavComposableDestinations.FormLocalidadeNavComposable.route}?idLocalidade=$idLocalidade")
}

fun NavHostController.navegaParaResultPesquisarLocalidade() {
    navegaDireto(FluviAppNavComposableDestinations.ResultPesquisarLocalidadeNavComposable.route)
}

fun NavHostController.navegaParaFormularioPorto(idPorto: String = "") {
    navegaDireto("${FluviAppNavComposableDestinations.FormPortoNavComposable.route}?idPorto=$idPorto")
}

fun NavHostController.navegaParaResultPesquisarPorto() {
    navegaDireto(FluviAppNavComposableDestinations.ResultPesquisarPortoNavComposable.route)
}

fun NavHostController.navegaParaFormularioUsuario() {
    navegaDireto(FluviAppNavComposableDestinations.FormUsuarioNavComposable.route)
}

fun NavHostController.navegaParaResultPesquisarUsuario() {
    navegaDireto(FluviAppNavComposableDestinations.ResultPesquisarUsuarioNavComposable.route)
}

fun NavHostController.navegaParaFormularioRota() {
    navegaDireto(FluviAppNavComposableDestinations.FormRotaNavComposable.route)
}

fun NavHostController.navegaParaResultPesquisarRota() {
    navegaDireto(FluviAppNavComposableDestinations.ResultPesquisarRotaNavComposable.route)
}

