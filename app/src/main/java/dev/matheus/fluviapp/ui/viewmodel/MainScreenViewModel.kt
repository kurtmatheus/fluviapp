package dev.matheus.fluviapp.ui.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matheus.fluviapp.model.mappers.ViagemDadosViagemMapper
import dev.matheus.fluviapp.model.operacoes.PermissoesUsuario
import dev.matheus.fluviapp.preferences.PreferencesKey
import dev.matheus.fluviapp.services.repository.operacoes.FuncionarioRepository
import com.google.firebase.auth.FirebaseAuth
import dev.matheus.fluviapp.services.repository.firebase.PassagemFirestoreRepository
import dev.matheus.fluviapp.services.repository.firebase.SincronizacaoSessao
import dev.matheus.fluviapp.services.repository.firebase.ViagemFirestoreRepository
import dev.matheus.fluviapp.telemetry.EstadoSincronizacao
import dev.matheus.fluviapp.ui.states.MainScreenState
import dev.matheus.fluviapp.ui.states.MainScreenUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val viagemRepository: ViagemFirestoreRepository,
    private val viagemMapper: ViagemDadosViagemMapper,
    private val passagemRepository: PassagemFirestoreRepository,
    private val funcionarioRepository: FuncionarioRepository,
    private val firebaseAuth: FirebaseAuth,
    private val sincronizacaoSessao: SincronizacaoSessao,
    private val estadoSincronizacao: EstadoSincronizacao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainScreenUiState())
    val uiState: StateFlow<MainScreenUiState>
        get() = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(mainScreenState = MainScreenState.LOADING) }
        obterUsuario()
        observarViagens()
        observarSincronizacao()
        sincronizarFirestore()
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
                _uiState.update { state ->
                    state.copy(
                        userName = username ?: state.userName,
                        secoesVisiveis = PermissoesUsuario.secoesVisiveis(papel, cargo),
                    )
                }
            }
        }
    }

    // Coleta reativa do espelho Room (SSOT — estudo sincronizacao-firestore-room.md, D1). A UI
    // atualiza sozinha quando o listener grava dados novos; sem delay(1000) nem leitura one-shot. O
    // mapper é suspend (ADR-0008) e encaixa no Flow.map. Encerra o refresh quando a emissão chega.
    private fun observarViagens() {
        viewModelScope.launch {
            viagemRepository.observarTodas()
                .map { viagens -> viagens.map { viagemMapper.map(it) } }
                .collect { cards ->
                    _uiState.update {
                        it.copy(
                            listaViagens = cards,
                            mainScreenState = MainScreenState.HOME,
                            isRefreshing = false,
                        )
                    }
                }
        }
    }

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
        }
    }

    fun refresh() {
        // D5: pull-to-refresh força a busca no servidor (get(Source.SERVER)); grava no Room e o Flow
        // reativo (observarViagens) reflete. O spinner fica até a busca concluir. Erro (offline) é
        // reportado pelo repo → EstadoSincronizacao → banner (D4).
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                viagemRepository.atualizarDoServidor()
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    private fun sincronizarFirestore() {
        viagemRepository.sincronizar()
        passagemRepository.sincronizarNumeroBilheteEmTempoReal()
        funcionarioRepository.sincronizar()
    }
}
