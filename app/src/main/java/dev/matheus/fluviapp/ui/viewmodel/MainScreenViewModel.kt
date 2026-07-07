package dev.matheus.fluviapp.ui.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.model.mappers.ViagemDadosViagemMapper
import dev.matheus.fluviapp.model.operacoes.Usuario.Cargo.ADM
import dev.matheus.fluviapp.model.operacoes.Usuario.Cargo.DIRETOR
import dev.matheus.fluviapp.preferences.PreferencesKey
import dev.matheus.fluviapp.services.repository.cadastro.passagem.AgenteRepository
import dev.matheus.fluviapp.services.repository.firebase.PassagemFirestoreRepository
import dev.matheus.fluviapp.services.repository.firebase.ViagemFirestoreRepository
import dev.matheus.fluviapp.ui.states.MainScreenState
import dev.matheus.fluviapp.ui.states.MainScreenState.HOME
import dev.matheus.fluviapp.ui.states.MainScreenState.LOADING
import dev.matheus.fluviapp.ui.states.MainScreenState.OPERACOES
import dev.matheus.fluviapp.ui.states.MainScreenState.PASSAGENS
import dev.matheus.fluviapp.ui.states.MainScreenUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val viagemRepository: ViagemFirestoreRepository,
    private val viagemMapper: ViagemDadosViagemMapper,
    private val passagemRepository: PassagemFirestoreRepository,
    private val agenteRepository: AgenteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainScreenUiState())
    val uiState: StateFlow<MainScreenUiState>
        get() = _uiState.asStateFlow()

    init {
        obterUsuario()
        atualizarListaViagem(false)
        sincronizarFirestore()
    }

    private fun obterUsuario() {
        atualizaMainPage(LOADING)
        viewModelScope.launch {
            dataStore.data.collect {
                val username = it[PreferencesKey.USUARIO_ATUAL]
                val logado = it[PreferencesKey.LOGADO]
                val cargo = it[PreferencesKey.CARGO_ATUAL]

                if (logado != null && logado && cargo != null) {
                    atualizaEhDiretorOuAdm(cargo)
                }

                if (username != null) {
                    _uiState.update { state ->
                        state.copy(
                            userName = username
                        )
                    }
                }
            }
        }
    }

    private fun atualizarListaViagem(isRefreshing: Boolean) {
        viewModelScope.launch {
            delay(1000)
            val listaViagensCard = viagemRepository
                .obterTodas()
                .map {
                    viagemMapper.map(it)
                }

            _uiState.update {
                it.copy(
                    listaViagens = listaViagensCard
                )
            }
            if (isRefreshing) {
                atualizarIsRefresing()
            }
            atualizaMainPage(HOME)

        }
    }

    fun setExibirUserDialog() {
        _uiState.update {
            it.copy(
                exibirUserDialog = !it.exibirUserDialog
            )
        }
    }

    fun atualizaMainPage(
        page: MainScreenState,
    ) {
        when (page) {
            is LOADING -> {
                _uiState.update {
                    it.copy(
                        mainScreenState = page,
                        title = R.string.subtitle_viagens_disponiveis,
                        homeActive = true,
                        passagensActive = false,
                        operacoesActive = false
                    )
                }
            }

            is HOME -> {
                _uiState.update {
                    it.copy(
                        mainScreenState = page,
                        title = R.string.subtitle_viagens_disponiveis,
                        homeActive = true,
                        passagensActive = false,
                        operacoesActive = false
                    )
                }
            }

            is PASSAGENS -> {
                _uiState.update {
                    it.copy(
                        mainScreenState = page,
                        title = R.string.subtitle_menu_passagens,
                        homeActive = false,
                        passagensActive = true,
                        operacoesActive = false
                    )
                }
            }

            is OPERACOES -> {
                _uiState.update {
                    it.copy(
                        mainScreenState = page,
                        title = R.string.subtitle_menu_operacoes,
                        homeActive = false,
                        passagensActive = false,
                        operacoesActive = true
                    )
                }
            }
        }
    }

    private fun atualizaEhDiretorOuAdm(cargo: String) {
        if (cargo == ADM.name ||
            cargo == DIRETOR.name) {
            _uiState.update { state ->
                state.copy(
                    isDiretorOuAdm = true
                )
            }
        }
    }

    suspend fun deslogar() {
        dataStore.edit {
            it[PreferencesKey.LOGADO] = false
            it[PreferencesKey.USUARIO_ATUAL] = ""
            it[PreferencesKey.CARGO_ATUAL] = ""
        }
    }

    private fun atualizarIsRefresing() {
        _uiState.update { it.copy(isRefreshing = !it.isRefreshing) }
    }

    fun refresh() {
        atualizarIsRefresing()
        atualizarListaViagem(isRefreshing = true)
        sincronizarFirestore()
    }

    private fun sincronizarFirestore() {
        viagemRepository.sincronizar()
        passagemRepository.sincronizarNumeroBilheteEmTempoReal()
        agenteRepository.sincronizar()
    }
}
