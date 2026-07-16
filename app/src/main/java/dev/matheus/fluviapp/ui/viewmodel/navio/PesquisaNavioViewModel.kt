package dev.matheus.fluviapp.ui.viewmodel.navio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matheus.fluviapp.model.viagem.Empresa
import dev.matheus.fluviapp.model.viagem.Navio
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.NavioRepository
import dev.matheus.fluviapp.ui.states.NavioResultado
import dev.matheus.fluviapp.ui.states.PesquisaNavioUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Busca de navios — filtro único por empresa (dropdown). Resolve o nome da empresa a partir do
 * `empresaId` (ADR-0008) contra a lista de empresas em cache; filtro e resolução ficam no VM.
 */
@HiltViewModel
class PesquisaNavioViewModel @Inject constructor(
    private val navioRepository: NavioRepository,
    private val empresaRepository: EmpresaRepository,
) : ViewModel() {

    private var navios: List<Navio> = emptyList()
    private var empresas: List<Empresa> = emptyList()

    private val _uiState = MutableStateFlow(PesquisaNavioUiState())
    val uiState: StateFlow<PesquisaNavioUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { recarregar() }
    }

    fun onEmpresaChange(empresa: String) = _uiState.update {
        it.copy(empresa = empresa, resultados = filtrar(empresa))
    }

    fun onDeletar(id: String) {
        viewModelScope.launch {
            navioRepository.deletar(id)
            recarregar()
        }
    }

    private suspend fun recarregar() {
        navios = navioRepository.obterTodos()
        empresas = empresaRepository.obterTodas()
        _uiState.update {
            it.copy(
                listaEmpresas = empresas.map { empresa -> empresa.nome },
                resultados = filtrar(it.empresa),
            )
        }
    }

    private fun filtrar(empresaNome: String): List<NavioResultado> {
        val idSelecionado = empresas.find { it.nome.equals(empresaNome, ignoreCase = true) }?.id
        return navios
            .filter { empresaNome.isBlank() || it.empresaId == idSelecionado }
            .map { navio ->
                NavioResultado(
                    id = navio.id,
                    nome = navio.descricaoNome,
                    empresaNome = empresas.find { it.id == navio.empresaId }?.nome.orEmpty(),
                )
            }
    }
}
