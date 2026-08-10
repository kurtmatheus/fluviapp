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
import dev.matheus.fluviapp.domain.operacoes.Usuario
import dev.matheus.fluviapp.preferences.PreferencesKey.CARGO_ATUAL
import dev.matheus.fluviapp.preferences.PreferencesKey.PAPEL_ATUAL
import dev.matheus.fluviapp.preferences.PreferencesKey.LOGADO
import dev.matheus.fluviapp.preferences.PreferencesKey.USUARIO_ATUAL
import dev.matheus.fluviapp.services.repository.operacoes.FuncionarioRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmbarcacaoRepository
import dev.matheus.fluviapp.services.repository.firebase.PassagemFirestoreRepository
import dev.matheus.fluviapp.services.repository.firebase.autenticacao.AutenticacaoRepository
import dev.matheus.fluviapp.services.repository.firebase.autenticacao.PerfilAutenticado
import dev.matheus.fluviapp.services.repository.firebase.autenticacao.ResultadoAutenticacao
import dev.matheus.fluviapp.services.repository.firebase.autenticacao.ResultadoPerfil
import dev.matheus.fluviapp.services.repository.firebase.autenticacao.toUsuario
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
    private val empresaRepository: EmpresaRepository,
    private val embarcacaoRepository: EmbarcacaoRepository,
    private val funcionarioRepository: FuncionarioRepository,
    private val passagemRepository: PassagemFirestoreRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState>
        get() = _uiState.asStateFlow()

    lateinit var loginFormHelper: LoginFormHelper

    lateinit var onNavegaParaMainScreen: () -> Unit

    /**
     * O `SeedFirestore` saiu daqui (ADR-0016: *o seed morre*, e quem alimenta o app é o painel).
     * Ele já era contraditório: prometia provisionar via "cadastro in-app com perfil auto-criado", um
     * mecanismo removido na P2.2c — e nunca criou `users`, de modo que a primeira conta sempre dependeu
     * do console. Mantê-lo era manter uma porta de escrita em massa que só rodava em debug, escondendo
     * essa dependência de ambiente em vez de mostrá-la.
     */
    init {
        viewModelScope.launch { initializeHelper() }
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
     * Autentica e decide entre QUATRO desfechos, nesta ordem (ADR-0015 §2.1):
     *
     *  1. **Tem perfil** (`users/{uid}`) → sessão aberta, caminho normal.
     *  2. **Não tem perfil, mas existe funcionário com este e-mail** → é o **primeiro acesso**. Não é um
     *     convite guardado em lugar nenhum: é uma *dedução* de dois fatos que já existem — a conta
     *     autenticou e o pré-cadastro está lá.
     *  3. **Não tem nem um nem outro** → autenticou, mas não é da casa. A conta existe no Auth e não
     *     tem registro na operação; quem resolve isso é a gestão, não o app.
     *  4. **Não deu para perguntar** → problema de conexão, e nada se conclui sobre o cadastro. Este
     *     desfecho *existia* antes escondido dentro do 3º: sem rede, o app dizia à pessoa que ela não
     *     era da casa e a deslogava.
     */
    private suspend fun autenticarUsuario() {
        val email = _uiState.value.email
        val senha = _uiState.value.senha

        when (val resultado = autenticacaoRepository.autenticar(email, senha)) {
            is ResultadoAutenticacao.Sucesso -> when (val perfil = autenticacaoRepository.perfilAutenticado()) {
                is ResultadoPerfil.Encontrado -> logarUsuario(perfil.perfil, email)

                is ResultadoPerfil.Ausente -> deduzirPrimeiroAcesso(email)

                is ResultadoPerfil.Indisponivel -> {
                    // A credencial continua válida: não se desloga por falha de rede.
                    Log.e(TAG, "perfil indisponível (sem rede?) para $email")
                    loginFormHelper.exibeErro()
                    loginFormHelper.setMensagemErro(R.string.error_perfil_indisponivel)
                    _uiState.value = _uiState.value.copy(logando = false)
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
     * funcionário ligado. Ambos saem do [perfil] que a porta de autenticação leu **do servidor**.
     *
     * O espelho Room é escrito aqui, e não consultado: antes o login procurava o autenticado num espelho
     * que um listener pré-login populava — e esse listener é negado pela regra `allow read: if
     * autenticado()`. O login passou a *ser a origem* do espelho em vez de depender dele, e é isso que a
     * [SessaoUsuario] lê depois.
     *
     * O que se exibe é o **nome do funcionário**; sem funcionário (papel puro de plataforma), o
     * `username` — o `Usuario` não tem nome (§8.1).
     */
    private suspend fun logarUsuario(perfil: PerfilAutenticado, emailAutenticado: String) {
        val usuario = usuarioRepository.registrarLogin(perfil.toUsuario(emailAutenticado))
        dataStore.edit { preferences ->
            preferences[LOGADO] = true
            preferences[USUARIO_ATUAL] = perfil.nome.ifBlank { usuario.username }
            preferences[PAPEL_ATUAL] = usuario.papel
            preferences[CARGO_ATUAL] = perfil.cargo
        }
        _uiState.value = _uiState.value.copy(logado = true)
    }

    /**
     * Liga os listeners das coleções ao entrar.
     *
     * O `constanteRepository` **saiu daqui** (ADR-0020 F2): era a última referência ao `Constante` no
     * caminho vivo do app, e servia só para aquecer um cache que ninguém mais lê — os quatro leitores que
     * restam estão em Viagem e Equipe, que o menu não alcança. Um listener anexado a cada login numa
     * coleção sem leitor é custo sem contrapartida, e some sem que nada em tela mude.
     *
     * A `viagens` **saiu na F8.0**, pela mesma razão e um passo adiante: a coleção que ela aquecia é a do
     * trecho disfarçado, que deixou de ter entidade. A `Viagem` da F8 ligará o próprio listener na tela
     * dela, como fazem a Rota, o Porto e a Localidade.
     *
     * A `localidades` **não entra**: quem a observa é o ViewModel da própria seção, que liga o listener ao
     * abrir a busca. Sincronizar tudo no login é o hábito antigo, de quando o Room precisava estar cheio
     * antes de alguém olhar — com o cache do SDK, cada tela liga o que usa (ADR-0017 D1).
     */
    fun sincronizar(context: Context) {
        try {
            funcionarioRepository.sincronizar()
            empresaRepository.sincronizar()
            embarcacaoRepository.sincronizar()
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