package dev.matheus.fluviapp.ui.viewmodel.empresa

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.model.viagem.Empresa
import dev.matheus.fluviapp.navigation.navcomposables.empresa.ID_EMPRESA_ARGUMENT
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaRepository
import dev.matheus.fluviapp.ui.states.FormEmpresaUiState
import dev.matheus.fluviapp.ui.viewmodel.helpers.empresa.validarEmpresa
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
 * Cadastro/edição de empresa no molde refatorado (cadastro-modulos §7.2): VM dona do estado;
 * eventos são métodos (sem lambdas no state); validação pura; sucesso via evento one-shot
 * (consumido por LaunchedEffect na navegação). Sem Context, sem navegar-no-finally, sem runBlocking.
 */
@HiltViewModel
class FormEmpresaViewModel @Inject constructor(
    private val empresaRepository: EmpresaRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // "" = criação; id preenchido = edição (arg de rota opcional, sem sentinela "null").
    private val idEmpresa: String = savedStateHandle.get<String>(ID_EMPRESA_ARGUMENT).orEmpty()

    private val _uiState = MutableStateFlow(FormEmpresaUiState())
    val uiState: StateFlow<FormEmpresaUiState> = _uiState.asStateFlow()

    private val _sucesso = Channel<Unit>(Channel.BUFFERED)
    val sucesso = _sucesso.receiveAsFlow()

    init {
        if (idEmpresa.isNotBlank()) carregar()
    }

    fun onNomeChange(v: String) = _uiState.update { it.copy(nome = v, isNomeError = false) }
    fun onRazaoSocialChange(v: String) = _uiState.update { it.copy(razaoSocial = v, isRazaoSocialError = false) }
    fun onCnpjChange(v: String) = _uiState.update { it.copy(cnpj = v.filter(Char::isDigit).take(14), isCnpjError = false) }
    fun onEnderecoChange(v: String) = _uiState.update { it.copy(endereco = v) }
    fun onTelefone1Change(v: String) = _uiState.update { it.copy(telefone1 = v) }
    fun onTelefone2Change(v: String) = _uiState.update { it.copy(telefone2 = v) }

    private fun carregar() {
        viewModelScope.launch {
            empresaRepository.obterPorId(idEmpresa)?.let { empresa ->
                _uiState.update {
                    it.copy(
                        titulo = R.string.subtitle_editar_empresa,
                        nome = empresa.nome,
                        razaoSocial = empresa.razaoSocial,
                        cnpj = empresa.cnpj.filter(Char::isDigit).take(14),
                        endereco = empresa.endereco,
                        telefone1 = empresa.telefone1,
                        telefone2 = empresa.telefone2,
                    )
                }
            }
        }
    }

    fun salvar() {
        val erros = validarEmpresa(_uiState.value)
        if (!erros.valido) {
            _uiState.update {
                it.copy(
                    isNomeError = erros.nome,
                    isRazaoSocialError = erros.razaoSocial,
                    isCnpjError = erros.cnpj,
                )
            }
            return
        }

        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            val s = _uiState.value
            try {
                empresaRepository.salvar(
                    Empresa(
                        id = idEmpresa,
                        nome = s.nome,
                        razaoSocial = s.razaoSocial,
                        cnpj = s.cnpj,
                        endereco = s.endereco,
                        telefone1 = s.telefone1,
                        telefone2 = s.telefone2,
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
        const val TAG = "formEmpresaViewModel"
    }
}
