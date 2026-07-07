package br.com.gruponaveg.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import br.com.gruponaveg.extensions.navegaParaBalancos
import br.com.gruponaveg.extensions.navegaParaDetalhesPassagem
import br.com.gruponaveg.extensions.navegaParaDetalhesViagem
import br.com.gruponaveg.extensions.navegaParaFormularioAgente
import br.com.gruponaveg.extensions.navegaParaFormularioViagem
import br.com.gruponaveg.extensions.navegaParaLoginGraph
import br.com.gruponaveg.extensions.navegaParaMainScreenGraph
import br.com.gruponaveg.extensions.navegaParaPesquisarPassagemGraph
import br.com.gruponaveg.extensions.navegaParaPesquisarViagemGraph
import br.com.gruponaveg.extensions.navegaParaResultPesquisarAgente
import br.com.gruponaveg.extensions.navegaParaResultadosPesquisarPassagem
import br.com.gruponaveg.extensions.navegaParaResultadosPesquisarViagem
import br.com.gruponaveg.extensions.navegarParaFormularioPassagemComViagem
import br.com.gruponaveg.navigation.destinations.NavegAppGraphDestinations
import br.com.gruponaveg.navigation.graphs.loginGraph
import br.com.gruponaveg.navigation.graphs.mainScreenGraph
import br.com.gruponaveg.navigation.graphs.pesquisarPassagemGraph
import br.com.gruponaveg.navigation.graphs.pesquisarViagemGraph
import br.com.gruponaveg.navigation.graphs.splashGraph
import br.com.gruponaveg.navigation.navcomposables.faturamento.balancoNavComposable
import br.com.gruponaveg.navigation.navcomposables.agente.formAgenteNavComposable
import br.com.gruponaveg.navigation.navcomposables.agente.resultSearchAgenteNavComposable
import br.com.gruponaveg.navigation.navcomposables.passagem.formPassagemNavComposable
import br.com.gruponaveg.navigation.navcomposables.viagem.formViagemNavComposable

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun NavegAppNavHost(
    navController: NavHostController,
) {
    NavHost(
        navController = navController,
        startDestination = NavegAppGraphDestinations.SplashScreen.route
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
            }
        )

        formViagemNavComposable(
            onCLickVoltar = {
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
    }
}
