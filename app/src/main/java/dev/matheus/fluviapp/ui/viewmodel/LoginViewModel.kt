package dev.matheus.fluviapp.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.extensions.toastMessage
import dev.matheus.fluviapp.model.operacoes.Usuario
import dev.matheus.fluviapp.preferences.PreferencesKey.CARGO_ATUAL
import dev.matheus.fluviapp.preferences.PreferencesKey.LOGADO
import dev.matheus.fluviapp.preferences.PreferencesKey.USUARIO_ATUAL
import dev.matheus.fluviapp.services.repository.cadastro.ConstanteRepository
import dev.matheus.fluviapp.services.repository.cadastro.passagem.AgenteRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.NavioRepository
import dev.matheus.fluviapp.services.repository.firebase.PassagemFirestoreRepository
import dev.matheus.fluviapp.services.repository.firebase.SeedFirestore
import dev.matheus.fluviapp.services.repository.firebase.ViagemFirestoreRepository
import dev.matheus.fluviapp.services.repository.operacoes.UsuarioRepository
import dev.matheus.fluviapp.ui.states.LoginUiState
import dev.matheus.fluviapp.ui.viewmodel.helpers.login.LoginFormHelper
import dev.matheus.fluviapp.ui.viewmodel.helpers.login.mapearMensagemErroAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val usuarioRepository: UsuarioRepository,
    private val constanteRepository: ConstanteRepository,
    private val empresaRepository: EmpresaRepository,
    private val navioRepository: NavioRepository,
    private val agenteRepository: AgenteRepository,
    private val viagemRepository: ViagemFirestoreRepository,
    private val passagemRepository: PassagemFirestoreRepository,
    private val seedFirestore: SeedFirestore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState>
        get() = _uiState.asStateFlow()

    lateinit var loginFormHelper: LoginFormHelper

    lateinit var onNavegaParaMainScreen: () -> Unit

    init {
        viewModelScope.launch {
            initializeHelper()
            seedFirestore.semearSeVazio()
            carregarUsuarios()
        }
    }

    private fun carregarUsuarios() {
        loginFormHelper.atualizarCarregandoUsuarios()
        viewModelScope.launch {
            usuarioRepository.carregarUsuarios()
            loginFormHelper.atualizarCarregandoUsuarios()
        }
    }

    private fun initializeHelper() {
        loginFormHelper = LoginFormHelper(
            uiState = _uiState,
            usuarioRepository = usuarioRepository
        )
    }

    fun validarLogin() {
        if (loginFormHelper.isFormularioValido()) {
            _uiState.update { it.copy(logando = true) }
            // Login vai sempre ao Firebase (autoridade da credencial); sem verificação local de
            // senha (ADR-0005). 1º login exige rede — o failure listener trata offline/erro.
            viewModelScope.launch { autenticarUsuario() }
        }
    }

    private suspend fun autenticarUsuario() {
        val email = _uiState.value.email
        val senha = _uiState.value.senha

        usuarioRepository.autenticarUsuario(
            email = email,
            senha = senha
        ).addOnCompleteListener {
            if (it.isSuccessful) {
                if (usuarioRepository.emailVerificado()) {
                    viewModelScope.launch {
                        val usuario = usuarioRepository.salvarUsuarioAutenticado(email)
                        logarUsuario(usuario)
                    }
                } else {
                    // gate ADR-0005/fluxo-login: nao entra sem e-mail verificado; oferece reenviar.
                    usuarioRepository.sair()
                    loginFormHelper.exibeErro()
                    loginFormHelper.setMensagemErro(R.string.error_email_nao_verificado)
                    _uiState.value =
                        _uiState.value.copy(logando = false, exibirReenviarVerificacao = true)
                }
            } else {
                Log.e(TAG, "autenticarUsuario: ${it.exception!!.message}", it.exception)
                exceptionHandle(it.exception!!)
                _uiState.value = _uiState.value.copy(logando = false)
            }
        }
    }

    fun reenviarVerificacao() {
        val email = _uiState.value.email
        val senha = _uiState.value.senha
        _uiState.update { it.copy(logando = true) }
        usuarioRepository.autenticarUsuario(email, senha).addOnCompleteListener { resultado ->
            if (resultado.isSuccessful) {
                usuarioRepository.enviarVerificacao()?.addOnCompleteListener {
                    usuarioRepository.sair()
                    loginFormHelper.exibeErro()
                    loginFormHelper.setMensagemErro(R.string.msg_verificacao_reenviada)
                    _uiState.value =
                        _uiState.value.copy(logando = false, exibirReenviarVerificacao = false)
                }
            } else {
                Log.e(TAG, "reenviarVerificacao: ${resultado.exception?.message}", resultado.exception)
                exceptionHandle(resultado.exception ?: Exception("falha ao reenviar"))
                _uiState.value = _uiState.value.copy(logando = false)
            }
        }
    }

    private fun exceptionHandle(ex: Throwable) {
        ex.printStackTrace()
        loginFormHelper.exibeErro()
        loginFormHelper.setMensagemErro(mapearMensagemErroAuth(ex))
    }

    private suspend fun logarUsuario(usuarioLogado: Usuario?) {
        usuarioLogado?.run {
            dataStore.edit { preferences ->
                preferences[LOGADO] = true
                preferences[USUARIO_ATUAL] = usuarioLogado.nome
                preferences[CARGO_ATUAL] = usuarioLogado.cargo
            }
            _uiState.value = _uiState.value.copy(logado = true)
        } ?: run {
            Log.e(TAG, "logarUsuario: autenticado no Firebase, mas perfil ausente (users vazio no Firestore/Room?)")
            _uiState.value = _uiState.value.copy(logando = false)
            loginFormHelper.exibeErro()
            loginFormHelper.setMensagemErro(R.string.error_falha_auth)
        }
    }

    fun sincronizar(context: Context) {
        try {
            constanteRepository.sincronizar()
            agenteRepository.sincronizar()
            empresaRepository.sincronizar()
            navioRepository.sincronizar()
            viagemRepository.sincronizar()
            passagemRepository.sincronizarNumeroBilheteEmTempoReal()
            onNavegaParaMainScreen()
        } catch (e: Exception) {
            e.printStackTrace()
            context.toastMessage("Falha na Sincronização. Tente Novamente mais tarde.")
        }
    }

    companion object {
        private const val TAG = "loginViewModel"
    }
}