package dev.matheus.fluviapp.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import dev.matheus.fluviapp.extensions.navegaParaContagemPassagem
import dev.matheus.fluviapp.extensions.navegaParaEmbarque
import dev.matheus.fluviapp.extensions.navegaParaDetalhesPassagem
import dev.matheus.fluviapp.extensions.navegaParaFormularioFuncionario
import dev.matheus.fluviapp.extensions.navegaParaLoginGraph
import dev.matheus.fluviapp.extensions.navegaParaMainScreenGraph
import dev.matheus.fluviapp.extensions.navegaParaPesquisarPassagemGraph
import dev.matheus.fluviapp.extensions.navegaParaPrimeiroAcesso
import dev.matheus.fluviapp.extensions.navegaParaRecuperarSenha
import dev.matheus.fluviapp.extensions.navegaParaFormularioEmpresa
import dev.matheus.fluviapp.extensions.navegaParaFormularioEmbarcacao
import dev.matheus.fluviapp.extensions.navegaParaResultPesquisarFuncionario
import dev.matheus.fluviapp.extensions.navegaParaResultadosPesquisarPassagem
import dev.matheus.fluviapp.extensions.navegarParaFormularioPassagemComViagem
import dev.matheus.fluviapp.navigation.destinations.ARG_EMAIL_PREFILL
import dev.matheus.fluviapp.navigation.destinations.FluviAppGraphDestinations
import dev.matheus.fluviapp.navigation.graphs.loginGraph
import dev.matheus.fluviapp.navigation.graphs.mainScreenGraph
import dev.matheus.fluviapp.extensions.navegaParaResultPesquisarEmpresa
import dev.matheus.fluviapp.extensions.navegaParaResultPesquisarEmbarcacao
import dev.matheus.fluviapp.extensions.navegaParaFormularioLocalidade
import dev.matheus.fluviapp.extensions.navegaParaResultPesquisarLocalidade
import dev.matheus.fluviapp.navigation.navcomposables.empresa.formEmpresaNavComposable
import dev.matheus.fluviapp.navigation.navcomposables.empresa.resultSearchEmpresaNavComposable
import dev.matheus.fluviapp.navigation.navcomposables.embarcacao.formEmbarcacaoNavComposable
import dev.matheus.fluviapp.navigation.navcomposables.embarcacao.resultSearchEmbarcacaoNavComposable
import dev.matheus.fluviapp.navigation.navcomposables.localidade.formLocalidadeNavComposable
import dev.matheus.fluviapp.navigation.navcomposables.localidade.resultSearchLocalidadeNavComposable
import dev.matheus.fluviapp.extensions.navegaParaFormularioPorto
import dev.matheus.fluviapp.extensions.navegaParaResultPesquisarPorto
import dev.matheus.fluviapp.extensions.navegaParaSelecaoVinculo
import dev.matheus.fluviapp.extensions.navegaParaFormularioUsuario
import dev.matheus.fluviapp.extensions.navegaParaResultPesquisarUsuario
import dev.matheus.fluviapp.extensions.navegaParaFormularioRota
import dev.matheus.fluviapp.extensions.navegaParaResultPesquisarRota
import dev.matheus.fluviapp.navigation.navcomposables.porto.formPortoNavComposable
import dev.matheus.fluviapp.navigation.navcomposables.porto.resultSearchPortoNavComposable
import dev.matheus.fluviapp.navigation.navcomposables.usuario.formUsuarioNavComposable
import dev.matheus.fluviapp.navigation.navcomposables.usuario.resultSearchUsuarioNavComposable
import dev.matheus.fluviapp.navigation.navcomposables.rota.formRotaNavComposable
import dev.matheus.fluviapp.navigation.navcomposables.rota.resultSearchRotaNavComposable
import dev.matheus.fluviapp.navigation.navcomposables.viagem.formViagemNavComposable
import dev.matheus.fluviapp.navigation.navcomposables.viagem.resultSearchViagemNavComposable
import dev.matheus.fluviapp.extensions.navegaParaFormularioViagem
import dev.matheus.fluviapp.extensions.navegaParaResultPesquisarViagem
import dev.matheus.fluviapp.navigation.graphs.pesquisarPassagemGraph
import dev.matheus.fluviapp.navigation.graphs.selecaoVinculoGraph
import dev.matheus.fluviapp.navigation.graphs.splashGraph
import dev.matheus.fluviapp.navigation.navcomposables.contagem.contagemPassagemNavComposable
import dev.matheus.fluviapp.navigation.navcomposables.funcionario.formFuncionarioNavComposable
import dev.matheus.fluviapp.navigation.navcomposables.funcionario.resultSearchFuncionarioNavComposable
import dev.matheus.fluviapp.navigation.navcomposables.passagem.formPassagemNavComposable
import dev.matheus.fluviapp.navigation.navcomposables.passagem.embarqueNavComposable

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun FluviAppNavHost(
    navController: NavHostController,
) {
    NavHost(
        navController = navController,
        startDestination = FluviAppGraphDestinations.SplashScreen.route
    ) {
        splashGraph(
            onNavegaParaLogin = {
                navController.navegaParaLoginGraph()
            },
            onNavegaParaHome = {
                navController.navegaParaMainScreenGraph()
            },
            onNavegaParaSelecaoVinculo = {
                navController.navegaParaSelecaoVinculo()
            }
        )

        selecaoVinculoGraph(
            onNavegaParaHome = {
                navController.navegaParaMainScreenGraph()
            }
        )

        loginGraph(
            onNavegarParaMainScreen = {
                navController.navegaParaMainScreenGraph()
            },
            onNavegaParaPrimeiroAcesso = { email ->
                navController.navegaParaPrimeiroAcesso(email)
            },
            onNavegaParaRecuperarSenha = { email ->
                navController.navegaParaRecuperarSenha(email)
            },
            onVoltarParaLogin = {
                navController.popBackStack()
            },
            // Volta ao login já com o e-mail preenchido: quem acabou de criar a senha entra de novo
            // com ela, e digitar o e-mail outra vez seria atrito puro (ADR-0015 §2.1).
            onPrimeiroAcessoConcluido = { email ->
                navController.getBackStackEntry(FluviAppGraphDestinations.LoginGraph.route)
                    .savedStateHandle[ARG_EMAIL_PREFILL] = email
                navController.popBackStack()
            }
        )

        mainScreenGraph(
            onNavegaParaLogin = {
                navController.navegaParaLoginGraph()
            },
            onNavegaParaFormularioNovaPassagemComViagem = { idViagem ->
                navController.navegarParaFormularioPassagemComViagem(idViagem)
            },
            onNavegaParaFormularioPesquisaPassagem = {
                navController.navegaParaPesquisarPassagemGraph()
            },
            onNavegaParaEmbarque = {
                navController.navegaParaEmbarque()
            },
            onNavegaParaContagemPassagem = {
                navController.navegaParaContagemPassagem()
            },
            onNavegaParaFormularioNovoFuncionario = {
                navController.navegaParaFormularioFuncionario()
            },
            onNavegaParaFormularioPesquisaFuncionario = {
                navController.navegaParaResultPesquisarFuncionario()
            },
            onNavegaParaFormularioNovaEmpresa = {
                navController.navegaParaFormularioEmpresa()
            },
            onNavegaParaFormularioPesquisaEmpresa = {
                navController.navegaParaResultPesquisarEmpresa()
            },
            onNavegaParaFormularioNovaEmbarcacao = {
                navController.navegaParaFormularioEmbarcacao()
            },
            onNavegaParaFormularioPesquisaEmbarcacao = {
                navController.navegaParaResultPesquisarEmbarcacao()
            },
            onNavegaParaFormularioNovaLocalidade = {
                navController.navegaParaFormularioLocalidade()
            },
            onNavegaParaFormularioPesquisaLocalidade = {
                navController.navegaParaResultPesquisarLocalidade()
            },
            onNavegaParaFormularioNovoPorto = {
                navController.navegaParaFormularioPorto()
            },
            onNavegaParaFormularioNovaRota = {
                navController.navegaParaFormularioRota()
            },
            onNavegaParaFormularioPesquisaRota = {
                navController.navegaParaResultPesquisarRota()
            },
            onNavegaParaFormularioNovaViagem = {
                navController.navegaParaFormularioViagem()
            },
            onNavegaParaFormularioPesquisaViagem = {
                navController.navegaParaResultPesquisarViagem()
            },
            onNavegaParaFormularioNovoUsuario = {
                navController.navegaParaFormularioUsuario()
            },
            onNavegaParaFormularioPesquisaUsuario = {
                navController.navegaParaResultPesquisarUsuario()
            },
            onNavegaParaFormularioPesquisaPorto = {
                navController.navegaParaResultPesquisarPorto()
            }
        )

        formPassagemNavComposable(
            onCLickVoltar = {
                navController.navigateUp()
            },
            onNavegaParaDetalhesPassagem = {
                navController.navegaParaDetalhesPassagem(it)
            }
        )

        pesquisarPassagemGraph(
            navController = navController,
            onClickVoltar = {
                navController.navigateUp()
            },
            onNavegaParaMainScreen = {
                navController.navegaParaMainScreenGraph()
            },
            onNavegaParaResultadosPesquisa = {
                navController.navegaParaResultadosPesquisarPassagem()
            },
            onNavegaParaDetalhesPassagem = {
                navController.navegaParaDetalhesPassagem(it)
            },
            onNavegaParaFormularioNovaPassagem = {
                navController.navegarParaFormularioPassagemComViagem(it)
            },
            onNavegaParaFormularioEditarPassagem = { idViagem, idPassagem ->
                navController.navegarParaFormularioPassagemComViagem(idViagem, idPassagem)
            }
        )

        contagemPassagemNavComposable(
            navController = navController,
            onClickVoltar = {
                navController.navigateUp()
            }
        )

        embarqueNavComposable(
            onClickVoltar = {
                navController.navigateUp()
            }
        )

        formFuncionarioNavComposable(
            onClickVoltar = {
                navController.navigateUp()
            },
            onNavegaParaMainScreen = {
                navController.navegaParaMainScreenGraph()
            }
        )

        resultSearchFuncionarioNavComposable(
            onClickVoltar = {
                navController.navigateUp()
            },
            onNavegaParaEditorFuncionario = {
                navController.navegaParaFormularioFuncionario(it)
            }
        )

        formEmpresaNavComposable(
            onNavegaParaMainScreen = {
                navController.navegaParaMainScreenGraph()
            },
            onClickVoltar = {
                navController.navigateUp()
            }
        )

        resultSearchEmpresaNavComposable(
            onClickVoltar = {
                navController.navigateUp()
            },
            onNavegaParaEditorEmpresa = {
                navController.navegaParaFormularioEmpresa(it)
            }
        )

        formEmbarcacaoNavComposable(
            onNavegaParaMainScreen = {
                navController.navegaParaMainScreenGraph()
            },
            onClickVoltar = {
                navController.navigateUp()
            }
        )

        resultSearchEmbarcacaoNavComposable(
            onClickVoltar = {
                navController.navigateUp()
            },
            onNavegaParaEditorEmbarcacao = {
                navController.navegaParaFormularioEmbarcacao(it)
            }
        )

        formLocalidadeNavComposable(
            onNavegaParaMainScreen = {
                navController.navegaParaMainScreenGraph()
            },
            onClickVoltar = {
                navController.navigateUp()
            }
        )

        resultSearchLocalidadeNavComposable(
            onClickVoltar = {
                navController.navigateUp()
            },
            onNavegaParaEditorLocalidade = {
                navController.navegaParaFormularioLocalidade(it)
            }
        )

        formPortoNavComposable(
            onNavegaParaMainScreen = {
                navController.navegaParaMainScreenGraph()
            },
            onClickVoltar = {
                navController.navigateUp()
            }
        )

        resultSearchPortoNavComposable(
            onClickVoltar = {
                navController.navigateUp()
            },
            onNavegaParaEditorPorto = {
                navController.navegaParaFormularioPorto(it)
            }
        )

        formUsuarioNavComposable(
            onNavegaParaMainScreen = {
                navController.navegaParaMainScreenGraph()
            },
            onClickVoltar = {
                navController.navigateUp()
            }
        )

        resultSearchUsuarioNavComposable(
            onClickVoltar = {
                navController.navigateUp()
            }
        )

        formRotaNavComposable(
            onNavegaParaMainScreen = {
                navController.navegaParaMainScreenGraph()
            },
            onClickVoltar = {
                navController.navigateUp()
            }
        )

        resultSearchRotaNavComposable(
            onClickVoltar = {
                navController.navigateUp()
            }
        )

        formViagemNavComposable(
            onNavegaParaMainScreen = {
                navController.navegaParaMainScreenGraph()
            },
            onClickVoltar = {
                navController.navigateUp()
            }
        )

        resultSearchViagemNavComposable(
            onClickVoltar = {
                navController.navigateUp()
            }
        )
    }
}
