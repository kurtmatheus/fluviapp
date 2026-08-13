package dev.matheus.fluviapp.ui.viewmodel.passagem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.matheus.fluviapp.domain.passagem.CategoriaPassagem
import dev.matheus.fluviapp.domain.passagem.Lancamento
import dev.matheus.fluviapp.domain.passagem.MetadadosPassagem
import dev.matheus.fluviapp.domain.passagem.Passagem
import dev.matheus.fluviapp.domain.passagem.PassagemDePassageiro
import dev.matheus.fluviapp.domain.passagem.PassagemDeVeiculo
import dev.matheus.fluviapp.domain.passagem.ResultadoEmissao
import dev.matheus.fluviapp.domain.passagem.StatusPassagem
import dev.matheus.fluviapp.domain.passagem.TipoPassagem
import dev.matheus.fluviapp.domain.passagem.avaliarEmissao
import dev.matheus.fluviapp.domain.viagem.OcorrenciaViagem
import dev.matheus.fluviapp.services.repository.operacoes.SessaoUsuario
import dev.matheus.fluviapp.services.repository.passagem.CriterioPassagem
import dev.matheus.fluviapp.services.repository.passagem.PassagemRepository
import dev.matheus.fluviapp.services.repository.passagem.RecorteTemporal
import dev.matheus.fluviapp.services.repository.pool.ClienteRepository
import dev.matheus.fluviapp.services.repository.pool.VeiculoRepository
import dev.matheus.fluviapp.domain.operacoes.PermissoesUsuario.EscopoEmpresa
import dev.matheus.fluviapp.ui.states.passagem.BilheteEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.ClienteEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.EmissaoUiState
import dev.matheus.fluviapp.ui.states.passagem.PagamentoEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.ParticipanteEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.PassoEmissao
import dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.validarPasso
import dev.matheus.fluviapp.util.Relogio
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

/**
 * **O ViewModel da emissão — único escritor do estado** ([ADR-0026] D1/D3, [ADR-0028] D4).
 *
 * Ele substitui um arranjo em que três *helpers* recebiam o `MutableStateFlow` e escreviam nele, mais um
 * `internal lateinit` exposto para a navegação alcançar. Aqui:
 *
 * - **quem escreve é este objeto**, e as classes auxiliares viraram **funções puras** (validação, conversão);
 * - **a sequência da emissão mora aqui**, não na navegação: a tela reage ao [eventos], e não orquestra;
 * - **não há `Context`**: nada nesta classe conhece Android além do `ViewModel`.
 *
 * ### A sequência, e por que ela é nesta ordem
 *
 * 1. **validar o passo** (puro);
 * 2. **registrar os participantes no pool** — é o passo que **exige rede** ([ADR-0025] D6), e por isso vem
 *    antes de reservar número: falhar aqui não pode ter consumido um número da sequência;
 * 3. **avaliar as guardas** (coerência + cota), com a contagem lida do servidor;
 * 4. **reservar o número** — atômico, e é o ponto sem volta;
 * 5. **emitir**.
 *
 * ### Tolerância a falha ([ADR-0028] D3)
 *
 * Falhar em qualquer ponto emite [EventoDeEmissao.Falhou] e **não limpa nada**: o atendimento inteiro
 * permanece na tela para o operador tentar de novo. *"Deve ser tolerante a falha"* não é emitir assim mesmo —
 * é não fazer o operador digitar tudo outra vez porque a rede caiu no meio.
 */
