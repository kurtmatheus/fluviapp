package dev.matheus.fluviapp.ui.viewmodel.helpers.login

import dev.matheus.fluviapp.services.repository.operacoes.UsuarioRepository
import dev.matheus.fluviapp.ui.states.LoginUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking

class LoginFormHelper(
    private val uiState: MutableStateFlow<LoginUiState>,
    private val usuarioRepository: UsuarioRepository
) {
    init {
        initializeFields()
        preencherCampoUltimoUsuario()
    }

    private fun initializeFields() {
        uiState.update { state ->
            state.copy(
                onUsuarioChange = {
                    atualizaUsuario(it)
                },
                onSenhaChange = {
                    atualizaSenha(it)
                }
            )
        }
    }

    private fun preencherCampoUltimoUsuario() {
        runBlocking(Dispatchers.IO) {
            usuarioRepository.obterUltimoUsuarioLogado()?.run {
                uiState.update {
                    it.copy(
                        email = email
                    )
                }
            }
        }
    }

    private fun atualizaUsuario(usuario: String) {
        uiState.update {
            it.copy(
                email = usuario,
                isUsuarioError = false
            )
        }
    }

    private fun atualizaSenha(senha: String) {
        uiState.update {
            it.copy(
                senha = senha,
                isSenhaError = false
            )
        }
    }

    fun exibeErro() {
        uiState.update {
            it.copy(
                exibirErro = true
            )
        }
    }

    fun setMensagemErro(mensagemErro: Int) {
        uiState.update {
            it.copy(
                mensagemErro = mensagemErro
            )
        }
    }

    fun updateSenhaVisible() {
        uiState.update {
            it.copy(
                isSenhaVisible = !it.isSenhaVisible
            )
        }
    }

    fun atualizarCarregandoUsuarios() {
        uiState.update {
            it.copy(
                carregandoUsuarios = !it.carregandoUsuarios
            )
        }
    }

    fun isFormularioValido(): Boolean {
        if (uiState.value.email.isBlank()) {
            uiState.update {
                it.copy(
                    isUsuarioError = true
                )
            }
        }

        if (uiState.value.senha.isBlank()) {
            uiState.update {
                it.copy(
                    isSenhaError = true
                )
            }
        }

        return !uiState.value.isUsuarioError &&
                !uiState.value.isSenhaError
    }


}
