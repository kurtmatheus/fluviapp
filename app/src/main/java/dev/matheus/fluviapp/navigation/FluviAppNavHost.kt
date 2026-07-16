package dev.matheus.fluviapp.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import dev.matheus.fluviapp.extensions.navegaParaBalancos
import dev.matheus.fluviapp.extensions.navegaParaDetalhesPassagem
import dev.matheus.fluviapp.extensions.navegaParaDetalhesViagem
import dev.matheus.fluviapp.extensions.navegaParaFormularioAgente
import dev.matheus.fluviapp.extensions.navegaParaFormularioViagem
import dev.matheus.fluviapp.extensions.navegaParaLoginGraph
import dev.matheus.fluviapp.extensions.navegaParaMainScreenGraph
import dev.matheus.fluviapp.extensions.navegaParaPesquisarPassagemGraph
import dev.matheus.fluviapp.extensions.navegaParaPesquisarViagemGraph
import dev.matheus.fluviapp.extensions.navegaParaRecuperarSenha
import dev.matheus.fluviapp.extensions.navegaParaFormularioEmpresa
import dev.matheus.fluviapp.extensions.navegaParaFormularioNavio
import dev.matheus.fluviapp.extensions.navegaParaResultPesquisarAgente
import dev.matheus.fluviapp.extensions.navegaParaResultadosPesquisarPassagem
import dev.matheus.fluviapp.extensions.navegaParaResultadosPesquisarViagem
import dev.matheus.fluviapp.extensions.navegarParaFormularioPassagemComViagem
import dev.matheus.fluviapp.navigation.destinations.ARG_EMAIL_PREFILL
import dev.matheus.fluviapp.navigation.destinations.FluviAppGraphDestinations
import dev.matheus.fluviapp.navigation.graphs.loginGraph
import dev.matheus.fluviapp.navigation.graphs.mainScreenGraph
import dev.matheus.fluviapp.extensions.navegaParaResultPesquisarEmpresa
import dev.matheus.fluviapp.extensions.navegaParaResultPesquisarNavio
import dev.matheus.fluviapp.navigation.navcomposables.empresa.formEmpresaNavComposable
import dev.matheus.fluviapp.navigation.navcomposables.empresa.resultSearchEmpresaNavComposable
import dev.matheus.fluviapp.navigation.navcomposables.navio.formNavioNavComposable
import dev.matheus.fluviapp.navigation.navcomposables.navio.resultSearchNavioNavComposable
import dev.matheus.fluviapp.navigation.graphs.pesquisarPassagemGraph
import dev.matheus.fluviapp.navigation.graphs.pesquisarViagemGraph
import dev.matheus.fluviapp.navigation.graphs.splashGraph
import dev.matheus.fluviapp.navigation.navcomposables.faturamento.balancoNavComposable
import dev.matheus.fluviapp.navigation.navcomposables.agente.formAgenteNavComposable
import dev.matheus.fluviapp.navigation.navcomposables.agente.resultSearchAgenteNavComposable
import dev.matheus.fluviapp.navigation.navcomposables.passagem.formPassagemNavComposable
import dev.matheus.fluviapp.navigation.navcomposables.viagem.formViagemNavComposable

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
            }
        )

        loginGraph(
            onNavegarParaMainScreen = {
                navController.navegaParaMainScreenGraph()
            },
            onNavegaParaCadastro = {
                navController.navigate(FluviAppGraphDestinations.Cadastro.route)
            },
            onNavegaParaRecuperarSenha = { email ->
                navController.navegaParaRecuperarSenha(email)
            },
            onVoltarParaLogin = {
                navController.popBackStack()
            },
            onVoltarComEmail = { email ->
                navController.getBackStackEntry(FluviAppGraphDestinations.LoginGraph.route)
                    .savedStateHandle[ARG_EMAIL_PREFILL] = email
                navController.popBackStack()
            }
        )

        mainScreenGraph(
            onNavegaParaLogin = {
                navController.navegaParaLoginGraph()
            },
            onNavegaParaFormularioNovaViagem = {
                navController.navegaParaFormularioViagem()
            },
            onNavegaParaFormularioPesquisaViagem = {
                navController.navegaParaPesquisarViagemGraph()
            },
            onNavegaParaFormularioNovaPassagemComViagem = { idViagem ->
                navController.navegarParaFormularioPassagemComViagem(idViagem)
            },
            onNavegaParaFormularioPesquisaPassagem = {
                navController.navegaParaPesquisarPassagemGraph()
            },
            onNavegaParaRelatorios = {
                navController.navegaParaBalancos()
            },
            onNavegaParaFormularioNovoAgente = {
                navController.navegaParaFormularioAgente()
            },
            onNavegaParaFormularioPesquisaAgente = {
                navController.navegaParaResultPesquisarAgente()
            },
            onNavegaParaFormularioNovaEmpresa = {
                navController.navegaParaFormularioEmpresa()
            },
            onNavegaParaFormularioPesquisaEmpresa = {
                navController.navegaParaResultPesquisarEmpresa()
            },
            onNavegaParaFormularioNovoNavio = {
                navController.navegaParaFormularioNavio()
            },
            onNavegaParaFormularioPesquisaNavio = {
                navController.navegaParaResultPesquisarNavio()
            }
        )

        formViagemNavComposable(
            onClickVoltar = {
                navController.navigateUp()
            },
            onNavegaParaMainScreen = {
                navController.navegaParaMainScreenGraph()
            }
        )

        pesquisarViagemGraph(
            navController = navController,
            onClickVoltar = {
                navController.navigateUp()
            },
            onNavegaParaMainScreen = {
                navController.navegaParaMainScreenGraph()
            },
            onNavegaParaResultadosPesquisa = {
                navController.navegaParaResultadosPesquisarViagem()
            },
            onNavegaParaDetalhesViagem = {
                navController.navegaParaDetalhesViagem(it)
            },
            onNavegaParaFormularioViagem = {
                navController.navegaParaFormularioViagem(it)
            },
            onNavegaParaFormularioPassagem = {
                navController.navegarParaFormularioPassagemComViagem(it)
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

        balancoNavComposable(
            navController = navController,
            onClickVoltar = {
                navController.navigateUp()
            }
        )

        formAgenteNavComposable(
            onClickVoltar = {
                navController.navigateUp()
            },
            onNavegaParaMainScreen = {
                navController.navegaParaMainScreenGraph()
            }
        )

        resultSearchAgenteNavComposable(
            onClickVoltar = {
                navController.navigateUp()
            },
            onNavegaParaEditorAgente = {
                navController.navegaParaFormularioAgente(it)
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

        formNavioNavComposable(
            onNavegaParaMainScreen = {
                navController.navegaParaMainScreenGraph()
            },
            onClickVoltar = {
                navController.navigateUp()
            }
        )

        resultSearchNavioNavComposable(
            onClickVoltar = {
                navController.navigateUp()
            },
            onNavegaParaEditorNavio = {
                navController.navegaParaFormularioNavio(it)
            }
        )
    }
}
