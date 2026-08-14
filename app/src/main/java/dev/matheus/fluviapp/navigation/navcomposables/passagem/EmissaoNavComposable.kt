package dev.matheus.fluviapp.navigation.navcomposables.passagem

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.matheus.fluviapp.navigation.destinations.ARG_OCORRENCIA
import dev.matheus.fluviapp.navigation.destinations.FluviAppNavComposableDestinations
import dev.matheus.fluviapp.ui.screens.passagem.emissao.EmissaoScreen
import dev.matheus.fluviapp.ui.viewmodel.passagem.EmissaoViewModel
import dev.matheus.fluviapp.ui.viewmodel.passagem.EventoDeEmissao
import dev.matheus.fluviapp.ui.viewmodel.passagem.MotivoDeFalha

/**
 * A emissão na navegação — **um destino só**, e é isso que a mantém honesta ([ADR-0029] D3).
 *
 * Os passos não são destinos porque o roteiro é derivado do estado: quantos existem depende do que se
 * escolheu. Se cada passo fosse uma rota, a navegação teria de conhecer essa regra — e voltaria a orquestrar
 * a emissão, que é exatamente o que o [ADR-0026] D3 tirou dela. Aqui ela sabe **entrar e sair**; o resto é do
 * ViewModel.
 *
 * A **ocorrência viaja como argumento obrigatório** (`viagemId@yyyy-MM-dd`). Obrigatório, e não opcional com
 * sentinela: o argumento opcional do formulário antigo trafegava o texto **"null"** e chegava a ser gravado —
 * um bilhete sem saída não é um bilhete a preencher, é um bilhete que não existe.
 */
fun NavGraphBuilder.emissaoNavComposable(
    onClickVoltar: () -> Unit,
    onNavegaParaBilhete: (String) -> Unit,
) {
    composable(
        route = FluviAppNavComposableDestinations.EmissaoNavComposable.route,
        arguments = listOf(navArgument(ARG_OCORRENCIA) { type = NavType.StringType }),
    ) { entrada ->
        val viewModel = hiltViewModel<EmissaoViewModel>()
        val state by viewModel.uiState.collectAsState()
        val chave = entrada.arguments?.getString(ARG_OCORRENCIA)

        // A saída chega uma vez, na entrada da tela: recarregar a cada recomposição apagaria o atendimento.
        LaunchedEffect(chave) { viewModel.iniciarPelaChave(chave) }

        // A navegação **reage** ao desfecho; ela não o produz (ADR-0026 D3).
        //
        // Emitir leva **direto ao bilhete**: a tela que anunciava "a passagem foi emitida" saiu, porque ela
        // só **dizia** que deu certo — e o bilhete **mostra**, além de se salvar ao aparecer. Bloqueio e
        // falha continuam no estado, que é onde o operador precisa deles: na tela em que estava.
        LaunchedEffect(Unit) {
            viewModel.eventos.collect { evento ->
                when {
                    evento is EventoDeEmissao.Emitida -> onNavegaParaBilhete(evento.idPassagem)

                    evento is EventoDeEmissao.Falhou && evento.motivo == MotivoDeFalha.SEM_OCORRENCIA ->
                        onClickVoltar()
                }
            }
        }

        EmissaoScreen(
            state = state,
            onEscolherCategoria = { viewModel.escolherCategoria(it); viewModel.avancar() },
            onEscolherAcomodacao = { viewModel.escolherAcomodacao(it); viewModel.avancar() },
            onEscolherTipo = { viewModel.escolherTipo(it); viewModel.avancar() },
            onEscolherGratuidade = { viewModel.escolherGratuidade(it); viewModel.avancar() },
            onEscolherQuantidade = { viewModel.escolherQuantidadeDePessoas(it); viewModel.avancar() },
            onEscolherClasse = { viewModel.escolherClasseDeVeiculo(it); viewModel.avancar() },
            onPreencherPessoa = viewModel::preencherPessoa,
            onPreencherVeiculo = viewModel::preencherVeiculo,
            onPreencherResponsavel = viewModel::preencherResponsavel,
            onPreencherPagamento = viewModel::preencherPagamento,
            onAvancar = viewModel::avancar,
            onVoltar = viewModel::voltar,
            onPular = viewModel::pular,
            onConfirmarEmissao = viewModel::confirmarEmissao,
            onRevisar = viewModel::revisar,
            onClickVoltarTela = onClickVoltar,
        )
    }
}