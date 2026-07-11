package dev.matheus.fluviapp.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.navigation.destinations.ARG_EMAIL_PREFILL
import dev.matheus.fluviapp.services.repository.firebase.autenticacao.AutenticacaoRepository
import dev.matheus.fluviapp.services.repository.firebase.autenticacao.ResultadoAutenticacao
import dev.matheus.fluviapp.ui.states.RecuperarSenhaUiState
import dev.matheus.fluviapp.ui.viewmodel.helpers.login.mapearMensagemErroAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Recuperação de senha em tela própria — não reaproveita o e-mail do login. O campo pode vir
 * pré-preenchido (arg de navegação, por conveniência), mas é editável e independente.
 */
@HiltViewModel
class RecuperarSenhaViewModel @Inject constructor(
    private val autenticacaoRepository: AutenticacaoRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        RecuperarSenhaUiState(email = savedStateHandle.get<String>(ARG_EMAIL_PREFILL).orEmpty())
    )
    val uiState: StateFlow<RecuperarSenhaUiState>
        get() = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(onEmailChange = ::onEmailChange) }
    }

    private fun onEmailChange(novo: String) {
        _uiState.update {
            it.copy(email = novo, isEmailError = false, exibirMensagem = false)
        }
    }

    fun recuperar() {
        val email = _uiState.value.email.trim()
        if (email.isBlank()) {
            _uiState.update {
                it.copy(
                    isEmailError = true,
                    exibirMensagem = true,
                    isSucesso = false,
                    mensagem = R.string.error_camp_obrig
                )
            }
            return
        }

        _uiState.update { it.copy(enviando = true) }
        viewModelScope.launch {
            when (val resultado = autenticacaoRepository.recuperarSenha(email)) {
                is ResultadoAutenticacao.Sucesso -> _uiState.update {
                    it.copy(
                        enviando = false,
                        exibirMensagem = true,
                        isSucesso = true,
                        mensagem = R.string.msg_recuperacao_enviada
                    )
                }

                is ResultadoAutenticacao.Falha -> _uiState.update {
                    it.copy(
                        enviando = false,
                        exibirMensagem = true,
                        isSucesso = false,
                        mensagem = mapearMensagemErroAuth(resultado.motivo)
                    )
                }
            }
        }
    }
}