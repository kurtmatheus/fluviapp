package dev.matheus.fluviapp.ui.viewmodel.empresa

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.operacoes.Atuacao
import dev.matheus.fluviapp.domain.viagem.AtuacaoDaEmpresa
import dev.matheus.fluviapp.domain.viagem.Empresa
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

    /** Liga/desliga uma atuação. Marcar e desmarcar são o mesmo gesto: o conjunto muda nos dois sentidos. */
    fun onAtuacaoToggle(atuacao: Atuacao) = _uiState.update { estado ->
        val atuacoes = if (atuacao in estado.atuacoes) {
            estado.atuacoes - atuacao
        } else {
            estado.atuacoes + atuacao
        }
        estado.copy(atuacoes = atuacoes, isAtuacoesError = false)
    }

    private fun carregar() {
        viewModelScope.launch {
            empresaRepository.obterPorId(idEmpresa)?.let { empresa ->
                val atuacoes = empresaRepository.obterAtuacoes(idEmpresa).map { it.atuacao }.toSet()
                _uiState.update {
                    it.copy(
                        titulo = R.string.subtitle_editar_empresa,
                        nome = empresa.nome,
                        razaoSocial = empresa.razaoSocial,
                        cnpj = empresa.cnpj.filter(Char::isDigit).take(14),
                        endereco = empresa.endereco,
                        telefone1 = empresa.telefone1,
                        telefone2 = empresa.telefone2,
                        atuacoes = atuacoes,
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
                    isAtuacoesError = erros.atuacoes,
                )
            }
            return
        }

        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            val s = _uiState.value
            try {
                // A parte primeiro: é ela que gera o id, e as atuações penduram nele (ADR-0016 §4).
                // A ordem não é preferência — sem id não há subcoleção onde escrever.
                val id = empresaRepository.salvar(
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
                // As concessões (navioIds) não são editadas aqui — o que este form decide é QUAIS
                // atuações a parte exerce. Preservam-se as que já existem, para que salvar a empresa
                // não apague concessão concedida noutro lugar.
                val concessoes = empresaRepository.obterAtuacoes(id).associateBy { it.atuacao }
                empresaRepository.salvarAtuacoes(
                    empresaId = id,
                    atuacoes = s.atuacoes.map { concessoes[it] ?: AtuacaoDaEmpresa(it) },
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
