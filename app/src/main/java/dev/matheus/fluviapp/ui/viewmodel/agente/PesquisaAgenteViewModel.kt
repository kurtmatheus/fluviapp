package dev.matheus.fluviapp.ui.viewmodel.agente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matheus.fluviapp.model.cadastro.passagem.Agente
import dev.matheus.fluviapp.services.repository.cadastro.passagem.AgenteRepository
import dev.matheus.fluviapp.ui.states.PesquisaAgenteUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Busca de agentes — VM próprio (não mais compartilhado com o formulário). O filtro por agência
 * roda aqui (não no composable): `resultados` já vem filtrado.
 */
@HiltViewModel
class PesquisaAgenteViewModel @Inject constructor(
    private val agenteRepository: AgenteRepository,
) : ViewModel() {

    private var todos: List<Agente> = emptyList()

    private val _uiState = MutableStateFlow(PesquisaAgenteUiState())
    val uiState: StateFlow<PesquisaAgenteUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            recarregar()
        }
    }

    fun onAgenciaChange(agencia: String) = _uiState.update {
        it.copy(agencia = agencia, resultados = filtrar(agencia, it.lotacao))
    }

    fun onLotacaoChange(lotacao: String) = _uiState.update {
        it.copy(lotacao = lotacao, resultados = filtrar(it.agencia, lotacao))
    }

    fun onDeletar(id: String) {
        viewModelScope.launch {
            agenteRepository.deletar(id)
            recarregar()
        }
    }

    /** Recarrega `todos` do repo e reaplica os filtros correntes (usado no init e pós-deleção). */
    private suspend fun recarregar() {
        todos = agenteRepository.obterTodosAgentes()
        _uiState.update {
            it.copy(
                listaAgencia = agenteRepository.obterTodasAgencias(),
                listaLotacao = todos.map { agente -> agente.lotacao }.distinct(),
                resultados = filtrar(it.agencia, it.lotacao),
            )
        }
    }

    // Agência: prefixo (dropdown editável). Lotação: seleção exata do dropdown (vazio = sem filtro).
    private fun filtrar(agencia: String, lotacao: String): List<Agente> =
        todos.filter {
            it.agencia.startsWith(agencia, ignoreCase = true) &&
                (lotacao.isBlank() || it.lotacao.equals(lotacao, ignoreCase = true))
        }
}
