package dev.matheus.fluviapp.ui.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matheus.fluviapp.model.mappers.ViagemDadosViagemMapper
import dev.matheus.fluviapp.model.operacoes.PermissoesUsuario
import dev.matheus.fluviapp.preferences.PreferencesKey
import dev.matheus.fluviapp.services.repository.cadastro.passagem.AgenteRepository
import com.google.firebase.auth.FirebaseAuth
import dev.matheus.fluviapp.services.repository.firebase.PassagemFirestoreRepository
import dev.matheus.fluviapp.services.repository.firebase.ViagemFirestoreRepository
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
    private val agenteRepository: AgenteRepository,
    private val firebaseAuth: FirebaseAuth,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainScreenUiState())
    val uiState: StateFlow<MainScreenUiState>
        get() = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(mainScreenState = MainScreenState.LOADING) }
        obterUsuario()
        observarViagens()
        sincronizarFirestore()
    }

    private fun obterUsuario() {
        viewModelScope.launch {
            dataStore.data.collect { prefs ->
                val username = prefs[PreferencesKey.USUARIO_ATUAL]
                val cargo = prefs[PreferencesKey.CARGO_ATUAL]
                _uiState.update { state ->
                    state.copy(
                        userName = username ?: state.userName,
                        secoesVisiveis = PermissoesUsuario.secoesVisiveis(cargo),
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
        // encerra a sessão do Firebase (autoridade) + limpa o cache de perfil no DataStore.
        firebaseAuth.signOut()
        dataStore.edit {
            it[PreferencesKey.LOGADO] = false
            it[PreferencesKey.USUARIO_ATUAL] = ""
            it[PreferencesKey.CARGO_ATUAL] = ""
        }
    }

    fun refresh() {
        // A lista já é reativa; o refresh força um re-sync. isRefreshing volta a false quando o Flow
        // reativo (observarViagens) emitir os dados atualizados. (D5 futuro: get(Source.SERVER).)
        _uiState.update { it.copy(isRefreshing = true) }
        sincronizarFirestore()
    }

    private fun sincronizarFirestore() {
        viagemRepository.sincronizar()
        passagemRepository.sincronizarNumeroBilheteEmTempoReal()
        agenteRepository.sincronizar()
    }
}
