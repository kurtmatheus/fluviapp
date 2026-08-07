package dev.matheus.fluviapp.ui.viewmodel.funcionario

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.operacoes.Funcionario
import dev.matheus.fluviapp.domain.operacoes.PermissoesUsuario
import dev.matheus.fluviapp.domain.operacoes.Vinculo
import dev.matheus.fluviapp.navigation.navcomposables.funcionario.ID_FUNCIONARIO_ARGUMENT
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaRepository
import dev.matheus.fluviapp.services.repository.operacoes.FuncionarioRepository
import dev.matheus.fluviapp.services.repository.operacoes.SessaoUsuario
import dev.matheus.fluviapp.ui.states.EmpresaOpcao
import dev.matheus.fluviapp.ui.states.FormFuncionarioUiState
import dev.matheus.fluviapp.ui.viewmodel.helpers.funcionario.validarFuncionario
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Cadastro/edição de membro da equipe no molde do ADR-0006, agora **editando vínculos** (ADR-0016 §6).
 *
 * O form continua tendo **dois recortes** (ADR-0015 §2.1/§8.5), com a coordenada trocada: a plataforma
 * escolhe empresa e cargo; o supervisor cadastra na **empresa dele**, sem tocar em cargo. O que mudou é
 * que "a dele" deixou de ser uma String de agência e passou a ser o `empresaId` do vínculo ativo.
 *
 * ### A ponte que este VM ainda carrega
 *
 * `Funcionario.agencia` continua sendo gravada — derivada do **nome da empresa do primeiro vínculo** —
 * porque a Passagem imprime esse campo no bilhete e ainda não foi revitalizada (F9). Sem a ponte, todo
 * membro novo emitiria bilhete com a agência em branco: uma regressão visível, causada por uma fatia que
 * não era da emissão. Ela sai na F6.5, junto com o campo.
 */
