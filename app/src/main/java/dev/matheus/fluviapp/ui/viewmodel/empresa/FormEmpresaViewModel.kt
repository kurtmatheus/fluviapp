package dev.matheus.fluviapp.ui.viewmodel.empresa

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.operacoes.Atuacao
import dev.matheus.fluviapp.domain.viagem.AtuacaoDaEmpresa
import dev.matheus.fluviapp.domain.viagem.Empresa
import dev.matheus.fluviapp.domain.viagem.de
import dev.matheus.fluviapp.navigation.navcomposables.empresa.ID_EMPRESA_ARGUMENT
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmbarcacaoRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaRepository
import dev.matheus.fluviapp.services.repository.cadastro.localidade.LocalidadeRepository
import dev.matheus.fluviapp.services.repository.cadastro.porto.PortoRepository
import dev.matheus.fluviapp.ui.states.FormEmpresaUiState
import dev.matheus.fluviapp.ui.states.PortoOpcao
import dev.matheus.fluviapp.ui.viewmodel.helpers.empresa.validarEmpresa
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
 * Cadastro/edição de empresa no molde refatorado (cadastro-modulos §7.2): VM dona do estado;
 * eventos são métodos (sem lambdas no state); validação pura; sucesso via evento one-shot
 * (consumido por LaunchedEffect na navegação). Sem Context, sem navegar-no-finally, sem runBlocking.
 *
 * Edita também a **concessão** (ADR-0016 §7.1), que desde a F7 tem **duas dimensões**: *em quê* — quais
 * embarcações esta parte pode vender — e *onde* — em quais portos ela pode operar. Por isso conhece os
 * repositórios de embarcação e de porto: não para cadastrá-los, mas para oferecer os candidatos.
 *
 * É a segunda dimensão que faz a linha ofertável deixar de ser concedida diretamente e virar
 * **consequência**: quem tem os dois portos pode ofertar a travessia entre eles; quem não tem, não pode.
 */
