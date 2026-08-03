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

    // `atualizarCarregandoUsuarios` saiu com o espelho da coleção `users`: não há mais carga de usuários
    // para esperar antes de mostrar o formulário. O `carregandoUsuarios` do estado fica em `false`, e o
    // ramo de spinner da LoginScreen virou resíduo — some quando aquela tela for revitalizada.

    fun isFormularioValido(): Boolean {
        uiState.update { validarCamposLogin(it) }
        return uiState.value.camposValidos()
    }


}
