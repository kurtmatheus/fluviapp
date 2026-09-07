package dev.matheus.fluviapp.ui.viewmodel.helpers.login

import dev.matheus.fluviapp.ui.states.LoginUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * O formulário de login — **e agora sem I/O nenhum**.
 *
 * Ele recebia o `UsuarioRepository` e lia o último logado dentro do próprio `init{}`, com um
 * `runBlocking(Dispatchers.IO)`: o **último `runBlocking` de produção do app** (removido em 2026-09-07).
 * Ele existia por uma incompatibilidade de forma, não por necessidade — a leitura era um `Flow` do Room e
 * construtor não é suspenso, então bloquear era a saída para conciliar as duas coisas.
 *
 * Quem lê agora é o ViewModel, que já está numa corrotina, e entrega o e-mail pronto por
 * [preencherUltimoUsuario]. O helper volta a ser o que o [ADR-0026] pede das classes auxiliares:
 * transformação de estado, sem repositório.
 */
class LoginFormHelper(
    private val uiState: MutableStateFlow<LoginUiState>,
) {
    init {
        initializeFields()
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

    /** Preenche o campo com quem entrou por último. `null` (ninguém entrou ainda) deixa o campo em paz. */
    fun preencherUltimoUsuario(email: String?) {
        if (email.isNullOrBlank()) return
        uiState.update { it.copy(email = email) }
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
