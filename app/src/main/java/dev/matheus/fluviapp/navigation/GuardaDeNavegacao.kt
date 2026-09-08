package dev.matheus.fluviapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.matheus.fluviapp.domain.operacoes.PermissoesUsuario
import dev.matheus.fluviapp.domain.screendata.SecaoMenu
import dev.matheus.fluviapp.navigation.destinations.secaoDaRota
import dev.matheus.fluviapp.services.repository.operacoes.SessaoUsuario
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * **A guarda de navegação** ([ADR-0032] D1) — o destino reautoriza, e não só o menu.
 *
 * ### O que ela conserta
 *
 * Até 2026-09-07 nenhum `NavGraphBuilder` consultava permissão: a única barreira era **a ausência do
 * botão no menu**. Um destino alcançado por outro caminho — a pilha de retorno, um `navigate` escrito
 * amanhã — abria sem ninguém perguntar nada. E esconder um botão é dizer *"não te ofereço"*, que não é a
 * mesma frase que *"não podes"*.
 *
 * ### Por que aqui, e não em cada tela
 *
 * Porque guarda por tela é guarda que a próxima tela esquece. Esta assina a pilha de navegação inteira e
 * vale para **todo** destino que declare seção — e declarar é obrigatório por construção: `secao` é
 * parâmetro sem valor padrão em `FluviAppNavComposableDestinations`, então uma tela nova **não compila**
 * sem responder de quem ela é.
 *
 * ### O que ela não é
 *
 * Não é a fronteira de autorização — essa é do servidor (ADR-0011), e continua sendo. Esta é o **segundo
 * freio**, e entrega o que a regra do servidor não faz: impedir que a pessoa chegue a uma tela cujas
 * escritas voltariam negadas, sem entender por quê.
 *
 * ### A escolha do gesto: voltar, não avisar
 *
 * Destino não autorizado sai da pilha (`popBackStack`) em vez de mostrar um aviso, e é o que combina com
 * o resto: o menu não oferece aquilo, então chegar ali é acidente de rota — e o conserto de um acidente é
 * voltar para onde se estava, não explicar o acidente.
 */
@Composable
fun GuardaDeNavegacao(navController: NavHostController) {
    val viewModel: GuardaDeNavegacaoViewModel = hiltViewModel()
    val papel by viewModel.papel.collectAsStateWithLifecycle()

    LaunchedEffect(navController, papel) {
        navController.currentBackStackEntryFlow.collect { entrada ->
            val secao = secaoDaRota(entrada.destination.route) ?: return@collect
            if (!PermissoesUsuario.podeAcessar(secao, papel)) {
                navController.popBackStack()
            }
        }
    }
}

/**
 * O papel de quem está operando, observado.
 *
 * Existe porque a guarda é `@Composable` e não pode injetar repositório — e porque ela precisa **reagir**
 * ao login e ao logout, que é justamente o que a porta passou a oferecer na mesma fatia ([ADR-0032] D2).
 *
 * Sem sessão o papel é `null`, e a política nega tudo: é o fail-closed do resto da casa, e aqui ele tem
 * uma consequência boa de lembrar — durante o login, antes de a sessão existir, **nenhum destino de seção
 * é alcançável**.
 */
@HiltViewModel
class GuardaDeNavegacaoViewModel @Inject constructor(
    sessaoUsuario: SessaoUsuario,
) : ViewModel() {
    val papel: StateFlow<String?> = sessaoUsuario.observar()
        .map { it?.papel }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )
}
