package dev.matheus.fluviapp.navigation.navcomposables.empresa

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.extensions.toastMessage
import dev.matheus.fluviapp.navigation.destinations.FluviAppNavComposableDestinations
import dev.matheus.fluviapp.ui.screens.forms.empresa.FormEmpresaScreen
import dev.matheus.fluviapp.ui.viewmodel.empresa.FormEmpresaViewModel

internal const val ID_EMPRESA_ARGUMENT = "idEmpresa"

fun NavGraphBuilder.formEmpresaNavComposable(
    onNavegaParaMainScreen: () -> Unit,
    onClickVoltar: () -> Unit,
) {
    composable(
        route = "${FluviAppNavComposableDestinations.FormEmpresaNavComposable.route}?$ID_EMPRESA_ARGUMENT={$ID_EMPRESA_ARGUMENT}",
        arguments = listOf(
            navArgument(ID_EMPRESA_ARGUMENT) {
                type = NavType.StringType
                defaultValue = ""
            }
        ),
    ) {
        val viewModel = hiltViewModel<FormEmpresaViewModel>()
        val state by viewModel.uiState.collectAsState()

        val context = LocalContext.current

        // Sucesso é um evento one-shot (não estado): navega uma vez, sem navegar-no-finally.
        //
        // O aviso vem ANTES de navegar, e é por isso que ele é um Toast e não um Snackbar: a tela que
        // poderia hospedar a barra é justamente a que está saindo. Toast vive fora da hierarquia da
        // janela, então sobrevive à navegação — feio, e certo aqui.
        LaunchedEffect(Unit) {
            viewModel.sucesso.collect {
                context.toastMessage(context.getString(R.string.msg_empresa_salva))
                onNavegaParaMainScreen()
            }
        }

        FormEmpresaScreen(
            uiState = state,
            onNomeChange = viewModel::onNomeChange,
            onRazaoSocialChange = viewModel::onRazaoSocialChange,
            onCnpjChange = viewModel::onCnpjChange,
            onEnderecoChange = viewModel::onEnderecoChange,
            onTelefone1Change = viewModel::onTelefone1Change,
            onTelefone2Change = viewModel::onTelefone2Change,
            onAtuacaoToggle = viewModel::onAtuacaoToggle,
            onEmbarcacaoToggle = viewModel::onEmbarcacaoToggle,
            onClickSalvar = viewModel::salvar,
            onClickVoltar = onClickVoltar,
        )
    }
}
