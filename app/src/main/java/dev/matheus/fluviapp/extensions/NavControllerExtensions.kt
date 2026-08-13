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

fun NavHostController.navegaParaPesquisarPassagemGraph() {
    navegaDireto(FluviAppGraphDestinations.PesquisarPassagemGraph.route)
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

/**
 * Abre a emissão **sobre uma saída** (F9.5): a chave da ocorrência vai no caminho, e é o card de saída do
 * Início que a fornece — a emissão nunca pergunta data nem hora ([ADR-0028] D5).
 */
fun NavHostController.navegaParaEmissao(chaveDaOcorrencia: String) {
    navegaDireto(FluviAppNavComposableDestinations.EmissaoNavComposable.comOcorrencia(chaveDaOcorrencia))
}

/** Abre o **bilhete** de uma passagem — o mesmo destino para quem emitiu agora e para quem foi buscar. */
fun NavHostController.navegaParaBilhete(idPassagem: String) {
    navegaDireto(FluviAppNavComposableDestinations.BilheteNavComposable.comPassagem(idPassagem))
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

fun NavHostController.navegaParaFormularioViagem() {
    navegaDireto(FluviAppNavComposableDestinations.FormViagemNavComposable.route)
}

fun NavHostController.navegaParaResultPesquisarViagem() {
    navegaDireto(FluviAppNavComposableDestinations.ResultPesquisarViagemNavComposable.route)
}

