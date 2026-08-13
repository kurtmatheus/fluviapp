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
import dev.matheus.fluviapp.ui.states.passagem.CabecalhoDaViagem
import dev.matheus.fluviapp.ui.states.passagem.ClienteEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.EmissaoUiState
import dev.matheus.fluviapp.ui.states.passagem.PagamentoEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.ParticipanteEmEdicao
import dev.matheus.fluviapp.ui.states.passagem.PassoDaEmissao
import dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.ColetorDeReferencias
import dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.cabecalhoDe
import dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.confirmacaoDe
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
    /** Resolve o **cabeçalho da saída** — é a mesma junção do embarque, sobre a ocorrência. */
    private val coletorDeReferencias: ColetorDeReferencias,
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

    fun iniciar(ocorrencia: OcorrenciaViagem, cabecalho: CabecalhoDaViagem) {
        this.ocorrencia = ocorrencia
        _uiState.update { EmissaoUiState(cabecalho = cabecalho) }
    }

    /**
     * A entrada real, pela navegação: recebe a **chave da ocorrência** e resolve o cabeçalho sozinha.
     *
     * O cabeçalho é junção (travessia e partida são ids no domínio), então segue o mesmo desenho do embarque:
     * o coletor busca, uma função pura formata. Falhar em resolver **não impede vender** — o bilhete aponta
     * para a ocorrência de qualquer forma, e um cabeçalho vazio é menos grave do que uma fila parada.
     */
    fun iniciarPelaChave(chaveDaOcorrencia: String?) {
        val ocorrencia = OcorrenciaViagem.deChave(chaveDaOcorrencia)
        if (ocorrencia == null) {
            viewModelScope.launch { _eventos.send(EventoDeEmissao.Falhou(MotivoDeFalha.SEM_OCORRENCIA)) }
            return
        }
        this.ocorrencia = ocorrencia
        _uiState.update { EmissaoUiState() }

        viewModelScope.launch {
            val referencias = runCatching { coletorDeReferencias.daOcorrencia(ocorrencia) }.getOrNull()
                ?: return@launch
            _uiState.update { it.copy(cabecalho = cabecalhoDe(ocorrencia, referencias)) }
        }
    }

    /** Recomeça o atendimento **na mesma saída** — o gesto de "nova passagem" do desfecho. */
    fun reiniciar() {
        _uiState.update { EmissaoUiState(cabecalho = it.cabecalho) }
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

    /**
     * Passo 3.2 — quantas pessoas vão na suíte ou no camarote. **É esta resposta que desenha os formulários
     * do passo 4**: escolher três acrescenta dois passos ao roteiro.
     *
     * Reduzir descarta as linhas que sobram, e descartar é melhor do que carregar em silêncio uma pessoa que
     * o bilhete não admite.
     */
    fun escolherQuantidadeDePessoas(quantidade: Int) {
        _uiState.update { estado ->
            val limite = quantidade.coerceIn(1, estado.bilhete.ocupacaoMaxima)
            val atual = estado.participante as? ParticipanteEmEdicao.DePassageiro ?: return@update estado
            val pessoas = List(limite) { indice -> atual.pessoas.getOrElse(indice) { ClienteEmEdicao() } }
            estado.copy(participante = atual.copy(pessoas = pessoas), erros = emptySet())
        }
    }

    /** Passo 2 do fluxo de veículo — e é a classe que decide o que o formulário seguinte pergunta. */
    fun escolherClasseDeVeiculo(classe: dev.matheus.fluviapp.domain.passagem.ClasseVeiculo) {
        _uiState.update { estado ->
            val atual = estado.participante as? ParticipanteEmEdicao.DeVeiculo ?: return@update estado
            estado.copy(
                participante = atual.copy(veiculo = atual.veiculo.copy(classe = classe)),
                erros = emptySet(),
            )
        }
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

    // --- Navegação pelo roteiro ---

    /**
     * Avança **se o passo corrente fecha**. Três passos têm efeito além de mudar de tela:
     *
     * - **cliente**: registra a pessoa no pool ali mesmo ([ADR-0029] D4), para que a operação que exige rede
     *   falhe **uma pessoa por vez**, no passo em que o operador está — e não no fim, depois de três
     *   formulários, sem dizer qual não subiu;
     * - **pagamento**: dispara a emissão, e o roteiro só anda se ela se resolver;
     * - **desfecho**: não avança para lugar nenhum — a saída dali é da navegação.
     */
    fun avancar() {
        val estado = _uiState.value
        val erros = validarPasso(estado.passo, estado.bilhete, estado.participante, estado.pagamento)
        if (erros.isNotEmpty()) {
            _uiState.update { it.copy(erros = erros) }
            return
        }

        when (val passo = estado.passo) {
            is PassoDaEmissao.DadosDoCliente -> viewModelScope.launch { registrarERastrear(passo) }
            // O pagamento **não emite**: ele abre a conferência. A emissão é o gesto seguinte, e é do
            // operador — ver [abrirConferencia].
            PassoDaEmissao.Pagamento -> viewModelScope.launch { abrirConferencia() }
            PassoDaEmissao.Desfecho -> Unit
            else -> seguir()
        }
    }

    /**
     * **O detalhamento que precede a emissão** — conferência dos dados inseridos, não um passo.
     *
     * Ela não conta no roteiro porque não coleta decisão nenhuma: devolve o que já foi respondido para o
     * operador ler antes de confirmar. O que a justifica é a **irreversibilidade**: cancelar depois mantém o
     * número e o registro do bilhete errado, então conferir antes é mais barato — e é o que se faz no balcão
     * de qualquer forma, lendo a tela em voz alta para o passageiro.
     */
    private suspend fun abrirConferencia() {
        val estado = _uiState.value
        val agencia = sessaoUsuario.atual()?.empresaAtivaNome.orEmpty()

        _uiState.update {
            it.copy(
                confirmacao = confirmacaoDe(
                    cabecalho = it.cabecalho,
                    bilhete = it.bilhete,
                    participante = it.participante,
                    pagamento = it.pagamento,
                    agencia = agencia,
                ),
            )
        }
    }

    /** Fecha a conferência **sem emitir** — o operador voltou para corrigir algo que leu na tela. */
    fun revisar() {
        _uiState.update { it.copy(confirmacao = null) }
    }

    /** O gesto que **de fato emite**, depois da conferência. */
    fun confirmarEmissao() {
        _uiState.update { it.copy(confirmacao = null) }
        emitir()
    }

    /** Voltar **não valida**: quem volta está corrigindo, e cobrar o passo na saída seria prendê-lo nele. */
    fun voltar() {
        _uiState.update {
            if (!it.podeVoltar) it else it.copy(indiceDoPasso = it.indiceDoPasso - 1, erros = emptySet())
        }
    }

    /** O "pular" do responsável pela retirada — só existe onde o passo é opcional. */
    fun pular() {
        val passo = _uiState.value.passo
        if (passo !is PassoDaEmissao.DadosDoCliente || !passo.opcional) return
        preencherResponsavel(null)
        seguir()
    }

    private fun seguir() {
        _uiState.update {
            val proximo = (it.indiceDoPasso + 1).coerceAtMost(it.roteiro.lastIndex)
            it.copy(indiceDoPasso = proximo, erros = emptySet())
        }
    }

    /**
     * Registra a pessoa deste passo no pool e **guarda o id no próprio formulário**.
     *
     * Guardar o id ali tem duas consequências boas: voltar ao passo e avançar de novo **assina** em vez de
     * criar (idempotente, ADR-0018 D3), e a emissão do fim já encontra tudo resolvido — ela deixa de ter I/O
     * de pool.
     */
    private suspend fun registrarERastrear(passo: PassoDaEmissao.DadosDoCliente) {
        val estado = _uiState.value
        val agenciaId = sessaoUsuario.atual()?.vinculoAtivo?.empresaId
        if (agenciaId.isNullOrBlank()) {
            _eventos.send(EventoDeEmissao.Falhou(MotivoDeFalha.SEM_VINCULO))
            return
        }

        val emEdicao = when (val participante = estado.participante) {
            is ParticipanteEmEdicao.DePassageiro -> participante.pessoas.getOrNull(passo.indice)
            is ParticipanteEmEdicao.DeVeiculo -> participante.responsavel
        } ?: return

        val cliente = emEdicao.paraCliente() ?: return
        _uiState.update { it.copy(emitindo = true) }

        val id = runCatching { clienteRepository.criarOuAssinar(cliente, agenciaId) }.getOrNull()
        _uiState.update { it.copy(emitindo = false) }

        if (id == null) {
            _eventos.send(EventoDeEmissao.Falhou(MotivoDeFalha.POOL_INDISPONIVEL))
            return
        }

        _uiState.update { atual ->
            val comId = emEdicao.copy(idExistente = id)
            val participante = when (val p = atual.participante) {
                is ParticipanteEmEdicao.DePassageiro -> p.copy(
                    pessoas = p.pessoas.mapIndexed { i, pessoa -> if (i == passo.indice) comId else pessoa },
                )

                is ParticipanteEmEdicao.DeVeiculo -> p.copy(responsavel = comId)
            }
            atual.copy(participante = participante)
        }
        seguir()
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
            // 1. O que falta registrar no pool: as **pessoas já foram**, cada uma no passo dela ([ADR-0029]
            // D4), então aqui sobra o veículo. Continua vindo antes do número pela mesma razão: falhar não
            // pode ter gasto um número da sequência.
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
            // Resolvida: o roteiro anda para o **desfecho**, que é onde o bilhete se entrega ([ADR-0029] D5).
            _uiState.update { it.copy(idEmitida = id) }
            seguir()
            concluir(EventoDeEmissao.Emitida(id))
        }
    }

    /**
     * O que ainda falta registrar quando a emissão chega — e o que falta é **o veículo**.
     *
     * As pessoas já entraram no pool no passo de cada uma ([ADR-0029] D4), e é daí que vem o `idExistente`
     * lido aqui. Uma pessoa sem id neste ponto seria um passo que não completou, e a emissão não a inventa:
     * **passageiro exige portador** ([ADR-0028] D3), então o agregado sai incoerente e a guarda o barra.
     */
    private suspend fun registrarParticipantes(
        estado: EmissaoUiState,
        agenciaId: String,
    ): ParticipantesRegistrados = when (val participante = estado.participante) {
        is ParticipanteEmEdicao.DePassageiro -> ParticipantesRegistrados(
            clienteIds = participante.pessoas.mapNotNull { it.idExistente },
        )

        is ParticipanteEmEdicao.DeVeiculo -> {
            val veiculo = participante.veiculo.paraVeiculo() ?: error("veículo incompleto")
            ParticipantesRegistrados(
                veiculoId = veiculoRepository.criarOuAssinar(veiculo, agenciaId),
                clienteIds = listOfNotNull(participante.responsavel?.idExistente),
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