@HiltViewModel
class EmissaoViewModel @Inject constructor(
    private val passagemRepository: PassagemRepository,
    private val clienteRepository: ClienteRepository,
    private val veiculoRepository: VeiculoRepository,
    private val sessaoUsuario: SessaoUsuario,
    private val relogio: Relogio,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmissaoUiState())
    val uiState: StateFlow<EmissaoUiState> = _uiState.asStateFlow()

    /**
     * Desfechos **one-shot**: acontecem uma vez e não são estado.
     *
     * `Channel` e não `StateFlow` pela razão de sempre: um estado de "emitida" seria reentregue na virada de
     * tela e navegaria de novo — o defeito clássico de tratar evento como estado.
     */
    private val _eventos = Channel<EventoDeEmissao>(Channel.BUFFERED)
    val eventos: Flow<EventoDeEmissao> = _eventos.receiveAsFlow()

    /** A saída vem **pronta** do card de Início: a emissão não pergunta data nem hora ([ADR-0028] D5). */
    private var ocorrencia: OcorrenciaViagem? = null

    fun iniciar(ocorrencia: OcorrenciaViagem, cabecalho: dev.matheus.fluviapp.ui.states.passagem.CabecalhoDaViagem) {
        this.ocorrencia = ocorrencia
        _uiState.update { EmissaoUiState(cabecalho = cabecalho) }
    }

    // --- Passo 1: o bilhete ---

    /**
     * Trocar a categoria **troca o participante**, e é isso que apaga a limpeza reativa: o que era do outro
     * sub-domínio deixa de existir em vez de deixar de importar.
     */
    fun escolherCategoria(categoria: CategoriaPassagem) {
        _uiState.update {
            it.copy(
                bilhete = BilheteEmEdicao(categoria = categoria),
                participante = when (categoria) {
                    CategoriaPassagem.PASSAGEIRO -> ParticipanteEmEdicao.DePassageiro()
                    CategoriaPassagem.VEICULO -> ParticipanteEmEdicao.DeVeiculo()
                },
                erros = emptySet(),
            )
        }
    }

    /**
     * A acomodação **redimensiona o passo 2**: rede cabe uma pessoa, suíte e camarote até três. Trocar para
     * uma menor descarta as linhas que não cabem mais — e descartar é melhor do que carregar em silêncio uma
     * pessoa que o bilhete não admite.
     */
    fun escolherAcomodacao(acomodacao: dev.matheus.fluviapp.domain.passagem.Acomodacao) {
        _uiState.update { estado ->
            val bilhete = estado.bilhete.copy(
                acomodacao = acomodacao,
                // Fora da rede só existe inteira: manter "meia" escolhida antes viraria estado ilegal.
                tipo = if (acomodacao.temEscolhaDeTipo) estado.bilhete.tipo else TipoPassagem.INTEIRA,
                gratuidade = if (acomodacao.temEscolhaDeTipo) estado.bilhete.gratuidade else null,
            )
            estado.copy(
                bilhete = bilhete,
                participante = redimensionar(estado.participante, acomodacao.ocupacaoMaxima),
                erros = emptySet(),
            )
        }
    }

    fun escolherTipo(tipo: TipoPassagem) {
        _uiState.update {
            it.copy(
                bilhete = it.bilhete.copy(
                    tipo = tipo,
                    gratuidade = if (tipo == TipoPassagem.GRATUIDADE) it.bilhete.gratuidade else null,
                ),
                erros = emptySet(),
            )
        }
    }

    fun escolherGratuidade(gratuidade: dev.matheus.fluviapp.domain.passagem.TipoGratuidade) {
        _uiState.update { it.copy(bilhete = it.bilhete.copy(gratuidade = gratuidade), erros = emptySet()) }
    }

    // --- Passo 2: quem viaja ---

    fun preencherPessoa(indice: Int, pessoa: ClienteEmEdicao) {
        _uiState.update { estado ->
            val atual = estado.participante as? ParticipanteEmEdicao.DePassageiro ?: return@update estado
            val pessoas = atual.pessoas.toMutableList()
            if (indice !in pessoas.indices) return@update estado
            pessoas[indice] = pessoa
            estado.copy(participante = atual.copy(pessoas = pessoas), erros = emptySet())
        }
    }

    /** Acrescenta uma linha de acompanhante, **até onde a acomodação admite**. */
    fun acrescentarAcompanhante() {
        _uiState.update { estado ->
            val atual = estado.participante as? ParticipanteEmEdicao.DePassageiro ?: return@update estado
            if (atual.pessoas.size >= estado.bilhete.ocupacaoMaxima) return@update estado
            estado.copy(participante = atual.copy(pessoas = atual.pessoas + ClienteEmEdicao()))
        }
    }

    fun removerAcompanhante(indice: Int) {
        _uiState.update { estado ->
            val atual = estado.participante as? ParticipanteEmEdicao.DePassageiro ?: return@update estado
            // O titular é a posição 0 e não se remove: sem ele não há bilhete de pessoa.
            if (indice <= 0 || indice !in atual.pessoas.indices) return@update estado
            estado.copy(participante = atual.copy(pessoas = atual.pessoas - atual.pessoas[indice]))
        }
    }

    fun preencherVeiculo(veiculo: dev.matheus.fluviapp.ui.states.passagem.VeiculoEmEdicao) {
        _uiState.update { estado ->
            val atual = estado.participante as? ParticipanteEmEdicao.DeVeiculo ?: return@update estado
            estado.copy(participante = atual.copy(veiculo = veiculo), erros = emptySet())
        }
    }

    /** `null` remove o responsável — e bilhete de veículo **sem ninguém nomeado é a forma normal**. */
    fun preencherResponsavel(responsavel: ClienteEmEdicao?) {
        _uiState.update { estado ->
            val atual = estado.participante as? ParticipanteEmEdicao.DeVeiculo ?: return@update estado
            estado.copy(participante = atual.copy(responsavel = responsavel), erros = emptySet())
        }
    }

    // --- Passo 3: o pagamento ---

    fun preencherPagamento(pagamento: PagamentoEmEdicao) {
        _uiState.update { it.copy(pagamento = pagamento, erros = emptySet()) }
    }

    // --- Navegação entre passos ---

    /** Avança **se o passo corrente fecha**; no último, emite. */
    fun avancar() {
        val estado = _uiState.value
        val erros = validarPasso(estado.passo, estado.bilhete, estado.participante, estado.pagamento)
        if (erros.isNotEmpty()) {
            _uiState.update { it.copy(erros = erros) }
            return
        }
        if (estado.ehUltimoPasso) {
            emitir()
            return
        }
        _uiState.update { it.copy(passo = it.passo.proximo() ?: it.passo, erros = emptySet()) }
    }

    /** Voltar **não valida**: quem volta está corrigindo, e cobrar o passo na saída seria prendê-lo nele. */
    fun voltar() {
        _uiState.update { it.copy(passo = it.passo.anterior() ?: it.passo, erros = emptySet()) }
    }

    // --- A emissão ---

    private fun emitir() {
        val ocorrencia = ocorrencia ?: run {
            viewModelScope.launch { _eventos.send(EventoDeEmissao.Falhou(MotivoDeFalha.SEM_OCORRENCIA)) }
            return
        }
        _uiState.update { it.copy(emitindo = true) }

        viewModelScope.launch {
            // A agência do bilhete é a do **vínculo em vigor**, nunca digitada (ADR-0015 P2.3): quem tem um
            // vínculo só opera por ele; quem tem vários opera pelo escolhido. Sem vínculo não há emissão —
            // quem emite é da operação (§8.4), e um bilhete sem agência seria um bilhete sem dono.
            val contexto = sessaoUsuario.atual()
            val agenciaId = contexto?.vinculoAtivo?.empresaId
            if (contexto == null || agenciaId.isNullOrBlank()) {
                concluir(EventoDeEmissao.Falhou(MotivoDeFalha.SEM_VINCULO))
                return@launch
            }

            val estado = _uiState.value
            // 1. O pool primeiro, porque é ele que exige rede — falhar aqui não pode ter gasto um número.
            val referencias = runCatching { registrarParticipantes(estado, agenciaId) }.getOrNull()
            if (referencias == null) {
                concluir(EventoDeEmissao.Falhou(MotivoDeFalha.POOL_INDISPONIVEL))
                return@launch
            }

            val passagem = montar(estado, ocorrencia, referencias, contexto.funcionario?.id.orEmpty(), agenciaId)

            // 2. As guardas, com a contagem lida do servidor (a cota é firestore-driven, ADR-0013 §8).
            if (passagem is PassagemDePassageiro) {
                val guarda = runCatching { avaliarComCota(passagem, ocorrencia) }.getOrNull()
                if (guarda == null) {
                    concluir(EventoDeEmissao.Falhou(MotivoDeFalha.SEM_REDE))
                    return@launch
                }
                if (guarda != ResultadoEmissao.Ok) {
                    concluir(EventoDeEmissao.Bloqueada(guarda))
                    return@launch
                }
            }

            // 3. O número, que é o ponto sem volta — e depois a emissão.
            val numero = runCatching { passagemRepository.reservarNumero(ocorrencia) }.getOrNull()
            if (numero == null) {
                concluir(EventoDeEmissao.Falhou(MotivoDeFalha.NUMERO_INDISPONIVEL))
                return@launch
            }

            val id = runCatching { passagemRepository.emitir(passagem.comNumero(numero.toString())) }.getOrNull()
            if (id == null) {
                concluir(EventoDeEmissao.Falhou(MotivoDeFalha.SEM_REDE))
                return@launch
            }
            concluir(EventoDeEmissao.Emitida(id))
        }
    }

    /**
     * Registra no pool quem o bilhete vai referenciar, e devolve os ids.
     *
     * **Passageiro exige portador; veículo não** ([ADR-0028] D3): sem cliente salvo o agregado é
     * inescrevível, então a falha aqui aborta a emissão — com o atendimento intacto.
     */
    private suspend fun registrarParticipantes(
        estado: EmissaoUiState,
        agenciaId: String,
    ): ParticipantesRegistrados = when (val participante = estado.participante) {
        is ParticipanteEmEdicao.DePassageiro -> ParticipantesRegistrados(
            clienteIds = participante.pessoas
                .mapNotNull { it.paraCliente() }
                .map { clienteRepository.criarOuAssinar(it, agenciaId) },
        )

        is ParticipanteEmEdicao.DeVeiculo -> {
            val veiculo = participante.veiculo.paraVeiculo() ?: error("veículo incompleto")
            ParticipantesRegistrados(
                veiculoId = veiculoRepository.criarOuAssinar(veiculo, agenciaId),
                clienteIds = listOfNotNull(
                    participante.responsavel?.paraCliente()
                        ?.let { clienteRepository.criarOuAssinar(it, agenciaId) },
                ),
            )
        }
    }

    /**
     * Conta as gratuidades **daquela categoria** já emitidas na ocorrência e avalia a guarda.
     *
     * O escopo é [EscopoEmpresa.Todas] de propósito, e não a agência de quem emite: a cota é **assento livre
     * da travessia**, como a ocupação — fatiá-la por agência daria duas vagas a cada uma. Canceladas não
     * contam (não ocupam, ADR-0018 D18).
     */
    private suspend fun avaliarComCota(
        passagem: PassagemDePassageiro,
        ocorrencia: OcorrenciaViagem,
    ): ResultadoEmissao {
        val gratuidade = passagem.gratuidade ?: return avaliarEmissao(passagem, jaEmitidasNaCategoria = 0)

        val emitidas = passagemRepository.consultar(
            CriterioPassagem(
                recorte = RecorteTemporal.Ocorrencia(ocorrencia),
                escopo = EscopoEmpresa.Todas,
                categoria = CategoriaPassagem.PASSAGEIRO,
                gratuidade = gratuidade,
            ),
        ).count { it.metadados.status != StatusPassagem.CANCELADA }

        return avaliarEmissao(passagem, jaEmitidasNaCategoria = emitidas)
    }

    private fun montar(
        estado: EmissaoUiState,
        ocorrencia: OcorrenciaViagem,
        registrados: ParticipantesRegistrados,
        funcionarioId: String,
        agenciaId: String,
    ): Passagem {
        val agora = relogio.agora().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val metadados = MetadadosPassagem(
            status = StatusPassagem.EMITIDA,
            funcionarioId = funcionarioId,
            agenciaId = agenciaId,
            criadoEm = agora,
            alteradoEm = agora,
        )
        val lancamentos = estado.pagamento.lancamentos.mapNotNull { linha ->
            linha.valorEmReais()?.let { Lancamento(id = UUID.randomUUID().toString(), forma = linha.forma, valor = it) }
        }
        val observacao = estado.pagamento.observacao.takeIf { it.isNotBlank() }

        return when (val participante = estado.participante) {
            is ParticipanteEmEdicao.DePassageiro -> PassagemDePassageiro(
                numero = "",
                ocorrencia = ocorrencia,
                lancamentos = lancamentos,
                observacao = observacao,
                metadados = metadados,
                acomodacao = estado.bilhete.acomodacao ?: error("acomodação não escolhida"),
                tipo = estado.bilhete.tipo,
                gratuidade = estado.bilhete.gratuidade,
                clientes = registrados.clienteIds,
            )

            is ParticipanteEmEdicao.DeVeiculo -> PassagemDeVeiculo(
                numero = "",
                ocorrencia = ocorrencia,
                lancamentos = lancamentos,
                observacao = observacao,
                metadados = metadados,
                veiculoId = registrados.veiculoId.orEmpty(),
                responsavelRetirada = registrados.clienteIds.firstOrNull(),
            )
        }
    }

    private suspend fun concluir(evento: EventoDeEmissao) {
        _uiState.update { it.copy(emitindo = false) }
        _eventos.send(evento)
    }

    private fun redimensionar(participante: ParticipanteEmEdicao, maximo: Int): ParticipanteEmEdicao =
        when (participante) {
            is ParticipanteEmEdicao.DePassageiro -> participante.copy(pessoas = participante.pessoas.take(maximo))
            is ParticipanteEmEdicao.DeVeiculo -> participante
        }

    private fun Passagem.comNumero(numero: String): Passagem = when (this) {
        is PassagemDePassageiro -> copy(numero = numero)
        is PassagemDeVeiculo -> copy(numero = numero)
    }

    private data class ParticipantesRegistrados(
        val clienteIds: List<String> = emptyList(),
        val veiculoId: String? = null,
    )
}

