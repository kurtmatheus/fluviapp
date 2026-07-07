package br.com.gruponaveg.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.gruponaveg.R
import br.com.gruponaveg.extensions.toastMessage
import br.com.gruponaveg.model.operacoes.Usuario
import br.com.gruponaveg.preferences.PreferencesKey.CARGO_ATUAL
import br.com.gruponaveg.preferences.PreferencesKey.LOGADO
import br.com.gruponaveg.preferences.PreferencesKey.USUARIO_ATUAL
import br.com.gruponaveg.services.repository.cadastro.ConstanteRepository
import br.com.gruponaveg.services.repository.cadastro.passagem.AgenteRepository
import br.com.gruponaveg.services.repository.cadastro.viagem.EmpresaRepository
import br.com.gruponaveg.services.repository.cadastro.viagem.NavioRepository
import br.com.gruponaveg.services.repository.firebase.PassagemFirestoreRepository
import br.com.gruponaveg.services.repository.firebase.ViagemFirestoreRepository
import br.com.gruponaveg.services.repository.operacoes.UsuarioRepository
import br.com.gruponaveg.ui.states.LoginUiState
import br.com.gruponaveg.ui.viewmodel.helpers.login.LoginFormHelper
import br.com.gruponaveg.util.CriptografiaUtil.Companion.encrypt
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
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
    private val passagemRepository: PassagemFirestoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState>
        get() = _uiState.asStateFlow()

    lateinit var loginFormHelper: LoginFormHelper

    lateinit var onNavegaParaMainScreen: () -> Unit

    init {
        viewModelScope.launch {
            initializeHelper()
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
            viewModelScope.launch {
                val usuarioSalvo =
                    usuarioRepository.obterPorEmailSenha(_uiState.value.email, _uiState.value.senha)
                if (usuarioSalvo != null) {
                    logarUsuario(usuarioSalvo)
                } else {
                    autenticarUsuario()
                }
            }
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
                viewModelScope.launch {
                    val usuario = usuarioRepository.salvarUsuarioAutenticado(email, senha.encrypt())
                    logarUsuario(usuario)
                }
            } else {
                Log.e(TAG, "autenticarUsuario: ${it.exception!!.message}", it.exception)
                exceptionHandle(it.exception!!)
                _uiState.value = _uiState.value.copy(logando = false)
            }
        }
    }

    private fun exceptionHandle(ex: Exception) {
        when (ex) {
            is FirebaseAuthInvalidCredentialsException -> {
                ex.printStackTrace()
                loginFormHelper.exibeErro()
                loginFormHelper.setMensagemErro(R.string.error_usuario_incorreto)
            }

            is FirebaseAuthInvalidUserException -> {
                ex.printStackTrace()
                loginFormHelper.exibeErro()
                loginFormHelper.setMensagemErro(R.string.error_usuario_inexistente)
            }

            else -> {
                ex.printStackTrace()
                loginFormHelper.exibeErro()
                loginFormHelper.setMensagemErro(R.string.error_falha_auth)
            }
        }
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