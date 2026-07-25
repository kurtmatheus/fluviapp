package dev.matheus.fluviapp.ui.viewmodel.passagem

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.extensions.isTextoNaoNulo
import dev.matheus.fluviapp.extensions.toastMessage
import dev.matheus.fluviapp.model.passagem.Passagem
import dev.matheus.fluviapp.model.passagem.ResultadoEmissao
import dev.matheus.fluviapp.model.mappers.ViagemDadosViagemMapper
import dev.matheus.fluviapp.model.rascunho.aplicarEm
import dev.matheus.fluviapp.model.rascunho.montarRascunho
import dev.matheus.fluviapp.navigation.navcomposables.passagem.EDIT_PASSAGEM_ARGUMENT
import dev.matheus.fluviapp.navigation.navcomposables.passagem.FORM_PASSAGEM_ARGUMENT
import dev.matheus.fluviapp.services.repository.cadastro.ConstanteRepository
import dev.matheus.fluviapp.services.repository.cadastro.passagem.AgenteRepository
import dev.matheus.fluviapp.services.repository.firebase.PassagemFirestoreRepository
import dev.matheus.fluviapp.services.repository.firebase.ViagemFirestoreRepository
import dev.matheus.fluviapp.services.repository.operacoes.UsuarioRepository
import dev.matheus.fluviapp.services.repository.rascunho.RascunhoStore
import dev.matheus.fluviapp.telemetry.Telemetry
import dev.matheus.fluviapp.ui.states.passagem.FormPassageiroUiState
import dev.matheus.fluviapp.ui.states.passagem.FormPassagemUiState
import dev.matheus.fluviapp.ui.states.passagem.FormVeiculoUiState
import dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.FormPassageiroHelper
import dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.FormPassagemHelper
import dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.FormVeiculoHelper
import dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.validacao.ErrosDadosPassagem
import dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.validacao.ErrosPassageiro
import dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.validacao.ErrosVeiculo
import dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.validacao.validarDadosPassagem
import dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.validacao.validarPassageiro
import dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.validacao.validarVeiculo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal const val TAMANHO_CPF = 11
internal const val TAMANHO_CNPJ = 14
internal const val TAMANHO_PASS = 8