/** O que aconteceu com a emissão — **uma vez**, e nunca estado ([ADR-0026] D3). */
sealed interface EventoDeEmissao {
    /** Emitida: carrega o id, que é o que o QR do bilhete leva. */
    data class Emitida(val idPassagem: String) : EventoDeEmissao

    /** Uma **guarda de negócio** barrou (cota, incoerência) — a emissão não deveria acontecer. */
    data class Bloqueada(val motivo: ResultadoEmissao) : EventoDeEmissao

    /** Algo **falhou** (rede, sessão) — a emissão poderia acontecer, e o atendimento continua na tela. */
    data class Falhou(val motivo: MotivoDeFalha) : EventoDeEmissao
}

/**
 * As falhas que interrompem a emissão, separadas porque cada uma pede uma frase diferente ao operador — e
 * porque três delas são **rede**, que é o estado normal de uma bilheteria de beira de rio.
 */
enum class MotivoDeFalha {
    /** A sessão não tem vínculo: quem emite é da operação (ADR-0015 §8.4). */
    SEM_VINCULO,

    /** Chegou aqui sem saída escolhida — a ocorrência vem do card de Início. */
    SEM_OCORRENCIA,

    /** O registro no pool não completou. É a operação que **exige rede** (ADR-0025 D6). */
    POOL_INDISPONIVEL,

    /** A reserva do número não completou — e ela é transação no servidor (ADR-0024 D6). */
    NUMERO_INDISPONIVEL,

    /** A escrita ou a consulta falharam. */
    SEM_REDE,
}
