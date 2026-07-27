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
import dev.matheus.fluviapp.preferences.PreferencesKey.PAPEL_ATUAL
import dev.matheus.fluviapp.preferences.PreferencesKey.LOGADO
import dev.matheus.fluviapp.preferences.PreferencesKey.USUARIO_ATUAL
import dev.matheus.fluviapp.services.repository.cadastro.ConstanteRepository
import dev.matheus.fluviapp.services.repository.operacoes.FuncionarioRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.NavioRepository
import dev.matheus.fluviapp.services.repository.firebase.PassagemFirestoreRepository
import dev.matheus.fluviapp.services.repository.firebase.SeedFirestore
import dev.matheus.fluviapp.services.repository.firebase.ViagemFirestoreRepository
import dev.matheus.fluviapp.services.repository.firebase.autenticacao.AutenticacaoRepository
import dev.matheus.fluviapp.services.repository.firebase.autenticacao.PerfilAutenticado
import dev.matheus.fluviapp.services.repository.firebase.autenticacao.ResultadoAutenticacao
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
    private val autenticacaoRepository: AutenticacaoRepository,
    private val constanteRepository: ConstanteRepository,
    private val empresaRepository: EmpresaRepository,
    private val navioRepository: NavioRepository,
    private val funcionarioRepository: FuncionarioRepository,
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

    fun preencherEmail(email: String) {
        _uiState.update { it.copy(email = email) }
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

        when (val resultado = autenticacaoRepository.autenticar(email, senha)) {
            is ResultadoAutenticacao.Sucesso -> {
                if (resultado.emailVerificado) {
                    val usuario = usuarioRepository.salvarUsuarioAutenticado(email)
                    // Uma leitura a mais no login para resolver o contexto de NEGÓCIO (cargo + nome do
                    // funcionário): é o mesmo caminho de dois saltos das regras (ADR-0015 §8.3), e o
                    // espelho Room de `funcionarios` ainda não sincronizou neste ponto do fluxo.
                    logarUsuario(usuario, autenticacaoRepository.perfilAutenticado())
                } else {
                    // gate: nao entra sem e-mail verificado; oferece reenviar.
                    autenticacaoRepository.sair()
                    loginFormHelper.exibeErro()
                    loginFormHelper.setMensagemErro(R.string.error_email_nao_verificado)
                    _uiState.value =
                        _uiState.value.copy(logando = false, exibirReenviarVerificacao = true)
                }
            }

            is ResultadoAutenticacao.Falha -> {
                loginFormHelper.exibeErro()
                loginFormHelper.setMensagemErro(mapearMensagemErroAuth(resultado.motivo))
                _uiState.value = _uiState.value.copy(logando = false)
            }
        }
    }

    /**
     * Login com Google: recebe o idToken (obtido na borda de UI via Credential Manager),
     * autentica na porta, semeia o perfil (Room) e abre a sessão. O `LaunchedEffect(logado)` da
     * tela cuida de sincronizar e navegar — mesmo caminho do login por e-mail/senha.
     */
    fun autenticarComGoogle(idToken: String) {
        _uiState.update { it.copy(logando = true) }
        viewModelScope.launch {
            when (val resultado = autenticacaoRepository.autenticarComGoogle(idToken)) {
                is ResultadoAutenticacao.Sucesso -> {
                    val perfil = autenticacaoRepository.perfilAutenticado()
                    if (perfil != null) {
                        // Semeia o Room antes de marcar a sessão (evita corrida com o listener).
                        usuarioRepository.salvar(
                            Usuario(
                                id = perfil.id,
                                email = perfil.email,
                                username = perfil.username,
                                papel = perfil.papel,
                                funcionarioId = perfil.funcionarioId,
                            )
                        )
                        logarUsuario(usuarioRepository.salvarUsuarioAutenticado(perfil.email), perfil)
                    } else {
                        Log.e(TAG, "autenticarComGoogle: signIn OK mas perfil ausente em users/{uid}")
                        loginFormHelper.exibeErro()
                        loginFormHelper.setMensagemErro(R.string.error_falha_auth)
                        _uiState.value = _uiState.value.copy(logando = false)
                    }
                }

                is ResultadoAutenticacao.Falha -> {
                    Log.e(TAG, "autenticarComGoogle: Falha motivo=${resultado.motivo}")
                    loginFormHelper.exibeErro()
                    loginFormHelper.setMensagemErro(mapearMensagemErroAuth(resultado.motivo))
                    _uiState.value = _uiState.value.copy(logando = false)
                }
            }
        }
    }

    /** Falha na borda do Credential Manager (não-cancelamento) — feedback ao usuário. */
    fun falhaLoginGoogle() {
        loginFormHelper.exibeErro()
        loginFormHelper.setMensagemErro(R.string.error_google)
        _uiState.update { it.copy(logando = false) }
    }

    fun reenviarVerificacao() {
        val email = _uiState.value.email
        val senha = _uiState.value.senha
        _uiState.update { it.copy(logando = true) }
        viewModelScope.launch {
            when (val resultado = autenticacaoRepository.reenviarVerificacao(email, senha)) {
                is ResultadoAutenticacao.Sucesso -> {
                    loginFormHelper.exibeErro()
                    loginFormHelper.setMensagemErro(R.string.msg_verificacao_reenviada)
                    _uiState.value =
                        _uiState.value.copy(logando = false, exibirReenviarVerificacao = false)
                }

                is ResultadoAutenticacao.Falha -> {
                    loginFormHelper.exibeErro()
                    loginFormHelper.setMensagemErro(mapearMensagemErroAuth(resultado.motivo))
                    _uiState.value = _uiState.value.copy(logando = false)
                }
            }
        }
    }

    /**
     * Abre a sessão com os DOIS contextos (ADR-0015 §8.2): o papel vem do perfil de sistema, o cargo do
     * funcionário ligado. O [perfil] é resolvido na porta de autenticação — quando ele não vem (login
     * por e-mail/senha lendo o espelho Room), a sessão nasce **sem cargo**, que é fail-closed: o membro
     * enxerga só o que o papel concede até o próximo login com rede.
     *
     * O que se exibe é o **nome do funcionário**; sem funcionário (papel puro de plataforma), o
     * `username` — o `Usuario` não tem nome (§8.1).
     */
    private suspend fun logarUsuario(usuarioLogado: Usuario?, perfil: PerfilAutenticado? = null) {
        usuarioLogado?.run {
            dataStore.edit { preferences ->
                preferences[LOGADO] = true
                preferences[USUARIO_ATUAL] = perfil?.nome?.ifBlank { null } ?: usuarioLogado.username
                preferences[PAPEL_ATUAL] = usuarioLogado.papel
                preferences[CARGO_ATUAL] = perfil?.cargo.orEmpty()
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
            funcionarioRepository.sincronizar()
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