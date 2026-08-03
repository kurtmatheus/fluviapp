package dev.matheus.fluviapp.ui.viewmodel.empresa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matheus.fluviapp.domain.viagem.Empresa
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaRepository
import dev.matheus.fluviapp.ui.states.PesquisaEmpresaUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Busca de empresas — filtra por `nome` (startsWith, ignore case) a lista carregada; filtro no VM. */
@HiltViewModel
class PesquisaEmpresaViewModel @Inject constructor(
    private val empresaRepository: EmpresaRepository,
) : ViewModel() {

    private var todas: List<Empresa> = emptyList()

    private val _uiState = MutableStateFlow(PesquisaEmpresaUiState())
    val uiState: StateFlow<PesquisaEmpresaUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { recarregar() }
    }

    fun onNomeChange(nome: String) = _uiState.update {
        it.copy(nome = nome, resultados = filtrar(nome))
    }

    fun onDeletar(id: String) {
        viewModelScope.launch {
            empresaRepository.deletar(id)
            recarregar()
        }
    }

    private suspend fun recarregar() {
        todas = empresaRepository.obterTodas()
        _uiState.update { it.copy(resultados = filtrar(it.nome)) }
    }

    private fun filtrar(nome: String): List<Empresa> =
        todas.filter { it.nome.startsWith(nome, ignoreCase = true) }
}
