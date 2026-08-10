package dev.matheus.fluviapp.ui.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matheus.fluviapp.domain.operacoes.Funcionario
import dev.matheus.fluviapp.domain.screendata.secoesDoMenu
import dev.matheus.fluviapp.preferences.PreferencesKey
import com.google.firebase.auth.FirebaseAuth
import dev.matheus.fluviapp.services.repository.firebase.SincronizacaoSessao
import dev.matheus.fluviapp.telemetry.EstadoSincronizacao
// REVITALIZAÇÃO: voltam com as seções Passagem / Equipe.
// import dev.matheus.fluviapp.services.repository.operacoes.FuncionarioRepository
// import dev.matheus.fluviapp.services.repository.firebase.PassagemFirestoreRepository
import dev.matheus.fluviapp.ui.states.MainScreenState
import dev.matheus.fluviapp.ui.states.MainScreenUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * **Revitalização (ADR-0020):** só a Empresa está viva ponta a ponta, e este ViewModel é onde o painel
 * para de fingir o contrário. Saíram daqui as dependências dos domínios ainda não refeitos — viagem,
 * passagem e funcionário —, e com elas a lista de próximas viagens, o pull-to-refresh e a sincronização
 * de três coleções que a home nem exibe mais. O que resta é o mínimo do painel: **quem está logado, o que
 * ele vê no menu, e sair**.
 *
 * As linhas comentadas ficam no lugar de propósito, com o repositório real ao lado: cada uma volta quando
 * a seção correspondente entrar em [dev.matheus.fluviapp.domain.screendata.SECOES_REVITALIZADAS], e assim
 * a volta é uma leitura, não uma arqueologia no histórico.
 *
 * **A da viagem não volta assim** (F8.0): o `viagemRepository` e o `viagemMapper` que estavam aqui eram da
 * Viagem-trecho, e ela foi demolida. Restaurar as linhas comentadas apontaria para tipos que não existem —
 * então elas saíram, e não ficaram fingindo que esperam. O que a home mostra é a **F10**, e ela nasce do
 * contexto escolhido (papel, empresa, cargo), não de uma lista de próximas viagens.
 */
@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    // REVITALIZAÇÃO: voltam com as seções Passagem / Equipe.
    // private val passagemRepository: PassagemFirestoreRepository,
    // private val funcionarioRepository: FuncionarioRepository,
    private val firebaseAuth: FirebaseAuth,
    private val sincronizacaoSessao: SincronizacaoSessao,
    private val estadoSincronizacao: EstadoSincronizacao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainScreenUiState())
    val uiState: StateFlow<MainScreenUiState>
        get() = _uiState.asStateFlow()

    init {
        // Sem viagens a observar, o painel não tem o que carregar: nasce em HOME. Antes quem tirava o
        // estado de LOADING era a primeira emissão de `observarViagens` — desligá-la sem isto deixaria o
        // spinner girando para sempre.
        obterUsuario()
        observarSincronizacao()
    }

    // D4: reflete a saúde do sync (EstadoSincronizacao) num flag de UI — o banner offline-first é
    // não-bloqueante (os cards do cache continuam). Limpa quando um snapshot do servidor chega.
    private fun observarSincronizacao() {
        viewModelScope.launch {
            estadoSincronizacao.comErro.collect { comErro ->
                _uiState.update { it.copy(sincronizacaoComErro = comErro) }
            }
        }
    }

    private fun obterUsuario() {
        viewModelScope.launch {
            dataStore.data.collect { prefs ->
                val username = prefs[PreferencesKey.USUARIO_ATUAL]
                // O menu é quase todo eixo de SISTEMA, mas a seção Equipe também olha o cargo: ela
                // existe para o supervisor gerir a própria agência (ADR-0015 §2.2/§8.2).
                val papel = prefs[PreferencesKey.PAPEL_ATUAL]
                val cargo = prefs[PreferencesKey.CARGO_ATUAL]
                // A **família** do menu deriva da atuação (ADR-0016 §2, ADR-0020 F3/F4), e a atuação vem
                // do cargo, que já a declara. Papel puro de plataforma não tem cargo — e aí `null` é a
                // informação, não a falta dela: quem administra a plataforma não atua num segmento.
                val atuacao = Funcionario.Cargo.de(cargo)?.atuacao
                _uiState.update { state ->
                    state.copy(
                        userName = username ?: state.userName,
                        // `secoesDoMenu` = a política (quem pode) ∩ o escopo revitalizado (o que existe).
                        // A política em si continua intacta — o recorte é do andaime, não da autorização.
                        secoesVisiveis = secoesDoMenu(papel, cargo, atuacao),
                    )
                }
            }
        }
    }

    // REVITALIZAÇÃO (F8.0): `observarViagens()` saiu de vez, e não fica comentado esperando.
    //
    // Ele coletava o espelho Room da Viagem-trecho e montava cards de "próximas viagens". As três peças
    // que ele usava morreram juntas: a entidade, o espelho (o Room deixou de ter a tabela) e o card. O que
    // a **F10** puser aqui parte de outra pergunta — o que este usuário faz agora, dado papel, empresa e
    // cargo —, e não de uma lista de cadastros.

    fun irParaHome() {
        _uiState.update { it.copy(mainScreenState = MainScreenState.HOME) }
    }

    suspend fun deslogar() {
        // para os listeners de sync da sessão (D2 — awaitClose remove as registrations), encerra a
        // sessão do Firebase (autoridade) + limpa o cache de perfil no DataStore.
        sincronizacaoSessao.parar()
        firebaseAuth.signOut()
        dataStore.edit {
            it[PreferencesKey.LOGADO] = false
            it[PreferencesKey.USUARIO_ATUAL] = ""
            it[PreferencesKey.CARGO_ATUAL] = ""
            // A escolha de contexto (F6.4) é de quem estava operando, não do aparelho: quem entrar
            // depois responde de novo. Deixá-la para trás faria a próxima pessoa herdar a empresa da
            // anterior — e o app não perguntaria, porque já teria uma resposta guardada.
            it.remove(PreferencesKey.EMPRESA_ATIVA)
        }
    }

    // REVITALIZAÇÃO: sem lista na home, não há o que puxar para atualizar — o pull-to-refresh saiu da tela
    // junto (ADR-0020), e o `refresh()` que o servia saiu com ele na F8.0. Ele forçava `Source.SERVER` no
    // repositório da Viagem-trecho para reencher o Room; nenhuma das duas coisas existe mais. Se o Início
    // da F10 quiser atualizar à mão, será sobre o `StateFlow` do listener (ADR-0017), não sobre o Room.

    // REVITALIZAÇÃO: sincronizar coleção que nenhuma tela viva consome é pagar listener por nada. A
    // Empresa tem a própria sincronização, disparada por quem a usa (EmpresaFirestoreRepository).
    //
    // private fun sincronizarFirestore() {
    //     passagemRepository.sincronizarNumeroBilheteEmTempoReal()
    //     funcionarioRepository.sincronizar()
    // }
}