@HiltViewModel
class FormFuncionarioViewModel @Inject constructor(
    private val funcionarioRepository: FuncionarioRepository,
    private val empresaRepository: EmpresaRepository,
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
            val contexto = sessaoUsuario.atual()
            aplicarRecorte(contexto)
            carregarEmpresas(contexto)
            if (idFuncionario.isNotBlank()) carregar()
        }
    }

    /**
     * O recorte vem ANTES da carga: é ele que decide se a lista de empresas é o universo ou uma só. Sem
     * contexto (sessão ausente) o form nasce fechado, que é o fail-closed coerente com o resto da
     * política.
     */
    private fun aplicarRecorte(contexto: dev.matheus.fluviapp.domain.operacoes.ContextoUsuario?) {
        val podeEscolherEmpresa = PermissoesUsuario.podeEscolherAgencia(contexto?.papel)
        val podeDefinirCargo = PermissoesUsuario.podeDefinirCargo(contexto?.papel)
        _uiState.update {
            it.copy(
                podeEscolherEmpresa = podeEscolherEmpresa,
                podeDefinirCargo = podeDefinirCargo,
                listaCargo = if (podeDefinirCargo) Funcionario.Cargo.entries.map(Funcionario.Cargo::name) else emptyList(),
            )
        }
    }

    /**
     * As empresas que este cadastrante pode atribuir. Para a plataforma, todas; para o supervisor, **só a
     * dele** — e já escolhida, porque uma lista de um item é uma pergunta sem alternativa.
     */
    private suspend fun carregarEmpresas(contexto: dev.matheus.fluviapp.domain.operacoes.ContextoUsuario?) {
        val todas = empresaRepository.obterTodas().map { EmpresaOpcao(id = it.id, nome = it.nome) }
        val minha = contexto?.vinculoAtivo?.empresaId

        val disponiveis = if (_uiState.value.podeEscolherEmpresa) todas else todas.filter { it.id == minha }

        _uiState.update {
            it.copy(
                empresas = disponiveis,
                empresaEmEdicao = if (it.podeEscolherEmpresa) it.empresaEmEdicao else disponiveis.firstOrNull()?.nome.orEmpty(),
            )
        }
    }

    private suspend fun carregar() {
        funcionarioRepository.obterPorId(idFuncionario)?.let { funcionario ->
            _uiState.update {
                it.copy(
                    titulo = R.string.subtitle_editar_agente,
                    nome = funcionario.descricaoNome,
                    email = funcionario.email,
                    // Os vínculos gravados prevalecem sobre qualquer recorte: o supervisor só alcança
                    // quem é da empresa dele (o recorte da lista, §2.2), então preservar o que está
                    // gravado não abre porta — evita reescrever o vínculo alheio de quem serve a duas.
                    vinculos = funcionario.vinculos,
                )
            }
        }
    }

    fun onNomeChange(v: String) = _uiState.update { it.copy(nome = v, isNomeError = false) }
    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v, isEmailError = false) }

    /** A empresa do vínculo **em montagem** — só muda para quem pode escolher. */
    fun onEmpresaChange(v: String) = _uiState.update {
        if (it.podeEscolherEmpresa) it.copy(empresaEmEdicao = v) else it
    }

    /** Só tem efeito para quem pode definir cargo — a tela nem desenha o seletor para os demais. */
    fun onCargoChange(v: String) = _uiState.update {
        if (it.podeDefinirCargo) it.copy(cargoEmEdicao = v) else it
    }

    /**
     * Acrescenta o vínculo em montagem à lista.
     *
     * **Substitui em vez de duplicar** quando a empresa já está na lista: dois vínculos na mesma empresa
     * não significam nada — o segundo só poderia contradizer o cargo do primeiro. Trocar o cargo passa a
     * ser reatribuir, que é o gesto que a pessoa tem em mente.
     */
    fun onAdicionarVinculo() = _uiState.update { estado ->
        val empresa = estado.empresas.firstOrNull { it.nome == estado.empresaEmEdicao } ?: return@update estado
        val cargo = if (estado.podeDefinirCargo) estado.cargoEmEdicao else Funcionario.Cargo.AGENTE.name
        val novo = Vinculo.de(empresa.id, cargo) ?: return@update estado

        estado.copy(
            vinculos = estado.vinculos.filterNot { it.empresaId == novo.empresaId } + novo,
            isVinculosError = false,
        )
    }

    fun onRemoverVinculo(empresaId: String) = _uiState.update {
        it.copy(vinculos = it.vinculos.filterNot { vinculo -> vinculo.empresaId == empresaId })
    }

    fun salvar() {
        val estado = _uiState.value
        val erros = validarFuncionario(estado)
        if (!erros.valido) {
            _uiState.update {
                it.copy(
                    isNomeError = erros.nome,
                    isEmailError = erros.email,
                    isVinculosError = erros.vinculos,
                )
            }
            return
        }

        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            try {
                // Edição parte do gravado (preserva id e o que esta tela não edita); criação parte do
                // zero, com id vazio → auto-id no repositório.
                val base = if (idFuncionario.isNotBlank()) funcionarioRepository.obterPorId(idFuncionario) else null
                val funcionario = (base ?: Funcionario(id = "", descricaoNome = "", agencia = "")).copy(
                    descricaoNome = estado.nome,
                    email = estado.email.trim(),
                    vinculos = estado.vinculos,
                    // A ponte para o bilhete (F6.5): o legado passa a espelhar a empresa do primeiro
                    // vínculo, em vez de ficar preso ao que alguém digitou antes.
                    agencia = estado.vinculosNaTela.firstOrNull()?.empresa.orEmpty(),
                    // O cargo legado acompanha o primeiro vínculo pelo mesmo motivo — quem ainda o lê é
                    // a política, enquanto ela não perguntar pelo vínculo (F6.5).
                    cargo = estado.vinculos.firstOrNull()?.cargo?.name ?: Funcionario.Cargo.AGENTE.name,
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