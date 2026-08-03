package dev.matheus.fluviapp.ui.viewmodel.funcionario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matheus.fluviapp.domain.operacoes.Funcionario
import dev.matheus.fluviapp.domain.operacoes.PermissoesUsuario
import dev.matheus.fluviapp.services.repository.operacoes.FuncionarioRepository
import dev.matheus.fluviapp.services.repository.operacoes.SessaoUsuario
import dev.matheus.fluviapp.ui.states.PesquisaFuncionarioUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Busca de funcionários — VM próprio (não mais compartilhado com o formulário). Os filtros rodam aqui
 * (não no composable): `resultados` já vem filtrado.
 *
 * Mesma tela, **dois recortes** (ADR-0015 §2.2): a plataforma vê todas as agências e filtra por elas; o
 * supervisor vê só a própria — e o recorte é aplicado ao *universo* (`todos`), não ao filtro, para que
 * nenhum caminho de UI o contorne. É a antecipação do escopo por agência (§4.1) na primeira tela que
 * precisa dele; a listagem de passagens só ganha isso em P2.6.
 */
@HiltViewModel
class PesquisaFuncionarioViewModel @Inject constructor(
    private val funcionarioRepository: FuncionarioRepository,
    private val sessaoUsuario: SessaoUsuario,
) : ViewModel() {

    private var todos: List<Funcionario> = emptyList()

    /** Agência do logado quando ele não vê todas; vazia para a plataforma. */
    private var agenciaDoEscopo: String = ""

    private val _uiState = MutableStateFlow(PesquisaFuncionarioUiState())
    val uiState: StateFlow<PesquisaFuncionarioUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            aplicarRecorte()
            recarregar()
        }
    }

    private suspend fun aplicarRecorte() {
        val contexto = sessaoUsuario.atual()
        val veTodasAgencias = PermissoesUsuario.podeVerTodasAgencias(contexto?.papel)
        agenciaDoEscopo = if (veTodasAgencias) "" else contexto?.agencia.orEmpty()
        _uiState.update {
            it.copy(
                podeFiltrarPorAgencia = veTodasAgencias,
                podeDeletar = PermissoesUsuario.podeDeletarFuncionario(contexto?.papel),
            )
        }
    }

    fun onAgenciaChange(agencia: String) = _uiState.update {
        if (!it.podeFiltrarPorAgencia) it
        else it.copy(agencia = agencia, resultados = filtrar(agencia, it.lotacao))
    }

    fun onLotacaoChange(lotacao: String) = _uiState.update {
        it.copy(lotacao = lotacao, resultados = filtrar(it.agencia, lotacao))
    }

    fun onDeletar(id: String) {
        // Segunda barreira do mesmo recorte: a tela esconde o botão, mas o VM também recusa — o estado
        // pode ter sido montado antes do recorte, e deletar não é reversível.
        if (!_uiState.value.podeDeletar) return
        viewModelScope.launch {
            funcionarioRepository.deletar(id)
            recarregar()
        }
    }

    /** Recarrega `todos` do repo (já recortado) e reaplica os filtros correntes (init e pós-deleção). */
    private suspend fun recarregar() {
        todos = if (agenciaDoEscopo.isBlank()) {
            funcionarioRepository.obterTodosFuncionarios()
        } else {
            funcionarioRepository.obterFuncionariosPorAgencia(agenciaDoEscopo)
        }
        _uiState.update {
            it.copy(
                listaAgencia = if (it.podeFiltrarPorAgencia) funcionarioRepository.obterTodasAgencias() else emptyList(),
                listaLotacao = todos.map { funcionario -> funcionario.lotacao }.distinct(),
                resultados = filtrar(it.agencia, it.lotacao),
            )
        }
    }

    // Agência: prefixo (dropdown editável). Lotação: seleção exata do dropdown (vazio = sem filtro).
    private fun filtrar(agencia: String, lotacao: String): List<Funcionario> =
        todos.filter {
            it.agencia.startsWith(agencia, ignoreCase = true) &&
                (lotacao.isBlank() || it.lotacao.equals(lotacao, ignoreCase = true))
        }
}