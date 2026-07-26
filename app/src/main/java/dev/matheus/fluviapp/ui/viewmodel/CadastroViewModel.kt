package dev.matheus.fluviapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matheus.fluviapp.services.repository.firebase.autenticacao.AutenticacaoRepository
import dev.matheus.fluviapp.services.repository.firebase.autenticacao.MotivoFalhaAuth
import dev.matheus.fluviapp.services.repository.firebase.autenticacao.ResultadoAutenticacao
import dev.matheus.fluviapp.ui.states.CadastroUiState
import dev.matheus.fluviapp.ui.viewmodel.helpers.cadastro.CadastroFormHelper
import dev.matheus.fluviapp.ui.viewmodel.helpers.login.mapearMensagemErroAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CadastroViewModel @Inject constructor(
    private val autenticacaoRepository: AutenticacaoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CadastroUiState())
    val uiState: StateFlow<CadastroUiState>
        get() = _uiState.asStateFlow()

    val cadastroFormHelper = CadastroFormHelper(_uiState)

    fun cadastrar() {
        if (!cadastroFormHelper.isFormularioValido()) return

        _uiState.update { it.copy(cadastrando = true) }
        val email = _uiState.value.email
        val senha = _uiState.value.senha
        val nome = _uiState.value.nome

        viewModelScope.launch {
            when (val resultado = autenticacaoRepository.cadastrar(email, senha)) {
                is ResultadoAutenticacao.Sucesso -> {
                    // perfil na verdade (Firestore); nao entra logado (gate exige verificado).
                    autenticacaoRepository.criarPerfil(email = email, nome = nome, cargo = CARGO_PADRAO)
                    autenticacaoRepository.sair()
                    _uiState.update { it.copy(cadastrando = false, cadastrado = true) }
                }

                is ResultadoAutenticacao.Falha -> {
                    if (resultado.motivo == MotivoFalhaAuth.EMAIL_JA_CADASTRADO) {
                        // fluxo ativo: e-mail existe -> redireciona ao login com ele preenchido.
                        _uiState.update { it.copy(cadastrando = false, irParaLoginComEmail = email) }
                    } else {
                        cadastroFormHelper.exibeErro()
                        cadastroFormHelper.setMensagemErro(mapearMensagemErroAuth(resultado.motivo))
                        _uiState.update { it.copy(cadastrando = false) }
                    }
                }
            }
        }
    }

    companion object {
        private const val CARGO_PADRAO = "AGENTE"
    }
}