@HiltViewModel
class FormPassagemViewModel @Inject constructor(
    private val usuarioRepository: UsuarioRepository,
    private val constanteRepository: ConstanteRepository,
    private val viagemRepository: ViagemFirestoreRepository,
    private val agenteRepository: AgenteRepository,
    private val passagemRepository: PassagemFirestoreRepository,
    private val rascunhoStore: RascunhoStore,
    private val emissaoTelemetry: Telemetry,
    private val viagemDadosViagemMapper: ViagemDadosViagemMapper,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiStatePassagem = MutableStateFlow(FormPassagemUiState())
    val uiStatePassagem: StateFlow<FormPassagemUiState>
        get() = _uiStatePassagem.asStateFlow()

    private val _uiStatePassageiro = MutableStateFlow(FormPassageiroUiState())
    val uiStatePassageiro: StateFlow<FormPassageiroUiState>
        get() = _uiStatePassageiro.asStateFlow()

    private val _uiStateVeiculo = MutableStateFlow(FormVeiculoUiState())
    val uiStateVeiculo: StateFlow<FormVeiculoUiState>
        get() = _uiStateVeiculo.asStateFlow()

    private val idViagem: String = checkNotNull(savedStateHandle[FORM_PASSAGEM_ARGUMENT])
    private val idPassagem: String = checkNotNull(savedStateHandle[EDIT_PASSAGEM_ARGUMENT])

    internal lateinit var formPassagemHelper: FormPassagemHelper
    private lateinit var formPassageiroHelper: FormPassageiroHelper
    private lateinit var formVeiculoHelper: FormVeiculoHelper

    init {
        viewModelScope.launch {
            inicializarHelpers()
            carregarListas()
            preencherViagem()
            inicializarEditor()
            // Rascunho (ADR-0004) só no fluxo de NOVA passagem; editar carrega a verdade do repo.
            if (!idPassagem.isTextoNaoNulo()) {
                restaurarRascunhoSeHouver()
                iniciarAutoSaveRascunho()
            }
        }
    }

    /** cacheada -> volátil: restaura o rascunho, guardado pela viagem (slot único). */
    private suspend fun restaurarRascunhoSeHouver() {
        val snapshot = rascunhoStore.recuperar() ?: return
        val viagemAtual = _uiStatePassagem.value.codigoViagem
        if (snapshot.codigoViagem.isNotBlank() && snapshot.codigoViagem != viagemAtual) return

        val restaurado = snapshot.aplicarEm(
            _uiStatePassagem.value,
            _uiStatePassageiro.value,
            _uiStateVeiculo.value,
        )
        _uiStatePassagem.value = restaurado.passagem
        _uiStatePassageiro.value = restaurado.passageiro
        _uiStateVeiculo.value = restaurado.veiculo
        emissaoTelemetry.rastro("rascunho restaurado (viagem ${snapshot.codigoViagem})")
    }

    /**
     * volátil -> cacheada: grava o rascunho a cada mutação. `drop(1)` pula a emissão inicial
     * (pós-restauração/viagem preenchida) — form intocado NÃO cria rascunho.
     */
    private fun iniciarAutoSaveRascunho() {
        combine(_uiStatePassagem, _uiStatePassageiro, _uiStateVeiculo) { passagem, passageiro, veiculo ->
            montarRascunho(passagem, passageiro, veiculo)
        }
            .drop(1)
            .onEach { snapshot ->
                rascunhoStore.salvar(snapshot)
                emissaoTelemetry.rastro("rascunho salvo")
            }
            .launchIn(viewModelScope)
    }

    private fun inicializarHelpers() {
        formPassagemHelper = FormPassagemHelper(
            uiStatePassagem = _uiStatePassagem,
            uiStatePassageiro = _uiStatePassageiro,
            uiStateVeiculo = _uiStateVeiculo,
            constanteRepository = constanteRepository,
            viagemRepository = viagemRepository,
            agenteRepository = agenteRepository,
            passagemRepository = passagemRepository,
            viagemDadosViagemMapper = viagemDadosViagemMapper,
        )
        formPassageiroHelper = FormPassageiroHelper(
            uiState = _uiStatePassageiro,
            uiStatePassagem = _uiStatePassagem,
            constanteRepository = constanteRepository,
            passagemRepository = passagemRepository
        )
        formVeiculoHelper = FormVeiculoHelper(
            uiState = _uiStateVeiculo,
            constanteRepository = constanteRepository,
            passagemRepository = passagemRepository
        )
    }

    /** Carga suspensa das listas dos sub-forms (molde ADR-0006) — tira o runBlocking do init. */
    private suspend fun carregarListas() {
        formPassagemHelper.carregarListas()
        formPassageiroHelper.carregarListas()
        formVeiculoHelper.carregarListas()
    }

    private suspend fun preencherViagem() {
        formPassagemHelper.atualizarDadosViagemPorId(idViagem)
    }

    private fun inicializarEditor() {
        if (idPassagem.isTextoNaoNulo()) {
            formPassagemHelper.atualizarIsLoading()
            carregarCampos()
        }
    }

    private fun carregarCampos() {
        viewModelScope.launch {
            passagemRepository.obterPorId(idPassagem).let { passagem ->
                formPassagemHelper.preencherDadosPassagem(passagem)

                if (passagem.ehVeiculo) {
                    preencherVeiculo(passagem)
                } else {
                    formPassageiroHelper.preencherDadosPassageiros(passagem)
                }
            }
            formPassagemHelper.atualizarIsLoading()
        }
    }

    private fun preencherVeiculo(passagem: Passagem) {
        formPassagemHelper.checkVeiculo()
        formVeiculoHelper.preencherDadosVeiculo(passagem)
    }

    suspend fun salvarPassagem(context: Context): String? {
        // Guardas de emissão (ADR-0013 §2b), fail-closed: bloqueia sem tarifa tabelada ou com a cota de
        // gratuidade da viagem já atingida. Mensagem via toast; não salva.
        when (val emissao = formPassagemHelper.validarEmissao(idPassagem)) {
            ResultadoEmissao.SemTarifa -> {
                context.toastMessage(context.resources.getString(R.string.error_emissao_sem_tarifa))
                return null
            }

            is ResultadoEmissao.CotaGratuidadeAtingida -> {
                context.toastMessage(
                    context.resources.getString(R.string.error_emissao_cota_gratuidade, emissao.categoria)
                )
                return null
            }

            ResultadoEmissao.Ok -> Unit
        }

        val usuarioLogado = usuarioRepository.obterUltimoUsuarioLogado()

        return usuarioLogado?.let {
            val id = formPassagemHelper.salvarPassagem(
                idPassagem = idPassagem,
                funcionarioResponsavel = it.nome,
                funcionarioId = it.id
            )
            // promoção volátil/cacheada -> sólida: descarta o rascunho (invariante snapshot ⇔ rascunho).
            rascunhoStore.remover()
            emissaoTelemetry.rastro("rascunho descartado (promovido a passagem $id)")
            limparStates()
            context.toastMessage(context.resources.getString(R.string.msg_transmissao_passagem))
            id
        }
    }

    fun validarFormularios(): Boolean {
        // Validação PURA dos dados da passagem (molde ADR-0006, fatia 3).
        val formPassagemvalido = aplicarErrosDadosPassagem(
            validarDadosPassagem(_uiStatePassagem.value, _uiStatePassageiro.value.isGratuidade)
        )
        val formPassageiroVeiculoValido = if (_uiStatePassagem.value.isVeiculoChecked) {
            // Validação PURA do veículo (molde ADR-0006): calcula os erros e o VM os aplica ao estado.
            aplicarErrosVeiculo(validarVeiculo(_uiStateVeiculo.value))
        } else {
            // Validação PURA do passageiro (molde ADR-0006, fatia 2).
            aplicarErrosPassageiro(
                validarPassageiro(_uiStatePassageiro.value, _uiStatePassagem.value.dataViagem)
            )
        }

        return formPassagemvalido && formPassageiroVeiculoValido
    }

    /** Aplica os erros da validação pura dos dados da passagem no estado e devolve se ficou válido. */
    private fun aplicarErrosDadosPassagem(erros: ErrosDadosPassagem): Boolean {
        _uiStatePassagem.update {
            it.copy(
                isDataViagemError = erros.dataViagem,
                textDataViagemError = erros.textDataViagem,
                isHoraViagemError = erros.horaViagem,
                isAgenciaError = erros.agencia,
                isAgenteError = erros.agente,
                isFormaPagamentoError = erros.formaPagamento,
                isValorPagoError = erros.valorPago,
                isValorPixError = erros.valorPix,
                isValorDinheiroError = erros.valorDinheiro,
                isValorDebitoError = erros.valorDebito,
                isValorCreditoError = erros.valorCredito,
            )
        }
        return erros.valido
    }

    /** Aplica os erros da validação pura do veículo no estado e devolve se ficou válido. */
    private fun aplicarErrosVeiculo(erros: ErrosVeiculo): Boolean {
        _uiStateVeiculo.update {
            it.copy(
                isDocumentoResponsavelRetiradaError = erros.documentoResponsavel,
                isTipoVeiculoError = erros.tipoVeiculo,
                isModeloVeiculoError = erros.modeloVeiculo,
                isPlacaVeiculoError = erros.placaVeiculo,
                isCilindradaError = erros.cilindrada,
            )
        }
        return erros.valido
    }

    /** Aplica os erros da validação pura do passageiro no estado e devolve se ficou válido. */
    private fun aplicarErrosPassageiro(erros: ErrosPassageiro): Boolean {
        _uiStatePassageiro.update {
            it.copy(
                isAcomodacaoError = erros.acomodacao,
                isTipoPassagemError = erros.tipoPassagem,
                isTipoGratuidadeError = erros.tipoGratuidade,
                isNomePassageiro1Error = erros.nomeP1,
                isDocumentoPassageiro1Error = erros.documentoP1,
                isDataNascimentoPassageiro1Error = erros.dataNascimentoP1,
                textDataNascimentoError = erros.textDataNascimentoP1,
                isNomePassageiro2Error = erros.nomeP2,
                isDocumentoPassageiro2Error = erros.documentoP2,
                isDataNascimentoPassageiro2Error = erros.dataNascimentoP2,
                isDocumentoPassageiro3Error = erros.documentoP3,
                isNomePassageiro3Error = erros.nomeP3,
                isDataNascimentoPassageiro3Error = erros.dataNascimentoP3,
            )
        }
        return erros.valido
    }

    // Eventos do sub-form de veículo (molde ADR-0006, §1b): o VM os expõe; delegam aos métodos do helper.
    fun onNomeResponsavelRetiradaChange(valor: String) = formVeiculoHelper.atualizarNomeResponsavelRetirada(valor)
    fun onTipoDocumentoResponsavelRetiradaChange(valor: String) = formVeiculoHelper.atualizarTipoDocumentoResponsavelRetirada(valor)
    fun onClickLimparTipoDocumentoResponsavelRetirada() = formVeiculoHelper.limparCamposDocumento()
    fun onDocumentoResponsavelRetiradaChange(valor: String) = formVeiculoHelper.atualizarDocumentoResponsavelRetirada(valor)
    fun onTipoVeiculoChange(valor: String) = formVeiculoHelper.atualizarTipoVeiculo(valor)
    fun onModeloVeiculoChange(valor: String) = formVeiculoHelper.atualizarModeloVeiculo(valor)
    fun onPlacaVeiculoChange(valor: String) = formVeiculoHelper.atualizarPlacaVeiculo(valor)
    fun onCorVeiculoChange(valor: String) = formVeiculoHelper.atualizaCorVeiculo(valor)
    fun onCilindradaChange(valor: String) = formVeiculoHelper.atualizarCilindrada(valor)

    // Eventos do sub-form de passageiro (molde ADR-0006, §1b): o VM os expõe; delegam ao helper.
    // onCheckPassageiroN ignora o Boolean do checkbox (o helper faz toggle) — preserva o comportamento.
    fun onAcomodacaoChange(valor: String) = formPassageiroHelper.atualizarAcomodacao(valor)
    fun onTipoPassagemChange(valor: String) = formPassageiroHelper.atualizarTipoPassagem(valor)
    fun onTipoGratuidadeChange(valor: String) = formPassageiroHelper.atualizarTipoGratuidade(valor)
    fun onNomePassageiro1Change(valor: String) = formPassageiroHelper.atualizarNomePassageiro1(valor)
    fun onTipoDocumentoPassageiro1Change(valor: String) = formPassageiroHelper.atualizarTipoDocumentoPassageiro1(valor)
    fun onClickLimparDocumentoPassageiro1() = formPassageiroHelper.limparDocumentoPassageiro1()
    fun onDocumentoPassageiro1Change(valor: String) = formPassageiroHelper.atualizaDocumentoPassageiro1(valor)
    fun onDataNascimentoPassageiro1Change(valor: String) = formPassageiroHelper.atualizarDataNascimentoPassageiro1(valor)
    fun onCheckPassageiro2(checked: Boolean) = formPassageiroHelper.checkPassageiro2()
    fun onNomePassageiro2Change(valor: String) = formPassageiroHelper.atualizarNomePassageiro2(valor)
    fun onTipoDocumentoPassageiro2Change(valor: String) = formPassageiroHelper.atualizarTipoDocumentoPassageiro2(valor)
    fun onClickLimparDocumentoPassageiro2() = formPassageiroHelper.limparDocumentoPassageiro2()
    fun onDocumentoPassageiro2Change(valor: String) = formPassageiroHelper.atualizaDocumentoPassageiro2(valor)
    fun onDataNascimentoPassageiro2Change(valor: String) = formPassageiroHelper.atualizarDataNascimentoPassageiro2(valor)
    fun onCheckPassageiro3(checked: Boolean) = formPassageiroHelper.checkPassageiro3()
    fun onNomePassageiro3Change(valor: String) = formPassageiroHelper.atualizarNomePassageiro3(valor)
    fun onTipoDocumentoPassageiro3Change(valor: String) = formPassageiroHelper.atualizarTipoDocumentoPassageiro3(valor)
    fun onClickLimparDocumentoPassageiro3() = formPassageiroHelper.limparDocumentoPassageiro3()
    fun onDocumentoPassageiro3Change(valor: String) = formPassageiroHelper.atualizaDocumentoPassageiro3(valor)
    fun onDataNascimentoPassageiro3Change(valor: String) = formPassageiroHelper.atualizarDataNascimentoPassageiro3(valor)

    // Eventos dos dados da passagem (molde ADR-0006, §1b): o VM os expõe; delegam ao helper.
    // onCheck* ignoram o Boolean do checkbox (o helper faz toggle) — preserva o comportamento.
    fun onCheckVeiculo(checked: Boolean) = formPassagemHelper.checkVeiculo()
    fun onDataViagemChange(valor: String) = formPassagemHelper.atualizarDataViagem(valor)
    fun onHoraViagemChange(valor: String) = formPassagemHelper.atualizarHoraViagem(valor)
    fun onCheckPix(checked: Boolean) = formPassagemHelper.checkPix()
    fun onCheckDinheiro(checked: Boolean) = formPassagemHelper.checkDinheiro()
    fun onCheckDebito(checked: Boolean) = formPassagemHelper.checkDebito()
    fun onCheckCredito(checked: Boolean) = formPassagemHelper.checkCredito()
    fun onValorPagoChange(valor: String) = formPassagemHelper.atualizarValorPago(valor)
    fun onValorPixChange(valor: String) = formPassagemHelper.atualizarValorPix(valor)
    fun onValorDinheiroChange(valor: String) = formPassagemHelper.atualizarValorDinheiro(valor)
    fun onValorDebitoChange(valor: String) = formPassagemHelper.atualizarValorDebito(valor)
    fun onValorCreditoChange(valor: String) = formPassagemHelper.atualizarValorCredito(valor)
    fun onObservacaoChange(obs: String, ehGravacao: Boolean) = formPassagemHelper.atualizaObservacao(obs, ehGravacao)

    // Eventos da área de agência: API pronta, porém a UI (área de agência) segue comentada até a rework
    // do agente (§6 do estudo). atualizarListaAgente ainda usa runBlocking — dívida a resolver nessa rework.
    fun onAgenciaChange(valor: String) {
        formPassagemHelper.atualizarListaAgente(valor)
        formPassagemHelper.atualizarAgencia(valor)
    }
    fun onAgenteChange(valor: String) = formPassagemHelper.atualizarAgente(valor)

    private fun limparStates() {
        formPassagemHelper.limparState()
        formPassageiroHelper.limparState()
        formVeiculoHelper.limparState()
    }
}