@HiltViewModel
class FormEmpresaViewModel @Inject constructor(
    private val empresaRepository: EmpresaRepository,
    private val embarcacaoRepository: EmbarcacaoRepository,
    private val portoRepository: PortoRepository,
    private val localidadeRepository: LocalidadeRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // "" = criação; id preenchido = edição (arg de rota opcional, sem sentinela "null").
    private val idEmpresa: String = savedStateHandle.get<String>(ID_EMPRESA_ARGUMENT).orEmpty()

    private val _uiState = MutableStateFlow(FormEmpresaUiState())
    val uiState: StateFlow<FormEmpresaUiState> = _uiState.asStateFlow()

    private val _sucesso = Channel<Unit>(Channel.BUFFERED)
    val sucesso = _sucesso.receiveAsFlow()

    init {
        // Sequenciado numa corrotina só: a edição só faz sentido depois que a frota chegou, senão a
        // concessão gravada apareceria como uma lista de ids sem nome ao lado.
        viewModelScope.launch {
            carregarFrota()
            carregarPortos()
            if (idEmpresa.isNotBlank()) carregar()
        }
    }

    private suspend fun carregarFrota() {
        _uiState.update { it.copy(embarcacoes = embarcacaoRepository.obterTodos()) }
    }

    /**
     * Os portos ativos — a outra metade da concessão (F7). O rótulo traz a cidade porque é ela que
     * distingue homônimos: conceder "Porto Central" sem saber qual é conceder no escuro.
     */
    private suspend fun carregarPortos() {
        val localidades = localidadeRepository.obterTodas().associate { it.id to it.rotulo }
        val opcoes = portoRepository.obterTodos()
            .filter { it.ativo }
            .map { porto ->
                PortoOpcao(
                    id = porto.id,
                    rotulo = listOfNotNull(porto.nome, localidades[porto.localidadeId])
                        .filter { texto -> texto.isNotBlank() }
                        .joinToString(" · "),
                )
            }
            .sortedBy { it.rotulo }

        _uiState.update { it.copy(portos = opcoes) }
    }

    /** Concede ou revoga **um** porto — mesmo gesto do toggle da embarcação, no outro eixo. */
    fun onPortoToggle(portoId: String) = _uiState.update { estado ->
        val concedidos = if (portoId in estado.portosConcedidos) {
            estado.portosConcedidos - portoId
        } else {
            estado.portosConcedidos + portoId
        }
        estado.copy(portosConcedidos = concedidos)
    }

    fun onNomeChange(v: String) = _uiState.update { it.copy(nome = v, isNomeError = false) }
    fun onRazaoSocialChange(v: String) = _uiState.update { it.copy(razaoSocial = v, isRazaoSocialError = false) }
    fun onCnpjChange(v: String) = _uiState.update { it.copy(cnpj = v.filter(Char::isDigit).take(14), isCnpjError = false) }
    fun onEnderecoChange(v: String) = _uiState.update { it.copy(endereco = v) }
    fun onTelefone1Change(v: String) = _uiState.update { it.copy(telefone1 = v) }
    fun onTelefone2Change(v: String) = _uiState.update { it.copy(telefone2 = v) }

    /**
     * Liga/desliga uma atuação. Marcar e desmarcar são o mesmo gesto: o conjunto muda nos dois sentidos.
     *
     * Deixar de agenciar **apaga a concessão do estado**, e não por limpeza: salvar sem a atuação já
     * apaga o documento inteiro (`salvarAtuacoes` remove o que não foi desejado), então guardar os ids
     * seria manter na memória uma promessa que a próxima escrita desfaz. Mesma razão de o tipo da
     * embarcação zerar a capacidade de veículo — o estado tem de ser igual ao que a pessoa vê.
     */
    fun onAtuacaoToggle(atuacao: Atuacao) = _uiState.update { estado ->
        val atuacoes = if (atuacao in estado.atuacoes) {
            estado.atuacoes - atuacao
        } else {
            estado.atuacoes + atuacao
        }
        estado.copy(
            atuacoes = atuacoes,
            isAtuacoesError = false,
            embarcacoesConcedidas = if (Atuacao.AGENCIAMENTO in atuacoes) estado.embarcacoesConcedidas else emptySet(),
            portosConcedidos = if (Atuacao.AGENCIAMENTO in atuacoes) estado.portosConcedidos else emptySet(),
        )
    }

    /**
     * Concede ou revoga **uma** embarcação. Revogar é o mesmo gesto que conceder, como nas atuações — e
     * aqui isso importa mais: concessão é allow-list de segurança (ADR-0016 §7.1), e uma lista da qual
     * não se consegue tirar nada não é uma lista de permissão, é um caminho de mão única.
     */
    fun onEmbarcacaoToggle(embarcacaoId: String) = _uiState.update { estado ->
        val concedidas = if (embarcacaoId in estado.embarcacoesConcedidas) {
            estado.embarcacoesConcedidas - embarcacaoId
        } else {
            estado.embarcacoesConcedidas + embarcacaoId
        }
        estado.copy(embarcacoesConcedidas = concedidas)
    }

    private suspend fun carregar() {
        empresaRepository.obterPorId(idEmpresa)?.let { empresa ->
            val atuacoesGravadas = empresaRepository.obterAtuacoes(idEmpresa)
            _uiState.update {
                it.copy(
                    titulo = R.string.subtitle_editar_empresa,
                    nome = empresa.nome,
                    razaoSocial = empresa.razaoSocial,
                    cnpj = empresa.cnpj.filter(Char::isDigit).take(14),
                    endereco = empresa.endereco,
                    telefone1 = empresa.telefone1,
                    telefone2 = empresa.telefone2,
                    atuacoes = atuacoesGravadas.map { gravada -> gravada.atuacao }.toSet(),
                    embarcacoesConcedidas = atuacoesGravadas.de(Atuacao.AGENCIAMENTO)?.embarcacaoIds.orEmpty(),
                    portosConcedidos = atuacoesGravadas.de(Atuacao.AGENCIAMENTO)?.portoIds.orEmpty(),
                )
            }
        }
    }

    fun salvar() {
        val erros = validarEmpresa(_uiState.value)
        if (!erros.valido) {
            _uiState.update {
                it.copy(
                    isNomeError = erros.nome,
                    isRazaoSocialError = erros.razaoSocial,
                    isCnpjError = erros.cnpj,
                    isAtuacoesError = erros.atuacoes,
                )
            }
            return
        }

        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            val s = _uiState.value
            try {
                // A parte primeiro: é ela que gera o id, e as atuações penduram nele (ADR-0016 §4).
                // A ordem não é preferência — sem id não há subcoleção onde escrever.
                val id = empresaRepository.salvar(
                    Empresa(
                        id = idEmpresa,
                        nome = s.nome,
                        razaoSocial = s.razaoSocial,
                        cnpj = s.cnpj,
                        endereco = s.endereco,
                        telefone1 = s.telefone1,
                        telefone2 = s.telefone2,
                    )
                )
                // A concessão do AGENCIAMENTO vem do estado — é o que esta tela agora edita. As demais
                // atuações continuam sendo **preservadas** a partir do que está gravado: elas não têm
                // editor, e salvar a empresa não pode apagar em silêncio o que este form não mostra.
                val gravadas = empresaRepository.obterAtuacoes(id).associateBy { it.atuacao }
                empresaRepository.salvarAtuacoes(
                    empresaId = id,
                    atuacoes = s.atuacoes.map { atuacao ->
                        if (atuacao == Atuacao.AGENCIAMENTO) {
                            AtuacaoDaEmpresa(
                                atuacao = atuacao,
                                embarcacaoIds = s.embarcacoesConcedidas,
                                portoIds = s.portosConcedidos,
                            )
                        } else {
                            gravadas[atuacao] ?: AtuacaoDaEmpresa(atuacao)
                        }
                    },
                )
                _sucesso.send(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "salvar: ${e.message}", e)
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    private companion object {
        const val TAG = "formEmpresaViewModel"
    }
}
