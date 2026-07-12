package dev.matheus.fluviapp.ui.viewmodel.agente

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Categoria.MUNICIPIO
import dev.matheus.fluviapp.model.cadastro.passagem.Agente
import dev.matheus.fluviapp.model.mapDescricao
import dev.matheus.fluviapp.navigation.navcomposables.agente.ID_AGENTE_ARGUMENT
import dev.matheus.fluviapp.services.repository.cadastro.ConstanteRepository
import dev.matheus.fluviapp.services.repository.cadastro.passagem.AgenteRepository
import dev.matheus.fluviapp.ui.states.FormAgenteUiState
import dev.matheus.fluviapp.ui.viewmodel.helpers.agente.validarAgente
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Cadastro/edição de agente no molde refatorado (cadastro-modulos §7.2): VM dona do estado (sem
 * FormHelper); eventos são métodos; validação pura; sucesso via evento one-shot; cargas suspensas
 * (sem runBlocking); arg de rota opcional; sem Context. A BUSCA vive em PesquisaAgenteViewModel.
 */
@HiltViewModel
class FormAgenteViewModel @Inject constructor(
    private val agenteRepository: AgenteRepository,
    private val constanteRepository: ConstanteRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val idAgente: String = savedStateHandle.get<String>(ID_AGENTE_ARGUMENT).orEmpty()

    private val _uiState = MutableStateFlow(FormAgenteUiState())
    val uiState: StateFlow<FormAgenteUiState> = _uiState.asStateFlow()

    private val _sucesso = Channel<Unit>(Channel.BUFFERED)
    val sucesso = _sucesso.receiveAsFlow()

    init {
        carregarFontes()
        if (idAgente.isNotBlank()) carregar()
    }

    private fun carregarFontes() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    listaAgencia = agenteRepository.obterTodasAgencias(),
                    listaMunicipios = constanteRepository.obterTodosPorCategoria(MUNICIPIO.name).mapDescricao(),
                )
            }
        }
    }

    private fun carregar() {
        viewModelScope.launch {
            agenteRepository.obterPorId(idAgente)?.let { agente ->
                _uiState.update {
                    it.copy(
                        titulo = R.string.subtitle_editar_agente,
                        agencia = agente.agencia,
                        agente = agente.descricaoNome,
                        lotacao = agente.lotacao,
                    )
                }
            }
        }
    }

    fun onAgenciaChange(v: String) = _uiState.update { it.copy(agencia = v, isAgenciaError = false) }
    fun onAgenteChange(v: String) = _uiState.update { it.copy(agente = v, isAgenteError = false) }
    fun onLotacaoChange(v: String) = _uiState.update { it.copy(lotacao = v, isLotacaoError = false) }

    fun salvar() {
        val erros = validarAgente(_uiState.value)
        if (!erros.valido) {
            _uiState.update {
                it.copy(
                    isAgenciaError = erros.agencia,
                    isAgenteError = erros.agente,
                    isLotacaoError = erros.lotacao,
                )
            }
            return
        }

        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            val s = _uiState.value
            try {
                // Edição: parte do agente persistido (preserva id e podeSelecionarFormaPagamento) e
                // aplica os campos do form. Criação: novo agente com id vazio (auto-id).
                val base = if (idAgente.isNotBlank()) agenteRepository.obterPorId(idAgente) else null
                val agente = (base ?: Agente(id = "", descricaoNome = "", agencia = "", lotacao = "")).copy(
                    descricaoNome = s.agente,
                    agencia = s.agencia,
                    lotacao = s.lotacao,
                )
                agenteRepository.salvar(agente)
                _sucesso.send(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "salvar: ${e.message}", e)
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    private companion object {
        const val TAG = "formAgenteViewModel"
    }
}
