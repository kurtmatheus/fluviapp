package dev.matheus.fluviapp.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import dev.matheus.fluviapp.services.repository.operacoes.UsuarioRepository
import dev.matheus.fluviapp.ui.states.CadastroUiState
import dev.matheus.fluviapp.ui.viewmodel.helpers.cadastro.CadastroFormHelper
import dev.matheus.fluviapp.ui.viewmodel.helpers.login.mapearMensagemErroAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class CadastroViewModel @Inject constructor(
    private val usuarioRepository: UsuarioRepository,
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

        usuarioRepository.cadastrar(email, senha).addOnCompleteListener { resultado ->
            if (resultado.isSuccessful) {
                // perfil na verdade (Firestore) + verificacao; nao entra logado (gate exige verificado).
                usuarioRepository.criarPerfil(email = email, nome = nome, cargo = CARGO_PADRAO)
                val verificacao = usuarioRepository.enviarVerificacao()
                if (verificacao != null) {
                    verificacao.addOnCompleteListener { concluir() }
                } else {
                    concluir()
                }
            } else {
                Log.e(TAG, "cadastrar: ${resultado.exception?.message}", resultado.exception)
                cadastroFormHelper.exibeErro()
                cadastroFormHelper.setMensagemErro(
                    mapearMensagemErroAuth(resultado.exception ?: Exception("falha no cadastro")),
                )
                _uiState.update { it.copy(cadastrando = false) }
            }
        }
    }

    private fun concluir() {
        usuarioRepository.sair()
        _uiState.update { it.copy(cadastrando = false, cadastrado = true) }
    }

    companion object {
        private const val TAG = "cadastroViewModel"
        private const val CARGO_PADRAO = "OPERADOR"
    }
}