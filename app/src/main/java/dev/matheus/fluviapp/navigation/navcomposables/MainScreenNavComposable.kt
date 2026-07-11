package dev.matheus.fluviapp.navigation.navcomposables

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.model.screendata.DadosBotoesMenus
import dev.matheus.fluviapp.model.screendata.SecaoMenu
import dev.matheus.fluviapp.navigation.destinations.FluviAppNavComposableDestinations
import dev.matheus.fluviapp.ui.components.RequestMultiplePermissions
import dev.matheus.fluviapp.ui.screens.MainScreen
import dev.matheus.fluviapp.ui.viewmodel.MainScreenViewModel
import dev.matheus.fluviapp.ui.viewmodel.ThemeViewModel
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

        // Mesma instância que a MainActivity usa p/ dirigir o tema (escopo Activity).
        val themeViewModel = hiltViewModel<ThemeViewModel>(context as ComponentActivity)
        val temaEscuro by themeViewModel.temaEscuro.collectAsState()
        val escuro = temaEscuro ?: isSystemInDarkTheme()

        RequestMultiplePermissions(
            context = context,
            onGrantedPermission = {},
            onDeniedPermission = {},
            permissionsList = listOf(
                android.Manifest.permission.BLUETOOTH,
                android.Manifest.permission.BLUETOOTH_CONNECT
            )
        )

        // Ações (cadastrar/pesquisar) de cada seção do menu — liga os cards às rotas.
        fun acoesDe(secao: SecaoMenu): List<DadosBotoesMenus> = when (secao) {
            SecaoMenu.PASSAGEM -> listOf(
                DadosBotoesMenus(R.string.btn_pesquisar_passagens, R.drawable.ic_lupa_75, onNavegaParaFormularioPesquisaPassagem),
                DadosBotoesMenus(R.string.btn_balanco_vendas, R.drawable.ic_relatorio_75, onNavegaParaBalanco),
            )

            SecaoMenu.VIAGEM -> listOf(
                DadosBotoesMenus(R.string.btn_nova_viagem, R.drawable.ic_add_75, onNavegaParaFormularioNovaViagem),
                DadosBotoesMenus(R.string.btn_pesquisar_viagens, R.drawable.ic_lupa_75, onNavegaParaFormularioPesquisaViagem),
            )

            SecaoMenu.AGENTE -> listOf(
                DadosBotoesMenus(R.string.btn_novo_agente, R.drawable.ic_add_75, onNavegaParaFormularioNovoAgente),
                DadosBotoesMenus(R.string.btn_pesquisar_agente, R.drawable.ic_lupa_75, onNavegaParaFormularioPesquisaAgente),
            )
        }

        MainScreen(
            state = state,
            onClickInicio = { viewModel.irParaHome() },
            onSelecionarSecao = { secao -> viewModel.selecionarSecao(secao, acoesDe(secao)) },
            onClickDeslogar = {
                coroutineScope.launch {
                    viewModel.deslogar()
                    onNavegaParaLogin()
                }
            },
            onClickAdicionarPassagem = onNavegaParaFormularioNovaPassagemComViagem,
            onRefresh = { viewModel.refresh() },
            isDarkTheme = escuro,
            onToggleTheme = { themeViewModel.alternarTema(escuro) },
        )
    }
}
