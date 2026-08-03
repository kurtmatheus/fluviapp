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
import dev.matheus.fluviapp.domain.screendata.DadosBotoesMenus
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

        // Ações (cadastrar/pesquisar) de cada seção do menu — liga os cards às rotas.
        fun acoesDe(secao: SecaoMenu): List<DadosBotoesMenus> = when (secao) {
            SecaoMenu.PASSAGEM -> listOf(
                DadosBotoesMenus(R.string.btn_pesquisar_passagens, R.drawable.ic_lupa_75, onNavegaParaFormularioPesquisaPassagem),
                DadosBotoesMenus(R.string.btn_contagem_passagem, R.drawable.ic_relatorio_75, onNavegaParaContagemPassagem),
            )

            SecaoMenu.VIAGEM -> listOf(
                DadosBotoesMenus(R.string.btn_nova_viagem, R.drawable.ic_add_75, onNavegaParaFormularioNovaViagem),
                DadosBotoesMenus(R.string.btn_pesquisar_viagens, R.drawable.ic_lupa_75, onNavegaParaFormularioPesquisaViagem),
            )

            SecaoMenu.EQUIPE -> listOf(
                DadosBotoesMenus(R.string.btn_novo_agente, R.drawable.ic_add_75, onNavegaParaFormularioNovoFuncionario),
                DadosBotoesMenus(R.string.btn_pesquisar_agente, R.drawable.ic_lupa_75, onNavegaParaFormularioPesquisaFuncionario),
            )

            SecaoMenu.EMPRESA -> listOf(
                DadosBotoesMenus(R.string.btn_nova_empresa, R.drawable.ic_add_75, onNavegaParaFormularioNovaEmpresa),
                DadosBotoesMenus(R.string.btn_pesquisar_empresa, R.drawable.ic_lupa_75, onNavegaParaFormularioPesquisaEmpresa),
            )

            SecaoMenu.NAVIO -> listOf(
                DadosBotoesMenus(R.string.btn_novo_navio, R.drawable.ic_add_75, onNavegaParaFormularioNovoNavio),
                DadosBotoesMenus(R.string.btn_pesquisar_navio, R.drawable.ic_lupa_75, onNavegaParaFormularioPesquisaNavio),
            )
        }

        MainScreen(
            state = state,
            acoesPorSecao = state.secoesVisiveis.associateWith(::acoesDe),
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
