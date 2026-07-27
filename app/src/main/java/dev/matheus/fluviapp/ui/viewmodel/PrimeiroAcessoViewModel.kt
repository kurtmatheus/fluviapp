package dev.matheus.fluviapp.ui.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.model.operacoes.Funcionario
import dev.matheus.fluviapp.model.operacoes.Usuario
import dev.matheus.fluviapp.navigation.destinations.ARG_EMAIL_PRIMEIRO_ACESSO
import dev.matheus.fluviapp.services.repository.firebase.autenticacao.AutenticacaoRepository
import dev.matheus.fluviapp.services.repository.firebase.autenticacao.ResultadoAutenticacao
import dev.matheus.fluviapp.services.repository.operacoes.FuncionarioRepository
import dev.matheus.fluviapp.ui.states.PrimeiroAcessoUiState
import dev.matheus.fluviapp.ui.viewmodel.helpers.login.mapearMensagemErroAuth
import dev.matheus.fluviapp.ui.viewmodel.helpers.login.validarPrimeiroAcesso
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Primeiro acesso (ADR-0015 §2.1). A pessoa chega aqui **já autenticada** com a senha padrão; o que
 * falta é (a) uma senha que só ela saiba e (b) o perfil `users/{uid}`.
 *
 * A tela recebe o **e-mail** e resolve o funcionário sozinha, em vez de receber o id pronto do login:
 * é uma leitura a mais num momento raro, e em troca a tela sobrevive à morte do processo sem depender
 * de estado carregado por outro ViewModel.
 *
 * A ordem das duas escritas importa e não é intercambiável: **troca a senha primeiro, cria o perfil
 * depois**. Se o perfil nascesse antes e a troca falhasse, o login seguinte não seria mais detectado
 * como primeiro acesso — e a pessoa ficaria presa à senha compartilhada, sem tela que a deixe trocar.
 * Na ordem escolhida, o pior caso é reparável: a senha já é dela, e o primeiro acesso acontece de novo.
 */
@HiltViewModel
class PrimeiroAcessoViewModel @Inject constructor(
    private val autenticacaoRepository: AutenticacaoRepository,
    private val funcionarioRepository: FuncionarioRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val email: String = savedStateHandle.get<String>(ARG_EMAIL_PRIMEIRO_ACESSO).orEmpty()

    private var funcionario: Funcionario? = null

    private val _uiState = MutableStateFlow(PrimeiroAcessoUiState())
    val uiState: StateFlow<PrimeiroAcessoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            funcionario = funcionarioRepository.obterPorEmailDoServidor(email)
            _uiState.update { it.copy(nome = funcionario?.descricaoNome.orEmpty()) }
        }
    }

    fun onSenhaChange(v: String) = _uiState.update { it.copy(senha = v, isSenhaError = false, mensagemErro = 0) }

    fun onConfirmacaoChange(v: String) =
        _uiState.update { it.copy(confirmacao = v, isConfirmacaoError = false, mensagemErro = 0) }

    fun alternarVisibilidadeSenha() = _uiState.update { it.copy(isSenhaVisible = !it.isSenhaVisible) }

    fun confirmar() {
        val erros = validarPrimeiroAcesso(_uiState.value)
        if (!erros.valido) {
            _uiState.update {
                it.copy(
                    isSenhaError = erros.senha,
                    isConfirmacaoError = erros.confirmacao,
                    mensagemErro = if (erros.senha) R.string.error_senha_curta else R.string.error_senhas_diferentes,
                )
            }
            return
        }

        val vinculo = funcionario
        if (vinculo == null) {
            // Sem funcionário não há perfil a criar — e trocar a senha sozinha deixaria a pessoa numa
            // conta órfã. Melhor não começar.
            _uiState.update { it.copy(mensagemErro = R.string.error_sem_cadastro_na_equipe) }
            return
        }

        _uiState.update { it.copy(processando = true, mensagemErro = 0) }
        viewModelScope.launch {
            when (val resultado = autenticacaoRepository.alterarSenha(_uiState.value.senha)) {
                is ResultadoAutenticacao.Sucesso -> nascerPerfil(vinculo)
                is ResultadoAutenticacao.Falha -> {
                    Log.e(TAG, "alterarSenha falhou: ${resultado.motivo}")
                    _uiState.update {
                        it.copy(processando = false, mensagemErro = mapearMensagemErroAuth(resultado.motivo))
                    }
                }
            }
        }
    }

    /**
     * O perfil nasce **do registro do funcionário**, não de um formulário: papel `OPERADOR` (o coringa
     * de quem opera) e o vínculo pelo id — o e-mail casou as duas frentes, o id assume daqui em diante
     * (§8.3). O `username` sai da parte local do e-mail: a pessoa nunca escolheu um, e inventar um
     * campo aqui só atrasaria o acesso dela.
     *
     * A regra self-create do ADR-0011 continua intacta: neste ponto ela já está autenticada como ela
     * mesma, então `request.auth.uid == uid` vale sem exceção nenhuma no servidor.
     */
    private suspend fun nascerPerfil(funcionario: Funcionario) {
        try {
            autenticacaoRepository.criarPerfil(
                email = email,
                username = email.substringBefore('@'),
                papel = Usuario.Papel.OPERADOR.name,
                funcionarioId = funcionario.id,
            )
            // Sai da sessão: a senha nova só se prova no login seguinte, e é ele que abre a sessão pelo
            // caminho normal (perfil existente) — o passo de confirmação que o §2.1 pede.
            autenticacaoRepository.sair()
            _uiState.update { it.copy(processando = false, concluido = true) }
        } catch (e: Exception) {
            Log.e(TAG, "criarPerfil falhou: ${e.message}", e)
            _uiState.update { it.copy(processando = false, mensagemErro = R.string.error_falha_auth) }
        }
    }

    private companion object {
        const val TAG = "primeiroAcessoViewModel"
    }
}
