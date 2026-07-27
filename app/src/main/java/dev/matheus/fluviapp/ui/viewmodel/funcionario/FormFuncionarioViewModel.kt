package dev.matheus.fluviapp.ui.viewmodel.funcionario

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Categoria.MUNICIPIO
import dev.matheus.fluviapp.model.operacoes.Funcionario
import dev.matheus.fluviapp.model.mapDescricao
import dev.matheus.fluviapp.navigation.navcomposables.funcionario.ID_AGENTE_ARGUMENT
import dev.matheus.fluviapp.services.repository.cadastro.ConstanteRepository
import dev.matheus.fluviapp.services.repository.operacoes.FuncionarioRepository
import dev.matheus.fluviapp.ui.states.FormFuncionarioUiState
import dev.matheus.fluviapp.ui.viewmodel.helpers.funcionario.validarFuncionario
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
 * Cadastro/edição de funcionário no molde refatorado (cadastro-modulos §7.2): VM dona do estado (sem
 * FormHelper); eventos são métodos; validação pura; sucesso via evento one-shot; cargas suspensas
 * (sem runBlocking); arg de rota opcional; sem Context. A BUSCA vive em PesquisaFuncionarioViewModel.
 */
@HiltViewModel
class FormFuncionarioViewModel @Inject constructor(
    private val funcionarioRepository: FuncionarioRepository,
    private val constanteRepository: ConstanteRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val idFuncionario: String = savedStateHandle.get<String>(ID_AGENTE_ARGUMENT).orEmpty()

    private val _uiState = MutableStateFlow(FormFuncionarioUiState())
    val uiState: StateFlow<FormFuncionarioUiState> = _uiState.asStateFlow()

    private val _sucesso = Channel<Unit>(Channel.BUFFERED)
    val sucesso = _sucesso.receiveAsFlow()

    init {
        carregarFontes()
        if (idFuncionario.isNotBlank()) carregar()
    }

    private fun carregarFontes() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    listaAgencia = funcionarioRepository.obterTodasAgencias(),
                    listaMunicipios = constanteRepository.obterTodosPorCategoria(MUNICIPIO.name).mapDescricao(),
                )
            }
        }
    }

    private fun carregar() {
        viewModelScope.launch {
            funcionarioRepository.obterPorId(idFuncionario)?.let { funcionario ->
                _uiState.update {
                    it.copy(
                        titulo = R.string.subtitle_editar_agente,
                        agencia = funcionario.agencia,
                        funcionario = funcionario.descricaoNome,
                        lotacao = funcionario.lotacao,
                    )
                }
            }
        }
    }

    fun onAgenciaChange(v: String) = _uiState.update { it.copy(agencia = v, isAgenciaError = false) }
    fun onFuncionarioChange(v: String) = _uiState.update { it.copy(funcionario = v, isFuncionarioError = false) }
    fun onLotacaoChange(v: String) = _uiState.update { it.copy(lotacao = v, isLotacaoError = false) }

    fun salvar() {
        val erros = validarFuncionario(_uiState.value)
        if (!erros.valido) {
            _uiState.update {
                it.copy(
                    isAgenciaError = erros.agencia,
                    isFuncionarioError = erros.funcionario,
                    isLotacaoError = erros.lotacao,
                )
            }
            return
        }

        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            val s = _uiState.value
            try {
                // Edição: parte do funcionário persistido (preserva o id) e aplica os campos do form.
                // Criação: novo funcionário com id vazio (auto-id).
                val base = if (idFuncionario.isNotBlank()) funcionarioRepository.obterPorId(idFuncionario) else null
                val funcionario = (base ?: Funcionario(id = "", descricaoNome = "", agencia = "", lotacao = "")).copy(
                    descricaoNome = s.funcionario,
                    agencia = s.agencia,
                    lotacao = s.lotacao,
                )
                funcionarioRepository.salvar(funcionario)
                _sucesso.send(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "salvar: ${e.message}", e)
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    private companion object {
        const val TAG = "formFuncionarioViewModel"
    }
}
