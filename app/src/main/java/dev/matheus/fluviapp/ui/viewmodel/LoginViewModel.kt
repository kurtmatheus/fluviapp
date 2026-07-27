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

    /**
     * Autentica e decide entre TRÊS desfechos, nesta ordem (ADR-0015 §2.1):
     *
     *  1. **Tem perfil** (`users/{uid}`) → sessão aberta, caminho normal.
     *  2. **Não tem perfil, mas existe funcionário com este e-mail** → é o **primeiro acesso**. Não é um
     *     convite guardado em lugar nenhum: é uma *dedução* de dois fatos que já existem — a conta
     *     autenticou e o pré-cadastro está lá.
     *  3. **Não tem nem um nem outro** → autenticou, mas não é da casa. A conta existe no Auth e não
     *     tem registro na operação; quem resolve isso é a gestão, não o app.
     */
    private suspend fun autenticarUsuario() {
        val email = _uiState.value.email
        val senha = _uiState.value.senha

        when (val resultado = autenticacaoRepository.autenticar(email, senha)) {
            is ResultadoAutenticacao.Sucesso -> {
                val perfil = autenticacaoRepository.perfilAutenticado()
                if (perfil != null) {
                    logarUsuario(usuarioRepository.salvarUsuarioAutenticado(email), perfil)
                } else {
                    deduzirPrimeiroAcesso(email)
                }
            }

            is ResultadoAutenticacao.Falha -> {
                loginFormHelper.exibeErro()
                loginFormHelper.setMensagemErro(mapearMensagemErroAuth(resultado.motivo))
                _uiState.value = _uiState.value.copy(logando = false)
            }
        }
    }

    private suspend fun deduzirPrimeiroAcesso(email: String) {
        val funcionario = funcionarioRepository.obterPorEmailDoServidor(email)
        if (funcionario == null) {
            // Autenticou e não é da casa: sai da sessão para não deixar credencial válida sem perfil.
            Log.e(TAG, "autenticado sem perfil e sem funcionário para $email")
            autenticacaoRepository.sair()
            loginFormHelper.exibeErro()
            loginFormHelper.setMensagemErro(R.string.error_sem_cadastro_na_equipe)
            _uiState.value = _uiState.value.copy(logando = false)
            return
        }
        _uiState.value = _uiState.value.copy(logando = false, primeiroAcessoEmail = email)
    }

    /** Consome o sinal depois de navegar — senão a volta para o login reabriria a tela de senha. */
    fun primeiroAcessoConsumido() {
        _uiState.update { it.copy(primeiroAcessoEmail = null) }
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