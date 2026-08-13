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
    onNavegaParaEmbarque: () -> Unit,
    onNavegaParaFormularioNovoFuncionario: () -> Unit,
    onNavegaParaFormularioPesquisaFuncionario: () -> Unit,
    onNavegaParaFormularioNovaEmpresa: () -> Unit,
    onNavegaParaFormularioPesquisaEmpresa: () -> Unit,
    onNavegaParaFormularioNovaEmbarcacao: () -> Unit,
    onNavegaParaFormularioPesquisaEmbarcacao: () -> Unit,
    onNavegaParaFormularioNovaLocalidade: () -> Unit,
    onNavegaParaFormularioPesquisaLocalidade: () -> Unit,
    onNavegaParaFormularioNovoPorto: () -> Unit,
    onNavegaParaFormularioPesquisaPorto: () -> Unit,
    onNavegaParaFormularioNovoUsuario: () -> Unit,
    onNavegaParaFormularioPesquisaUsuario: () -> Unit,
    onNavegaParaFormularioNovaRota: () -> Unit,
    onNavegaParaFormularioPesquisaRota: () -> Unit,
    onNavegaParaFormularioNovaViagem: () -> Unit,
    onNavegaParaFormularioPesquisaViagem: () -> Unit,
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
            // REVITALIZAÇÃO (F9.2): as telas de pesquisa e contagem de passagem **não existem** — saíram com a
            // camada de dados que as alimentava. As ações continuam no [AcaoMenu] porque elas são domínio do
            // menu (o que cada seção oferece), e `PASSAGEM` não está em `SECOES_REVITALIZADAS`: nenhuma delas é
            // alcançável. A pesquisa volta na F9.5; a contagem, quando a **ocupação** tiver domínio planejado
            // (ADR-0027 D2 a deixa fora da F9 por não ter).
            AcaoMenu.PASSAGEM_PESQUISAR -> Unit
            AcaoMenu.PASSAGEM_CONTAGEM -> Unit
            AcaoMenu.EQUIPE_NOVO -> onNavegaParaFormularioNovoFuncionario()
            AcaoMenu.EQUIPE_PESQUISAR -> onNavegaParaFormularioPesquisaFuncionario()
            AcaoMenu.EMPRESA_NOVA -> onNavegaParaFormularioNovaEmpresa()
            AcaoMenu.EMPRESA_PESQUISAR -> onNavegaParaFormularioPesquisaEmpresa()
            AcaoMenu.EMBARCACAO_NOVA -> onNavegaParaFormularioNovaEmbarcacao()
            AcaoMenu.EMBARCACAO_PESQUISAR -> onNavegaParaFormularioPesquisaEmbarcacao()
            AcaoMenu.LOCALIDADE_NOVA -> onNavegaParaFormularioNovaLocalidade()
            AcaoMenu.LOCALIDADE_PESQUISAR -> onNavegaParaFormularioPesquisaLocalidade()
            AcaoMenu.PORTO_NOVO -> onNavegaParaFormularioNovoPorto()
            AcaoMenu.PORTO_PESQUISAR -> onNavegaParaFormularioPesquisaPorto()
            AcaoMenu.USUARIO_NOVO -> onNavegaParaFormularioNovoUsuario()
            AcaoMenu.USUARIO_PESQUISAR -> onNavegaParaFormularioPesquisaUsuario()
            AcaoMenu.ROTA_NOVA -> onNavegaParaFormularioNovaRota()
            AcaoMenu.ROTA_PESQUISAR -> onNavegaParaFormularioPesquisaRota()
            AcaoMenu.VIAGEM_NOVA -> onNavegaParaFormularioNovaViagem()
            AcaoMenu.VIAGEM_PESQUISAR -> onNavegaParaFormularioPesquisaViagem()
        }

        MainScreen(
            state = state,
            acoesPorSecao = acoesPorSecao(state.secoesVisiveis),
            onAcaoMenu = ::navegar,
            onClickInicio = { viewModel.irParaHome() },
            onClickDeslogar = {
                coroutineScope.launch {
                    viewModel.deslogar()
                    onNavegaParaLogin()
                }
            },
            isDarkTheme = escuro,
            onToggleTheme = { themeViewModel.alternarTema(escuro) },
            // REVITALIZAÇÃO: embarque, nova passagem e pull-to-refresh saíram do painel com os domínios
            // que os alimentam (ADR-0020). Os destinos continuam no grafo, sem entrada pela Main Screen.
            // onClickEmbarque = onNavegaParaEmbarque,
            // onRefresh = { viewModel.refresh() },
        )
    }
}
