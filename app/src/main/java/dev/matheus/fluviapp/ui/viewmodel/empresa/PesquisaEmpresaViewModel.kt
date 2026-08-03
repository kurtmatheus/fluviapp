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

/**
 * Busca de empresas — filtra por `nome` (startsWith, ignore case) a lista observada; filtro no VM.
 *
 * **Fonte reativa, não leitura de uma vez** (ADR-0017 D1): a lista vem do `StateFlow` que o listener do
 * Firestore alimenta, e não de um `obterTodas()` chamado à mão. A diferença aparece justamente onde o
 * repositório não compensa testar: cadastrar numa tela e voltar para esta, ou o dado mudar no servidor,
 * atualiza a lista sozinho — antes dependia de alguém lembrar de recarregar, e `onDeletar` era o único
 * lugar que lembrava.
 */
@HiltViewModel
class PesquisaEmpresaViewModel @Inject constructor(
    private val empresaRepository: EmpresaRepository,
) : ViewModel() {

    private var todas: List<Empresa> = emptyList()

    private val _uiState = MutableStateFlow(PesquisaEmpresaUiState())
    val uiState: StateFlow<PesquisaEmpresaUiState> = _uiState.asStateFlow()

    init {
        // `observarTodas` é só a janela para o StateFlow: sem ligar o listener, ninguém o alimenta. Antes
        // quem o ligava era o `obterTodas()` lá dentro, de carona.
        empresaRepository.sincronizar()
        viewModelScope.launch {
            empresaRepository.observarTodas().collect { empresas ->
                todas = empresas
                _uiState.update { it.copy(resultados = filtrar(it.nome)) }
            }
        }
    }

    fun onNomeChange(nome: String) = _uiState.update {
        it.copy(nome = nome, resultados = filtrar(nome))
    }

    /** Não recarrega: apagar emite um snapshot novo, e é por ele que a lista encolhe. */
    fun onDeletar(id: String) {
        viewModelScope.launch { empresaRepository.deletar(id) }
    }

    private fun filtrar(nome: String): List<Empresa> =
        todas.filter { it.nome.startsWith(nome, ignoreCase = true) }
}
