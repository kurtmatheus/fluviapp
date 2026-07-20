package dev.matheus.fluviapp.ui.viewmodel.passagem

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matheus.fluviapp.business.ImpressaoPassagem
import dev.matheus.fluviapp.model.mappers.PassagemDadosPassagemMapper
import dev.matheus.fluviapp.model.operacoes.PermissoesUsuario
import dev.matheus.fluviapp.model.screendata.DadosPassagem
import dev.matheus.fluviapp.navigation.navcomposables.passagem.DETALHES_PASSAGEM_ARGUMENT
import dev.matheus.fluviapp.preferences.PreferencesKey
import dev.matheus.fluviapp.services.repository.cadastro.passagem.PassagemDigitalRepository
import dev.matheus.fluviapp.services.repository.firebase.PassagemFirestoreRepository
import dev.matheus.fluviapp.services.repository.operacoes.UsuarioRepository
import dev.matheus.fluviapp.ui.states.ImpressaoState
import dev.matheus.fluviapp.ui.states.passagem.DetalhesPassagemState
import dev.matheus.fluviapp.ui.viewmodel.helpers.ImpressaoHelper
import dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.PassagemDigitalHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetalhesPassagemViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val passagemRepository: PassagemFirestoreRepository,
    private val passagemMapper: PassagemDadosPassagemMapper,
    private val usuarioRepository: UsuarioRepository,
    private val passagemDigitalRepository: PassagemDigitalRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetalhesPassagemState())
    val uiState: StateFlow<DetalhesPassagemState>
        get() = _uiState.asStateFlow()

    private val _uiStateImpressao = MutableStateFlow(ImpressaoState())
    val uiStateImpressao: StateFlow<ImpressaoState>
        get() = _uiStateImpressao.asStateFlow()

    private val idPassagem: String = checkNotNull(savedStateHandle[DETALHES_PASSAGEM_ARGUMENT])

    lateinit var impressaoHelper: ImpressaoHelper
    lateinit var passagemDigitalHelper: PassagemDigitalHelper

    init {
        viewModelScope.launch {
            inicializarHelper()
            inicializarState()
        }
    }

    private fun inicializarHelper() {
        impressaoHelper = ImpressaoHelper(
            uiState = _uiStateImpressao,
            passagemRepository = passagemRepository
        )
        passagemDigitalHelper = PassagemDigitalHelper(
            uiState = _uiState,
            idPassagem = idPassagem,
            passagemDigitalRepository = passagemDigitalRepository
        )
    }

    private suspend fun inicializarState() {
        passagemRepository.obterPorId(idPassagem).let { passagem ->
            if (passagem.ehVeiculo) atualizarShowDadosVeiculo()

            val passagemCard = passagemMapper.map(passagem)

            atualizarPassagemCard(passagemCard)
            ImpressaoPassagem.dadosPassagem = passagemCard
        }
        atualizarIsAdminOuFuncResponsavel()
    }

    private fun atualizarPassagemCard(passagemCard: DadosPassagem) {
        _uiState.update {
            it.copy(
                dadosPassagem = passagemCard
            )
        }
    }

    private fun atualizarShowDadosVeiculo() {
        _uiState.update {
            it.copy(
                isShowAreaVeiculo = true
            )
        }
    }

    fun showConfirmDialog() {
        _uiState.update {
            it.copy(
                isShowConfirmReturnDialog = !it.isShowConfirmReturnDialog
            )
        }
    }

    fun showConfirmDeleteDialog() {
        _uiState.update {
            it.copy(
                isShowConfirmDeleteDialog = !it.isShowConfirmDeleteDialog
            )
        }
    }

    fun deletarPassagem(idPassagem: String) {
        passagemRepository.deletar(idPassagem)
    }

    private suspend fun atualizarIsAdminOuFuncResponsavel() {
        val usuarioLogado = usuarioRepository.obterUltimoUsuarioLogado() ?: return
        dataStore.data.collect { preferences ->
            val cargo = preferences[PreferencesKey.CARGO_ATUAL]
            // Posse por nome (dívida da Fase 1, ADR-0010): compara o dono DA PASSAGEM com o logado.
            val ehDono = _uiState.value.dadosPassagem.funcionario == usuarioLogado.nome
            _uiState.update { state ->
                state.copy(
                    isAdminOuFuncResposavel = PermissoesUsuario.podeEditarPassagem(cargo, ehDono)
                )
            }
        }
    }

    fun showSheetEmissao() {
        _uiState.update {
            it.copy(
                isShowSheetEmissao = !it.isShowSheetEmissao
            )
        }
    }

    fun showDialogImpressaoDigital() {
        _uiState.update {
            it.copy(
                isShowDialogImpressaoDigital = !it.isShowDialogImpressaoDigital
            )
        }
    }
}
