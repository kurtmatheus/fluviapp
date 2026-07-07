package dev.matheus.fluviapp.navigation.navcomposables

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.model.screendata.DadosBotoesMenus
import dev.matheus.fluviapp.model.screendata.MenuBotoesCategoria
import dev.matheus.fluviapp.navigation.destinations.FluviAppNavComposableDestinations
import dev.matheus.fluviapp.ui.components.RequestMultiplePermissions
import dev.matheus.fluviapp.ui.screens.MainScreen
import dev.matheus.fluviapp.ui.states.MainScreenState
import dev.matheus.fluviapp.ui.states.MainScreenState.HOME
import dev.matheus.fluviapp.ui.viewmodel.MainScreenViewModel
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.S)
fun NavGraphBuilder.mainScreenNavComposable(
    onNavegaParaLogin: () -> Unit,
    onNavegaParaFormularioNovaViagem: () -> Unit,
    onNavegaParaFormularioPesquisaViagem: () -> Unit,
    onNavegaParaFormularioNovaPassagemComViagem: (String) -> Unit,
    onNavegaParaFormularioPesquisaPassagem: () -> Unit,
    onNavegaParaBalanco: () -> Unit,
    onNavegaParaFormularioNovoAgente: () -> Unit,
    onNavegaParaFormularioPesquisaAgente: () -> Unit
) {
    composable(
        route = FluviAppNavComposableDestinations.MainScreenNavComposable.route
    ) {

        val viewModel = hiltViewModel<MainScreenViewModel>()
        val state by viewModel.uiState.collectAsState()

        val coroutineScope = rememberCoroutineScope()
        val context = LocalContext.current

        RequestMultiplePermissions(
            context = context,
            onGrantedPermission = {},
            onDeniedPermission = {},
            permissionsList = listOf(
                android.Manifest.permission.BLUETOOTH,
                android.Manifest.permission.BLUETOOTH_CONNECT
            )
        )

        MainScreen(
            state = state,
            onClickHome = {
                viewModel.atualizaMainPage(HOME)
            },
            onClickMenuPassagens = {
                viewModel.atualizaMainPage(
                    MainScreenState.PASSAGENS(
                        listaBotoesMenus = getListaBotoesMenuPassagens(
                            onNavegaParaFormularioPesquisaPassagem = onNavegaParaFormularioPesquisaPassagem,
                            onNavegaParaRelatorios = onNavegaParaBalanco
                        )
                    )
                )
            },
            onClickMenuOperacoes = {
                viewModel.atualizaMainPage(
                    MainScreenState.OPERACOES(
                        listaBotoesMenus = getListaBotoesMenuOperacoes(
                            onNavegaParaFormularioNovaViagem = onNavegaParaFormularioNovaViagem,
                            onNavegaParaFormularioPesquisaViagem = onNavegaParaFormularioPesquisaViagem,
                            onNavegaParaFormularioNovoAgente = onNavegaParaFormularioNovoAgente,
                            onNavegaParaFormularioPesquisaAgente = onNavegaParaFormularioPesquisaAgente
                        )
                    )
                )
            },
            onClickUsername = {
                viewModel.setExibirUserDialog()
            },
            onDismissUserDialog = {
                viewModel.setExibirUserDialog()
            },
            onClickDeslogar = {
                coroutineScope.launch {
                    viewModel.setExibirUserDialog()
                    viewModel.deslogar()
                    onNavegaParaLogin()

                }
            },
            onClickAdicionarPassagem = onNavegaParaFormularioNovaPassagemComViagem,
            onRefresh = {
                viewModel.refresh()
            }
        )
    }
}

private fun getListaBotoesMenuOperacoes(
    onNavegaParaFormularioNovaViagem: () -> Unit,
    onNavegaParaFormularioPesquisaViagem: () -> Unit,
    onNavegaParaFormularioNovoAgente: () -> Unit,
    onNavegaParaFormularioPesquisaAgente: () -> Unit,
) = listOf(
    MenuBotoesCategoria(
        tituloCategoria = R.string.label_menu_viagens,
        iconCategoria = R.drawable.ic_navio_75,
        dadosBotoesMenus = listOf(
            DadosBotoesMenus(
                title = R.string.btn_nova_viagem,
                icon = R.drawable.ic_add_75,
                onClick = onNavegaParaFormularioNovaViagem
            ),
            DadosBotoesMenus(
                title = R.string.btn_pesquisar_viagens,
                icon = R.drawable.ic_lupa_75,
                onClick = onNavegaParaFormularioPesquisaViagem
            )
        )
    ),
//    MenuBotoesCategoria(
//        tituloCategoria = R.string.label_menu_agentes,
//        iconCategoria = R.drawable.ic_user_75,
//        dadosBotoesMenus = listOf(
//            DadosBotoesMenus(
//                title = R.string.btn_novo_agente,
//                icon = R.drawable.ic_add_75,
//                onClick = onNavegaParaFormularioNovoAgente
//            ),
//            DadosBotoesMenus(
//                title = R.string.btn_pesquisar_agente,
//                icon = R.drawable.ic_lupa_75,
//                onClick = onNavegaParaFormularioPesquisaAgente
//            )
//        )
//    )
)

private fun getListaBotoesMenuPassagens(
    onNavegaParaFormularioPesquisaPassagem: () -> Unit,
    onNavegaParaRelatorios: () -> Unit,
) = listOf(
    DadosBotoesMenus(
        title = R.string.btn_pesquisar_passagens,
        icon = R.drawable.ic_lupa_75,
        onClick = onNavegaParaFormularioPesquisaPassagem
    ),
    DadosBotoesMenus(
        title = R.string.btn_balanco_vendas,
        icon = R.drawable.ic_relatorio_75,
        onClick = onNavegaParaRelatorios
    )
)
