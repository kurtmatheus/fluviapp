package dev.matheus.fluviapp.ui.viewmodel.funcionario

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Categoria.MUNICIPIO
import dev.matheus.fluviapp.model.operacoes.Funcionario
import dev.matheus.fluviapp.model.operacoes.PermissoesUsuario
import dev.matheus.fluviapp.model.mapDescricao
import dev.matheus.fluviapp.navigation.navcomposables.funcionario.ID_FUNCIONARIO_ARGUMENT
import dev.matheus.fluviapp.services.repository.cadastro.ConstanteRepository
import dev.matheus.fluviapp.services.repository.operacoes.FuncionarioRepository
import dev.matheus.fluviapp.services.repository.operacoes.SessaoUsuario
import dev.matheus.fluviapp.ui.states.FormFuncionarioUiState
import dev.matheus.fluviapp.ui.viewmodel.helpers.funcionario.validarFuncionario
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Cadastro/edição de funcionário no molde refatorado (cadastro-modulos §7.2): VM dona do estado (sem
 * FormHelper); eventos são métodos; validação pura; sucesso via evento one-shot; cargas suspensas
 * (sem runBlocking); arg de rota opcional; sem Context. A BUSCA vive em PesquisaFuncionarioViewModel.
 *
 * O form tem **dois recortes** (ADR-0015 §2.1/§8.5), resolvidos aqui e entregues à tela como flags:
 * plataforma escolhe agência e cargo; supervisor cadastra na **própria** agência, sem tocar em cargo.
 */
@HiltViewModel
class FormFuncionarioViewModel @Inject constructor(
    private val funcionarioRepository: FuncionarioRepository,
    private val constanteRepository: ConstanteRepository,
    private val sessaoUsuario: SessaoUsuario,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val idFuncionario: String = savedStateHandle.get<String>(ID_FUNCIONARIO_ARGUMENT).orEmpty()

    private val _uiState = MutableStateFlow(FormFuncionarioUiState())
    val uiState: StateFlow<FormFuncionarioUiState> = _uiState.asStateFlow()

    private val _sucesso = Channel<Unit>(Channel.BUFFERED)
    val sucesso = _sucesso.receiveAsFlow()

    init {
        viewModelScope.launch {
            aplicarRecorte()
            carregarFontes()
            if (idFuncionario.isNotBlank()) carregar()
        }
    }

    /**
     * O recorte vem ANTES da carga: é ele que decide se a agência é escolhida ou herdada. Sem contexto
     * (sessão ausente) o form nasce fechado — sem seletor de agência nem de cargo —, que é o
     * fail-closed coerente com o resto da política.
     */
    private suspend fun aplicarRecorte() {
        val contexto = sessaoUsuario.atual()
        val podeEscolherAgencia = PermissoesUsuario.podeEscolherAgencia(contexto?.papel)
        _uiState.update {
            it.copy(
                podeEscolherAgencia = podeEscolherAgencia,
                podeDefinirCargo = PermissoesUsuario.podeDefinirCargo(contexto?.papel),
                // Agência implícita do supervisor: a dele, sem seletor (§2.1).
                agencia = if (podeEscolherAgencia) it.agencia else contexto?.agencia.orEmpty(),
            )
        }
    }

    private suspend fun carregarFontes() {
        val agencias = if (_uiState.value.podeEscolherAgencia) {
            funcionarioRepository.obterTodasAgencias()
        } else {
            emptyList()
        }
        _uiState.update {
            it.copy(
                listaAgencia = agencias,
                listaMunicipios = constanteRepository.obterTodosPorCategoria(MUNICIPIO.name).mapDescricao(),
                listaCargo = if (it.podeDefinirCargo) Funcionario.Cargo.entries.map(Funcionario.Cargo::name) else emptyList(),
            )
        }
    }

    private suspend fun carregar() {
        funcionarioRepository.obterPorId(idFuncionario)?.let { funcionario ->
            _uiState.update {
                it.copy(
                    titulo = R.string.subtitle_editar_agente,
                    // Na edição a agência do MEMBRO prevalece sobre a implícita — o supervisor só
                    // alcança membros da própria agência (o recorte da lista, §2.2), então isto não
                    // abre porta: preserva o que está gravado em vez de reescrever com a do editor.
                    agencia = funcionario.agencia,
                    funcionario = funcionario.descricaoNome,
                    email = funcionario.email,
                    lotacao = funcionario.lotacao,
                    cargo = funcionario.cargo,
                )
            }
        }
    }

    fun onAgenciaChange(v: String) = _uiState.update { it.copy(agencia = v, isAgenciaError = false) }
    fun onFuncionarioChange(v: String) = _uiState.update { it.copy(funcionario = v, isFuncionarioError = false) }
    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v, isEmailError = false) }
    fun onLotacaoChange(v: String) = _uiState.update { it.copy(lotacao = v, isLotacaoError = false) }

    /** Só tem efeito para quem pode definir cargo — a tela nem desenha o seletor para os demais. */
    fun onCargoChange(v: String) = _uiState.update {
        if (it.podeDefinirCargo) it.copy(cargo = v) else it
    }

    fun salvar() {
        val erros = validarFuncionario(_uiState.value)
        if (!erros.valido) {
            _uiState.update {
                it.copy(
                    isAgenciaError = erros.agencia,
                    isFuncionarioError = erros.funcionario,
                    isEmailError = erros.email,
                    isLotacaoError = erros.lotacao,
                )
            }
            return
        }

        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            val s = _uiState.value
            try {
                // Edição: parte do funcionário persistido (preserva o id) e aplica os campos do form.
                // Criação: novo funcionário com id vazio (auto-id).
                val base = if (idFuncionario.isNotBlank()) funcionarioRepository.obterPorId(idFuncionario) else null
                val funcionario = (base ?: Funcionario(id = "", descricaoNome = "", agencia = "", lotacao = "")).copy(
                    descricaoNome = s.funcionario,
                    agencia = s.agencia,
                    email = s.email.trim(),
                    lotacao = s.lotacao,
                    // Quem não pode definir cargo não o altera nem por engano: preserva o gravado, e o
                    // novo nasce AGENTE (o default da entidade). A regra do servidor exige o mesmo.
                    cargo = if (s.podeDefinirCargo) s.cargo else base?.cargo ?: Funcionario.Cargo.AGENTE.name,
                )
                funcionarioRepository.salvar(funcionario)
                _sucesso.send(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "salvar: ${e.message}", e)
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    private companion object {
        const val TAG = "formFuncionarioViewModel"
    }
}