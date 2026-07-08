package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem

import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.extensions.isTextoNaoNulo
import dev.matheus.fluviapp.extensions.preencherCampo
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Categoria.CATEGORIA_PASSAGEM
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Categoria.DOCUMENTO
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Categoria.PAGAMENTO
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Categoria.STATUS_PASSAGEM
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.A_EMITIR
import dev.matheus.fluviapp.model.cadastro.constantes.obterDescricaoFormatada
import dev.matheus.fluviapp.model.passagem.Passagem
import dev.matheus.fluviapp.model.passagem.Passagem.Companion.DESCONTO_ANTAC
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
) {

    init {
        atualizaCampos()
    }

    lateinit var viagem: Viagem

    private fun atualizaCampos() {
        uiStatePassagem.update { statePassagem ->
            statePassagem.copy(
                onCheckVeiculo = {
                    checkVeiculo()
                },
                onHoraViagemChange = {
                    atualizarHoraViagem(it)
                },
                onDataViagemChange = {
                    atualizarDataViagem(it)
                },
                onAgenciaChange = {
                    atualizarListaAgente(it)
                    atualizarAgencia(it)
                },
                onAgenteChange = {
                    atualizarAgente(it)
                },
                onCheckPix = {
                    checkPix()
                },
                onCheckDinheiro = {
                    checkDinheiro()
                },
                onCheckDebito = {
                    checkDebito()
                },
                onCheckCredito = {
                    checkCredito()
                },
                onValorPagoChange = {
                    atualizarValorPago(it)
                },
                onValorPixChange = {
                    atualizarValorPix(it)
                },
                onValorDinheiroChange = {
                    atualizarValorDinheiro(it)
                },
                onValorDebitoChange = {
                    atualizarValorDebito(it)
                },
                onValorCreditoChange = {
                    atualizarValorCredito(it)
                },
                onDescontoChange = {
                    atualizarDesconto(it)
                },
                onObservacaoChange = { obs, ehFalado ->
                    atualizaObservacao(obs, ehFalado)
                },
                listaTipoDocumento = runBlocking { constanteRepository.obterTodosPorCategoria(DOCUMENTO.name) },
                listaAgencia = runBlocking { agenteRepository.obterTodasAgencias() },
                listaAgente = runBlocking { agenteRepository.obterTodosAgentes() },
                listaFormaPagamento = runBlocking { constanteRepository.obterTodosPorCategoria(PAGAMENTO.name) },
                listaSituacaoPassagem = runBlocking { constanteRepository.obterTodosPorCategoria(STATUS_PASSAGEM.name) },
                listaCategoriaPassagem = runBlocking { constanteRepository.obterTodosPorCategoria(CATEGORIA_PASSAGEM.name) }
            )
        }
    }

    private fun atualizarListaAgente(agenciaDescricao: String) {
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

    private fun atualizarDataViagem(data: String) {
        uiStatePassagem.update {
            it.copy(
                dataViagem = data,
                isDataViagemError = false
            )
        }
    }

    private fun atualizarHoraViagem(hora: String) {
        uiStatePassagem.update {
            it.copy(
                horaViagem = hora,
                isHoraViagemError = false
            )
        }
    }

    private fun atualizarAgencia(agencia: String) {
        uiStatePassagem.update {
            it.copy(
                agencia = agencia,
                agente = "",
                isAgenciaError = false,
                isAgenteDisabled = agencia.isBlank()
            )
        }
    }

    private fun atualizarAgente(agente: String) {
        uiStatePassagem.update {
            it.copy(
                agente = agente,
                isAgenteError = false
            )
        }
    }

    private fun checkPix() {
        uiStatePassagem.update {
            it.copy(
                isPixChecked = !it.isPixChecked,
                isFormaPagamentoError = false
            )
        }
    }

    private fun checkDinheiro() {
        uiStatePassagem.update {
            it.copy(
                isDinheiroChecked = !it.isDinheiroChecked,
                isFormaPagamentoError = false
            )
        }
    }

    private fun checkDebito() {
        uiStatePassagem.update {
            it.copy(
                isDebitoChecked = !it.isDebitoChecked,
                isFormaPagamentoError = false
            )
        }
    }

    private fun checkCredito() {
        uiStatePassagem.update {
            it.copy(
                isCreditoChecked = !it.isCreditoChecked,
                isFormaPagamentoError = false
            )
        }
    }

    private fun atualizarValorPago(valor: String) {
        uiStatePassagem.update {
            it.copy(
                valorPago = valor,
                isValorPagoError = false
            )
        }
    }

    private fun atualizarValorPix(valor: String) {
        uiStatePassagem.update {
            it.copy(
                valorPix = valor,
                isValorPixError = false,
            )
        }
    }

    private fun atualizarValorDinheiro(valor: String) {
        uiStatePassagem.update {
            it.copy(
                valorDinheiro = valor,
                isValorDinheiroError = false
            )
        }
    }

    private fun atualizarValorDebito(valor: String) {
        uiStatePassagem.update {
            it.copy(
                valorDebito = valor,
                isValorDebitoError = false
            )
        }
    }

    private fun atualizarValorCredito(valor: String) {
        uiStatePassagem.update {
            it.copy(
                valorCredito = valor,
                isValorCreditoError = false
            )
        }
    }

    private fun atualizarDesconto(desconto: String) {
        uiStatePassagem.update {
            it.copy(
                desconto = desconto
            )
        }
    }

    private fun atualizaObservacao(obs: String, ehGravacao: Boolean) {
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

        viagem.let {
            atualizarEmpresaViagem(viagem.empresa)
            atualizarNavioViagem(viagem.navio)
            atualizarOrigemViagem(viagem.origem)
            atualizarDestinoViagem(viagem.destino)
            atualizarCodigoViagem(it.codigo)
        }
    }

    suspend fun salvarPassagem(
        idPassagem: String,
        funcionarioResponsavel: String,
    ): String {

        val passagem = montarPassagem(
            idPassagem = idPassagem,
            funcionarioResponsavel = funcionarioResponsavel
        )

        return passagemRepository.salvar(idPassagem, passagem)
    }

    private suspend fun montarPassagem(idPassagem: String, funcionarioResponsavel: String): Passagem {
        val statePassagem = uiStatePassagem.value
        val statePassageiro = uiStatePassageiro.value
        val stateVeiculo = uiStateVeiculo.value

        val situacaoPassagem = A_EMITIR.obterDescricaoFormatada()

        val desconto = verificarDesconto(statePassageiro, statePassagem)

        var passagemExistente: Passagem? = null

        if (idPassagem.isTextoNaoNulo()) {
            passagemExistente = passagemRepository.obterPorId(idPassagem)
        }

        val numeroBilhete = passagemRepository.obterContagem().inc()

        return Passagem(
            id = passagemExistente?.id.orEmpty(),
            numero = passagemExistente?.numero ?: numeroBilhete.toString(),
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
            desconto = desconto,
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
            funcionarioResponsavel = funcionarioResponsavel,
            status = situacaoPassagem
        )
    }

    private fun verificarDesconto(
        statePassageiro: FormPassageiroUiState,
        statePassagem: FormPassagemUiState
    ): Double = calcularDesconto(statePassageiro, statePassagem)

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
                desconto = passagem.desconto.preencherCampo(),
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
                desconto = "",
                observacao = "",
            )
        }
    }

    companion object {
        private val COMANDOS_VOZ = listOf("apagar", "deletar", "apaga", "deleta")
    }
}