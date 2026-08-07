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
// REVITALIZAÇÃO: voltam com as seções Viagem / Passagem / Equipe.
// import dev.matheus.fluviapp.domain.mappers.ViagemDadosViagemMapper
// import dev.matheus.fluviapp.services.repository.operacoes.FuncionarioRepository
// import dev.matheus.fluviapp.services.repository.firebase.PassagemFirestoreRepository
// import dev.matheus.fluviapp.services.repository.firebase.ViagemFirestoreRepository
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
 */
@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    // REVITALIZAÇÃO: voltam com as seções Viagem / Passagem / Equipe.
    // private val viagemRepository: ViagemFirestoreRepository,
    // private val viagemMapper: ViagemDadosViagemMapper,
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

    // REVITALIZAÇÃO: a home não lista viagens enquanto a seção Viagem não for refeita.
    //
    // Coleta reativa do espelho Room (SSOT — estudo sincronizacao-firestore-room.md, D1). A UI
    // atualiza sozinha quando o listener grava dados novos; sem delay(1000) nem leitura one-shot. O
    // mapper é suspend (ADR-0008) e encaixa no Flow.map. Encerra o refresh quando a emissão chega.
    // private fun observarViagens() {
    //     viewModelScope.launch {
    //         viagemRepository.observarTodas()
    //             .map { viagens -> viagens.map { viagemMapper.map(it) } }
    //             .collect { cards ->
    //                 _uiState.update {
    //                     it.copy(
    //                         listaViagens = cards,
    //                         mainScreenState = MainScreenState.HOME,
    //                         isRefreshing = false,
    //                     )
    //                 }
    //             }
    //     }
    // }

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

    // REVITALIZAÇÃO: sem lista na home, não há o que puxar para atualizar — o pull-to-refresh sai da tela
    // junto (ADR-0020). Volta com a seção Viagem.
    //
    // fun refresh() {
    //     // D5: pull-to-refresh força a busca no servidor (get(Source.SERVER)); grava no Room e o Flow
    //     // reativo (observarViagens) reflete. O spinner fica até a busca concluir. Erro (offline) é
    //     // reportado pelo repo → EstadoSincronizacao → banner (D4).
    //     viewModelScope.launch {
    //         _uiState.update { it.copy(isRefreshing = true) }
    //         try {
    //             viagemRepository.atualizarDoServidor()
    //         } finally {
    //             _uiState.update { it.copy(isRefreshing = false) }
    //         }
    //     }
    // }

    // REVITALIZAÇÃO: sincronizar coleção que nenhuma tela viva consome é pagar listener por nada. A
    // Empresa tem a própria sincronização, disparada por quem a usa (EmpresaFirestoreRepository).
    //
    // private fun sincronizarFirestore() {
    //     viagemRepository.sincronizar()
    //     passagemRepository.sincronizarNumeroBilheteEmTempoReal()
    //     funcionarioRepository.sincronizar()
    // }
}
