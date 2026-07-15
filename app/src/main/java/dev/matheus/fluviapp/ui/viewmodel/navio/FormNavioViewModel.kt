package dev.matheus.fluviapp.ui.viewmodel.navio

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.model.viagem.Navio
import dev.matheus.fluviapp.navigation.navcomposables.navio.ID_NAVIO_ARGUMENT
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.NavioRepository
import dev.matheus.fluviapp.ui.states.FormNavioUiState
import dev.matheus.fluviapp.ui.viewmodel.helpers.navio.validarNavio
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
 * Cadastro/edição de navio no molde refatorado (cadastro-modulos §7.2): VM dona do estado; eventos
 * são métodos; validação pura; sucesso via evento one-shot; cargas suspensas (sem runBlocking); arg
 * de rota opcional; sem Context. Vínculo N-1 com Empresa por nome, via dropdown de `listaEmpresas`.
 */
@HiltViewModel
class FormNavioViewModel @Inject constructor(
    private val navioRepository: NavioRepository,
    private val empresaRepository: EmpresaRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // "" = criação; id preenchido = edição (arg de rota opcional, sem sentinela "null").
    private val idNavio: String = savedStateHandle.get<String>(ID_NAVIO_ARGUMENT).orEmpty()

    private val _uiState = MutableStateFlow(FormNavioUiState())
    val uiState: StateFlow<FormNavioUiState> = _uiState.asStateFlow()

    private val _sucesso = Channel<Unit>(Channel.BUFFERED)
    val sucesso = _sucesso.receiveAsFlow()

    init {
        carregarFontes()
        if (idNavio.isNotBlank()) carregar()
    }

    private fun carregarFontes() {
        viewModelScope.launch {
            _uiState.update { it.copy(listaEmpresas = empresaRepository.obterTodas()) }
        }
    }

    fun onNomeChange(v: String) = _uiState.update { it.copy(nome = v, isNomeError = false) }
    fun onEmpresaChange(v: String) = _uiState.update { it.copy(empresa = v, isEmpresaError = false) }
    fun onCapacidadeVeiculoChange(v: String) = _uiState.update { it.copy(capacidadeVeiculo = v.filter(Char::isDigit)) }
    fun onCapacidadeSuite2Change(v: String) = _uiState.update { it.copy(capacidadeSuite2 = v.filter(Char::isDigit)) }
    fun onCapacidadeSuite3Change(v: String) = _uiState.update { it.copy(capacidadeSuite3 = v.filter(Char::isDigit)) }
    fun onCapacidadeCamaroteChange(v: String) = _uiState.update { it.copy(capacidadeCamarote = v.filter(Char::isDigit)) }

    private fun carregar() {
        viewModelScope.launch {
            navioRepository.obterPorId(idNavio)?.let { navio ->
                _uiState.update {
                    it.copy(
                        titulo = R.string.subtitle_editar_navio,
                        nome = navio.descricaoNome,
                        empresa = navio.empresa,
                        capacidadeVeiculo = navio.capacidadeVeiculo.toString(),
                        capacidadeSuite2 = navio.capacidadeSuite2.toString(),
                        capacidadeSuite3 = navio.capacidadeSuite3.toString(),
                        capacidadeCamarote = navio.capacidadeCamarote.toString(),
                    )
                }
            }
        }
    }

    fun salvar() {
        val erros = validarNavio(_uiState.value)
        if (!erros.valido) {
            _uiState.update { it.copy(isNomeError = erros.nome, isEmpresaError = erros.empresa) }
            return
        }

        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            val s = _uiState.value
            try {
                navioRepository.salvar(
                    Navio(
                        id = idNavio, // "" na criação → auto-id no repo
                        descricaoNome = s.nome,
                        capacidadeVeiculo = s.capacidadeVeiculo.toIntOrNull() ?: 0,
                        capacidadeSuite2 = s.capacidadeSuite2.toIntOrNull() ?: 0,
                        capacidadeSuite3 = s.capacidadeSuite3.toIntOrNull() ?: 0,
                        capacidadeCamarote = s.capacidadeCamarote.toIntOrNull() ?: 0,
                        empresa = s.empresa,
                    )
                )
                _sucesso.send(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "salvar: ${e.message}", e)
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    private companion object {
        const val TAG = "formNavioViewModel"
    }
}
