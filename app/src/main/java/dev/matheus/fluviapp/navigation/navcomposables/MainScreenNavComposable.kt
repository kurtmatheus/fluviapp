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
import dev.matheus.fluviapp.domain.screendata.AcaoMenu
import dev.matheus.fluviapp.domain.screendata.acoesPorSecao
import dev.matheus.fluviapp.domain.screendata.SecaoMenu
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
    onNavegaParaEmbarque: () -> Unit,
    onNavegaParaContagemPassagem: () -> Unit,
    onNavegaParaFormularioNovoFuncionario: () -> Unit,
    onNavegaParaFormularioPesquisaFuncionario: () -> Unit,
    onNavegaParaFormularioNovaEmpresa: () -> Unit,
    onNavegaParaFormularioPesquisaEmpresa: () -> Unit,
    onNavegaParaFormularioNovoNavio: () -> Unit,
    onNavegaParaFormularioPesquisaNavio: () -> Unit
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

        // O que sobrou aqui é NAVEGAÇÃO: para onde cada ação vai. Quais ações cada seção oferece é
        // domínio, e mora em `AcaoMenu` (ADR-0020 F3) — antes as duas coisas viviam juntas no
        // `acoesDe(secao)`, o que tornava a estrutura do menu impossível de testar sem um NavGraphBuilder.
        // O `when` é exaustivo sobre o enum: ação nova sem destino não compila.
        fun navegar(acao: AcaoMenu) = when (acao) {
            AcaoMenu.PASSAGEM_PESQUISAR -> onNavegaParaFormularioPesquisaPassagem()
            AcaoMenu.PASSAGEM_CONTAGEM -> onNavegaParaContagemPassagem()
            AcaoMenu.VIAGEM_NOVA -> onNavegaParaFormularioNovaViagem()
            AcaoMenu.VIAGEM_PESQUISAR -> onNavegaParaFormularioPesquisaViagem()
            AcaoMenu.EQUIPE_NOVO -> onNavegaParaFormularioNovoFuncionario()
            AcaoMenu.EQUIPE_PESQUISAR -> onNavegaParaFormularioPesquisaFuncionario()
            AcaoMenu.EMPRESA_NOVA -> onNavegaParaFormularioNovaEmpresa()
            AcaoMenu.EMPRESA_PESQUISAR -> onNavegaParaFormularioPesquisaEmpresa()
            AcaoMenu.NAVIO_NOVO -> onNavegaParaFormularioNovoNavio()
            AcaoMenu.NAVIO_PESQUISAR -> onNavegaParaFormularioPesquisaNavio()
        }

        MainScreen(
            state = state,
            acoesPorSecao = acoesPorSecao(state.secoesVisiveis),
            onAcaoMenu = ::navegar,
            onClickInicio = { viewModel.irParaHome() },
            onClickEmbarque = onNavegaParaEmbarque,
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
