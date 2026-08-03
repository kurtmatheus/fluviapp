package dev.matheus.fluviapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.matheus.fluviapp.domain.operacoes.ContextoUsuario
import dev.matheus.fluviapp.services.repository.operacoes.SessaoUsuario
import dev.matheus.fluviapp.ui.states.SplashScreenState
import dev.matheus.fluviapp.ui.states.SplashScreenUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashScreenViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val sessaoUsuario: SessaoUsuario,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashScreenUiState())
    val uiState: StateFlow<SplashScreenUiState>
        get() = _uiState.asStateFlow()

    init {
        resolverDestino()
    }

    /**
     * Resolve o destino inicial (ADR-0020 D9). São **duas** perguntas, e antes só a primeira era feita:
     *
     * 1. **há sessão?** — autoridade é a sessão persistida do Firebase (offline-capaz, ADR-0005);
     * 2. **quem é esta pessoa na operação?** — usuário → funcionário → atuação, pela porta
     *    [SessaoUsuario], que já existia e a splash não usava.
     *
     * A segunda é o que a decisão acrescentou: o painel **deriva da atuação**, então entrar antes de
     * sabê-la é montar o menu errado e corrigi-lo depois — que é a informação "esquecida" que se quer
     * evitar. Isso torna a espera **real**, e é a primeira que o app tem; a E1.1 tinha removido a espera
     * **artificial** (`delay(Random)`), e as duas coisas não se contradizem.
     *
     * Contexto ausente com sessão válida (registro local sumiu) volta para o login em vez de entrar: é lá
     * que o vínculo se refaz. Falha de leitura vira [SplashScreenState.Erro], com repetição — nunca se
     * entra pela metade, nem se fica preso em `Carregando`.
     */
    private fun resolverDestino() {
        if (firebaseAuth.currentUser == null) {
            atualizar(destinoDaSplash(temSessao = false))
            return
        }

        atualizar(SplashScreenState.Carregando)
        viewModelScope.launch {
            val estado = runCatching { sessaoUsuario.atual() }
                .fold(
                    onSuccess = { contexto -> destinoDaSplash(temSessao = true, contexto = contexto) },
                    onFailure = { destinoDaSplash(temSessao = true, falhouAoCarregar = true) },
                )
            atualizar(estado)
        }
    }

    /** Repetição depois de [SplashScreenState.Erro] — a saída que impede a tela de virar beco sem saída. */
    fun tentarNovamente() = resolverDestino()

    private fun atualizar(estado: SplashScreenState) =
        _uiState.update { it.copy(splashScreenState = estado) }
}
/**
 * A decisão da splash, **pura** — mesmo molde das validações do ADR-0006: a regra sai do ViewModel para
 * poder ser testada sem `FirebaseAuth` (classe final, e o projeto não usa biblioteca de mock).
 *
 * Três entradas, quatro saídas:
 *
 * | sessão | contexto | falhou | destino |
 * |---|---|---|---|
 * | não | — | — | `Deslogado` |
 * | sim | — | sim | `Erro` |
 * | sim | ausente | não | `Deslogado` — sessão válida sem registro local: o vínculo se refaz no login |
 * | sim | presente | não | `Logado` |
 *
 * A terceira linha é a que muda o comportamento anterior: antes bastava haver sessão no Firebase para
 * entrar, e o painel montava sem saber quem era a pessoa na operação.
 */
internal fun destinoDaSplash(
    temSessao: Boolean,
    contexto: ContextoUsuario? = null,
    falhouAoCarregar: Boolean = false,
): SplashScreenState = when {
    !temSessao -> SplashScreenState.Deslogado
    falhouAoCarregar -> SplashScreenState.Erro
    contexto == null -> SplashScreenState.Deslogado
    else -> SplashScreenState.Logado
}
