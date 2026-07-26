package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem

import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.extensions.isTextoNaoNulo
import dev.matheus.fluviapp.extensions.preencherCampo
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Categoria.CATEGORIA_PASSAGEM
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Categoria.DOCUMENTO
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Categoria.PAGAMENTO
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Categoria.STATUS_PASSAGEM
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.MOTO
import dev.matheus.fluviapp.model.mappers.ViagemDadosViagemMapper
import dev.matheus.fluviapp.model.passagem.Passagem
import dev.matheus.fluviapp.model.passagem.ResultadoEmissao
import dev.matheus.fluviapp.model.passagem.StatusPassagem
import dev.matheus.fluviapp.model.passagem.tarifaMotoBase
import dev.matheus.fluviapp.model.viagem.Viagem
import dev.matheus.fluviapp.services.repository.cadastro.ConstanteRepository
import dev.matheus.fluviapp.services.repository.cadastro.passagem.AgenteRepository
import dev.matheus.fluviapp.services.repository.firebase.PassagemFirestoreRepository
import dev.matheus.fluviapp.services.repository.firebase.ViagemFirestoreRepository
import dev.matheus.fluviapp.ui.states.passagem.FormPassageiroUiState
import dev.matheus.fluviapp.ui.states.passagem.FormPassagemUiState
import dev.matheus.fluviapp.ui.states.passagem.FormVeiculoUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking

class FormPassagemHelper(
    private val uiStatePassagem: MutableStateFlow<FormPassagemUiState>,
    private val uiStatePassageiro: MutableStateFlow<FormPassageiroUiState>,
    private val uiStateVeiculo: MutableStateFlow<FormVeiculoUiState>,
    private val constanteRepository: ConstanteRepository,
    private val viagemRepository: ViagemFirestoreRepository,
    private val agenteRepository: AgenteRepository,
    private val passagemRepository: PassagemFirestoreRepository,
    private val viagemDadosViagemMapper: ViagemDadosViagemMapper,
) {

    lateinit var viagem: Viagem

    /** Carga suspensa das listas (molde ADR-0006): sem `runBlocking` na thread principal no init. */
    suspend fun carregarListas() {
        uiStatePassagem.update {
            it.copy(
                listaTipoDocumento = constanteRepository.obterTodosPorCategoria(DOCUMENTO.name),
                listaAgencia = agenteRepository.obterTodasAgencias(),
                listaAgente = agenteRepository.obterTodosAgentes(),
                listaFormaPagamento = constanteRepository.obterTodosPorCategoria(PAGAMENTO.name),
                listaSituacaoPassagem = constanteRepository.obterTodosPorCategoria(STATUS_PASSAGEM.name),
                listaCategoriaPassagem = constanteRepository.obterTodosPorCategoria(CATEGORIA_PASSAGEM.name),
            )
        }
    }

    internal fun atualizarListaAgente(agenciaDescricao: String) {
        uiStatePassagem.update { state ->
            state.copy(
                listaAgente = runBlocking { agenteRepository.obterAgentesPorAgencia(agenciaDescricao) }
            )
        }
    }

    internal fun checkVeiculo() {
        uiStatePassagem.update {
            it.copy(
                isVeiculoChecked = !it.isVeiculoChecked
            )
        }

        limparCamposPassageiroOuVeiculo()
    }

    private fun limparCamposPassageiroOuVeiculo() {
        if (uiStatePassagem.value.isVeiculoChecked) {
            uiStatePassageiro.update {
                it.copy(
                    acomodacao = "",
                    tipoPassagem = "",
                    tipoGratuidade = "",
                    nomePassageiro1 = "",
                    nomePassageiro2 = "",
                    nomePassageiro3 = "",
                    tipoDocumentoPassageiro1 = "",
                    tipoDocumentoPassageiro2 = "",
                    tipoDocumentoPassageiro3 = "",
                    documentoPassageiro1 = "",
                    documentoPassageiro2 = "",
                    documentoPassageiro3 = "",
                    dataNascimentoPassageiro1 = "",
                    dataNascimentoPassageiro2 = "",
                    dataNascimentoPassageiro3 = "",
                    isPassageiro2Checked = false,
                    isPassageiro3Checked = false
                )
            }
        } else {
            uiStateVeiculo.update {
                it.copy(
                    nomeResponsavelRetirada = "",
                    tipoDocumentoResponsavelRetirada = "",
                    documentoResponsavelRetirada = "",
                    tipoVeiculo = "",
                    modeloVeiculo = "",
                    corVeiculo = "",
                    placaVeiculo = ""
                )
            }
        }
    }

    private fun atualizarEmpresaViagem(empresa: String) {
        uiStatePassagem.update {
            it.copy(
                empresaViagem = empresa,
            )
        }
    }

    private fun atualizarNavioViagem(navio: String) {
        uiStatePassagem.update {
            it.copy(
                navioViagem = navio,
            )
        }
    }

    private fun atualizarOrigemViagem(origem: String) {
        uiStatePassagem.update {
            it.copy(
                origemViagem = origem
            )
        }
    }

    private fun atualizarDestinoViagem(destino: String) {
        uiStatePassagem.update {
            it.copy(
                destinoViagem = destino
            )
        }
    }

    private fun atualizarCodigoViagem(codigo: String) {
        uiStatePassagem.update {
            it.copy(
                codigoViagem = codigo
            )
        }
    }

    private fun atualizarViagemId(id: String) {
        uiStatePassagem.update {
            it.copy(
                viagemId = id
            )
        }
    }

    private fun atualizarIdsViagem(navioId: String, empresaId: String) {
        uiStatePassagem.update {
            it.copy(
                navioId = navioId,
                empresaId = empresaId,
            )
        }
    }

    internal fun atualizarDataViagem(data: String) {
        uiStatePassagem.update {
            it.copy(
                dataViagem = data,
                isDataViagemError = false
            )
        }
    }

    internal fun atualizarHoraViagem(hora: String) {
        uiStatePassagem.update {
            it.copy(
                horaViagem = hora,
                isHoraViagemError = false
            )
        }
    }

    internal fun atualizarAgencia(agencia: String) {
        uiStatePassagem.update {
            it.copy(
                agencia = agencia,
                agente = "",
                isAgenciaError = false,
                isAgenteDisabled = agencia.isBlank()
            )
        }
    }

    internal fun atualizarAgente(agente: String) {
        uiStatePassagem.update {
            it.copy(
                agente = agente,
                isAgenteError = false
            )
        }
    }

    internal fun checkPix() {
        uiStatePassagem.update {
            it.copy(
                isPixChecked = !it.isPixChecked,
                isFormaPagamentoError = false
            )
        }
    }

    internal fun checkDinheiro() {
        uiStatePassagem.update {
            it.copy(
                isDinheiroChecked = !it.isDinheiroChecked,
                isFormaPagamentoError = false
            )
        }
    }

    internal fun checkDebito() {
        uiStatePassagem.update {
            it.copy(
                isDebitoChecked = !it.isDebitoChecked,
                isFormaPagamentoError = false
            )
        }
    }

    internal fun checkCredito() {
        uiStatePassagem.update {
            it.copy(
                isCreditoChecked = !it.isCreditoChecked,
                isFormaPagamentoError = false
            )
        }
    }

    internal fun atualizarValorPago(valor: String) {
        uiStatePassagem.update {
            it.copy(
                valorPago = valor,
                isValorPagoError = false
            )
        }
    }

    internal fun atualizarValorPix(valor: String) {
        uiStatePassagem.update {
            it.copy(
                valorPix = valor,
                isValorPixError = false,
            )
        }
    }

    internal fun atualizarValorDinheiro(valor: String) {
        uiStatePassagem.update {
            it.copy(
                valorDinheiro = valor,
                isValorDinheiroError = false
            )
        }
    }

    internal fun atualizarValorDebito(valor: String) {
        uiStatePassagem.update {
            it.copy(
                valorDebito = valor,
                isValorDebitoError = false
            )
        }
    }

    internal fun atualizarValorCredito(valor: String) {
        uiStatePassagem.update {
            it.copy(
                valorCredito = valor,
                isValorCreditoError = false
            )
        }
    }

    internal fun atualizaObservacao(obs: String, ehGravacao: Boolean) {
        uiStatePassagem.update {
            when {
                COMANDOS_VOZ.contains(obs) -> {
                    it.copy(
                        observacao = ""
                    )
                }

                !ehGravacao -> {
                    it.copy(
                        observacao = obs.uppercase()
                    )
                }

                ehGravacao && it.observacao.isBlank() -> {
                    it.copy(
                        observacao = obs.uppercase()
                    )
                }

                else -> {
                    it.copy(
                        observacao = "${it.observacao.trim()} ${obs.uppercase()}"
                    )
                }
            }
        }
    }

    /**
     * Bloqueio de emissão fail-closed (ADR-0013): em vez do toast transiente, marca um banner persistente
     * com a causa e DESTACA o campo responsável (a chave sem tarifa). SemTarifa → acomodação (passageiro)
     * ou tipoVeiculo/cilindrada (veículo/moto); a tela rola até o banner.
     */
    internal fun bloquearEmissaoSemTarifa() {
        val statePassagem = uiStatePassagem.value
        if (statePassagem.isVeiculoChecked) {
            val veiculo = uiStateVeiculo.value
            if (veiculo.tipoVeiculo == MOTO.name && veiculo.cilindrada.isBlank()) {
                uiStateVeiculo.update { it.copy(isCilindradaError = true) }
            } else {
                uiStateVeiculo.update { it.copy(isTipoVeiculoError = true) }
            }
        } else {
            uiStatePassageiro.update { it.copy(isAcomodacaoError = true) }
        }
        uiStatePassagem.update {
            it.copy(emissaoBloqueadaMsg = R.string.error_emissao_sem_tarifa, emissaoBloqueadaArg = "")
        }
    }

    internal fun bloquearEmissaoCotaGratuidade(categoria: String) {
        uiStatePassageiro.update { it.copy(isTipoGratuidadeError = true) }
        uiStatePassagem.update {
            it.copy(emissaoBloqueadaMsg = R.string.error_emissao_cota_gratuidade, emissaoBloqueadaArg = categoria)
        }
    }

    internal fun limparBloqueioEmissao() {
        uiStatePassagem.update { it.copy(emissaoBloqueadaMsg = 0, emissaoBloqueadaArg = "") }
    }

    fun atualizarIsSaving() {
        uiStatePassagem.update {
            it.copy(
                isSaving = !it.isSaving
            )
        }
    }

    fun atualizarIsLoading() {
        uiStatePassagem.update {
            it.copy(
                isLoading = !it.isLoading
            )
        }
    }

    suspend fun atualizarDadosViagemPorId(idViagem: String) {
        viagem = viagemRepository.obterPorId(idViagem)

        // A Viagem relaciona por id (ADR-0008 Fase 3); resolve os nomes p/ o snapshot da Passagem via
        // o mapper (mesma resolução do card). Esses nomes são congelados na Passagem na emissão.
        val card = viagemDadosViagemMapper.map(viagem)
        atualizarViagemId(viagem.id)
        atualizarIdsViagem(navioId = viagem.navioId, empresaId = viagem.empresaId)
        atualizarEmpresaViagem(card.empresa)
        atualizarNavioViagem(card.navio)
        atualizarOrigemViagem(card.origem)
        atualizarDestinoViagem(card.destino)
        atualizarCodigoViagem(card.codigo)

        // Tabela de tarifas da viagem (ADR-0013): alimenta o preview de valor e a tarifaBase congelada.
        val tarifas = viagemRepository.obterTarifas(viagem.id).associate { it.chave to it.valor }
        uiStatePassagem.update { it.copy(tarifasViagem = tarifas) }
    }

    /**
     * Guardas de emissão (ADR-0013 §2b), fail-closed. Roda antes de montar/salvar:
     * - **Sem tarifa**: a chave escolhida (acomodação/classe/moto) não resolve uma tarifaBase → bloqueia
     *   (sem base não há como medir desconto/déficit).
     * - **Cota de gratuidade**: só na criação, máx. 2 por categoria por viagem (contagem firestore-driven).
     */
    suspend fun validarEmissao(idPassagem: String): ResultadoEmissao {
        val statePassagem = uiStatePassagem.value
        val statePassageiro = uiStatePassageiro.value
        val stateVeiculo = uiStateVeiculo.value

        val tarifaBase = resolverTarifaBase(statePassagem, statePassageiro, stateVeiculo)
        if (tarifaBase == null) return ResultadoEmissao.SemTarifa

        // Cota vale na criação E na edição que vira gratuidade; o próprio bilhete é excluído da contagem
        // (excetoId) para uma edição não bloquear a si mesma.
        if (statePassageiro.isGratuidade) {
            val categoria = statePassageiro.tipoGratuidade
            val emitidas = passagemRepository.contarGratuidadePorViagem(
                viagemId = statePassagem.viagemId,
                gratuidade = categoria,
                excetoId = idPassagem,
            )
            if (emitidas >= LIMITE_GRATUIDADE_POR_CATEGORIA) {
                return ResultadoEmissao.CotaGratuidadeAtingida(categoria)
            }
        }

        return ResultadoEmissao.Ok
    }

    suspend fun salvarPassagem(
        idPassagem: String,
        funcionarioResponsavel: String,
        funcionarioId: String,
    ): String {

        val passagem = montarPassagem(
            idPassagem = idPassagem,
            funcionarioResponsavel = funcionarioResponsavel,
            funcionarioId = funcionarioId
        )

        return passagemRepository.salvar(idPassagem, passagem)
    }

    private suspend fun montarPassagem(
        idPassagem: String,
        funcionarioResponsavel: String,
        funcionarioId: String,
    ): Passagem {
        val statePassagem = uiStatePassagem.value
        val statePassageiro = uiStatePassageiro.value
        val stateVeiculo = uiStateVeiculo.value

        // Status canônico (ADR-0012): grava o .name do tipo de domínio; formatação fica na exibição.
        val situacaoPassagem = StatusPassagem.A_EMITIR.name

        var passagemExistente: Passagem? = null

        if (idPassagem.isTextoNaoNulo()) {
            passagemExistente = passagemRepository.obterPorId(idPassagem)
        }

        val numeroBilhete = passagemRepository.obterContagem().inc()

        // Congela a tarifa da inteira (ADR-0013): célula da tabela da Viagem p/ a chave escolhida
        // (acomodação do passageiro ou classe do veículo), resolvida na emissão. Preservada na edição
        // (snapshot, como a autoria). Moto → null (tarifa por cilindrada, próxima fatia da Fase 3).
        val tarifaBase = passagemExistente?.tarifaBase
            ?: resolverTarifaBase(statePassagem, statePassageiro, stateVeiculo)

        return Passagem(
            id = passagemExistente?.id.orEmpty(),
            numero = passagemExistente?.numero ?: numeroBilhete.toString(),
            viagemId = statePassagem.viagemId,
            navioId = statePassagem.navioId,
            empresaId = statePassagem.empresaId,
            codigoViagem = statePassagem.codigoViagem,
            empresa = statePassagem.empresaViagem,
            navio = statePassagem.navioViagem,
            origem = statePassagem.origemViagem,
            destino = statePassagem.destinoViagem,
            dataViagem = statePassagem.dataViagem,
            horaViagem = statePassagem.horaViagem,
            agencia = statePassagem.agencia,
            agente = statePassagem.agente,
            valorPago = statePassagem.valorPago.toDoubleOrNull(),
            valorPix = statePassagem.valorPix.toDoubleOrNull(),
            valorDinheiro = statePassagem.valorDinheiro.toDoubleOrNull(),
            valorDebito = statePassagem.valorDebito.toDoubleOrNull(),
            valorCredito = statePassagem.valorCredito.toDoubleOrNull(),
            tarifaBase = tarifaBase,
            observacao = statePassagem.observacao,
            tipoPassagem = statePassageiro.tipoPassagem,
            gratuidade = statePassageiro.tipoGratuidade,
            acomodacao = statePassageiro.acomodacao,
            nomePassageiro1 = statePassageiro.nomePassageiro1,
            documentoPassageiro1 = statePassageiro.tipoDocumentoPassageiro1,
            numeroDocumentoPassageiro1 = statePassageiro.documentoPassageiro1,
            dataNascimentoPassageiro1 = statePassageiro.dataNascimentoPassageiro1,
            nomePassageiro2 = statePassageiro.nomePassageiro2,
            documentoPassageiro2 = statePassageiro.tipoDocumentoPassageiro2,
            numeroDocumentoPassageiro2 = statePassageiro.documentoPassageiro2,
            dataNascimentoPassageiro2 = statePassageiro.dataNascimentoPassageiro2,
            nomePassageiro3 = statePassageiro.nomePassageiro3,
            tipoDocumentoPassageiro3 = statePassageiro.tipoDocumentoPassageiro3,
            numeroDocumentoPassageiro3 = statePassageiro.documentoPassageiro3,
            dataNascimentoPassageiro3 = statePassageiro.dataNascimentoPassageiro3,
            nomeResponsavelRetirada = stateVeiculo.nomeResponsavelRetirada,
            documentoResponsavelRetirada = stateVeiculo.tipoDocumentoResponsavelRetirada,
            numeroDocumentoResponsavelRetirada = stateVeiculo.documentoResponsavelRetirada,
            tipoVeiculo = stateVeiculo.tipoVeiculo,
            modeloVeiculo = stateVeiculo.modeloVeiculo,
            placaVeiculo = stateVeiculo.placaVeiculo,
            corVeiculo = stateVeiculo.corVeiculo,
            cilindrada = stateVeiculo.cilindrada,
            // Autoria congelada na emissão (ADR-0008/0010): só a CRIAÇÃO carimba o usuário atual;
            // editar preserva dono/responsável originais (um gestor editar não vira dono).
            funcionarioResponsavel = passagemExistente?.funcionarioResponsavel ?: funcionarioResponsavel,
            funcionarioId = passagemExistente?.funcionarioId ?: funcionarioId,
            status = situacaoPassagem
        )
    }

    /**
     * Tarifa da inteira da célula (viagem × chave tarifária) na tabela cadastrada (ADR-0013). A **chave** é
     * a acomodação (passageiro) ou o tipo de veículo (CARRO/CARRETA/CAMINHAO). Lê o espelho local das
     * tarifas da viagem. Moto cai em null aqui: não há célula — sua tarifa é a regra por cilindrada (próxima
     * fatia da Fase 3). Chave em branco ou célula sem tarifa cadastrada → null (o fail-closed é passo à parte).
     */
    private suspend fun resolverTarifaBase(
        statePassagem: FormPassagemUiState,
        statePassageiro: FormPassageiroUiState,
        stateVeiculo: FormVeiculoUiState,
    ): Double? {
        val ehVeiculo = statePassagem.isVeiculoChecked

        // Moto: tarifa pela regra da cilindrada (ADR-0013), não por célula cadastrada.
        if (ehVeiculo && stateVeiculo.tipoVeiculo == MOTO.name) {
            val cc = stateVeiculo.cilindrada.toIntOrNull() ?: return null
            return tarifaMotoBase(cc).toDouble()
        }

        val chave = if (ehVeiculo) stateVeiculo.tipoVeiculo else statePassageiro.acomodacao
        if (chave.isBlank()) return null
        return viagemRepository.obterTarifas(statePassagem.viagemId)
            .firstOrNull { it.chave == chave }?.valor
    }

    fun preencherDadosPassagem(
        passagem: Passagem,
    ) {
        uiStatePassagem.update { state ->
            state.copy(
                dataViagem = passagem.dataViagem,
                horaViagem = passagem.horaViagem,
                isAgenteDisabled = false,
                valorPago = passagem.valorPago.preencherCampo(),
                isPixChecked = passagem.valorPix.preencherCampo().isNotEmpty(),
                valorPix = passagem.valorPix.preencherCampo(),
                isDinheiroChecked = passagem.valorDinheiro.preencherCampo().isNotEmpty(),
                valorDinheiro = passagem.valorDinheiro.preencherCampo(),
                isDebitoChecked = passagem.valorDebito.preencherCampo().isNotEmpty(),
                valorDebito = passagem.valorDebito.preencherCampo(),
                isCreditoChecked = passagem.valorCredito.preencherCampo().isNotEmpty(),
                valorCredito = passagem.valorCredito.preencherCampo(),
                observacao = passagem.observacao.orEmpty(),
                titleForm = R.string.subtitle_editar_passagem,
                isEditing = true
            )
        }
    }

    fun limparState() {
        uiStatePassagem.update {
            it.copy(
                agencia = "",
                agente = "",
                valorPago = "",
                valorPix = "",
                valorDinheiro = "",
                valorDebito = "",
                valorCredito = "",
                observacao = "",
            )
        }
    }

    companion object {
        private val COMANDOS_VOZ = listOf("apagar", "deletar", "apaga", "deleta")
        private const val LIMITE_GRATUIDADE_POR_CATEGORIA = 2
    }
}