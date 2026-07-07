package br.com.gruponaveg.ui.viewmodel.passagem

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.gruponaveg.R
import br.com.gruponaveg.extensions.isTextoNaoNulo
import br.com.gruponaveg.extensions.toastMessage
import br.com.gruponaveg.model.passagem.Passagem
import br.com.gruponaveg.navigation.navcomposables.passagem.EDIT_PASSAGEM_ARGUMENT
import br.com.gruponaveg.navigation.navcomposables.passagem.FORM_PASSAGEM_ARGUMENT
import br.com.gruponaveg.services.repository.cadastro.ConstanteRepository
import br.com.gruponaveg.services.repository.cadastro.passagem.AgenteRepository
import br.com.gruponaveg.services.repository.firebase.PassagemFirestoreRepository
import br.com.gruponaveg.services.repository.firebase.ViagemFirestoreRepository
import br.com.gruponaveg.services.repository.operacoes.UsuarioRepository
import br.com.gruponaveg.ui.states.passagem.FormPassageiroUiState
import br.com.gruponaveg.ui.states.passagem.FormPassagemUiState
import br.com.gruponaveg.ui.states.passagem.FormVeiculoUiState
import br.com.gruponaveg.ui.viewmodel.helpers.passagem.FormPassageiroHelper
import br.com.gruponaveg.ui.viewmodel.helpers.passagem.FormPassagemHelper
import br.com.gruponaveg.ui.viewmodel.helpers.passagem.FormVeiculoHelper
import br.com.gruponaveg.ui.viewmodel.helpers.passagem.validacao.ValidacaoFormPassageiroHelper
import br.com.gruponaveg.ui.viewmodel.helpers.passagem.validacao.ValidacaoFormPassagemHelper
import br.com.gruponaveg.ui.viewmodel.helpers.passagem.validacao.ValidacaoFormVeiculoHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private lateinit var validacaoFormPassagemHelper: ValidacaoFormPassagemHelper
    private lateinit var validacaoFormPassageiroHelper: ValidacaoFormPassageiroHelper
    private lateinit var validacaoFormVeiculoHelper: ValidacaoFormVeiculoHelper

    init {
        viewModelScope.launch {
            inicializarHelpers()
            preencherViagem()
            inicializarEditor()
        }
    }

    private fun inicializarHelpers() {
        formPassagemHelper = FormPassagemHelper(
            uiStatePassagem = _uiStatePassagem,
            uiStatePassageiro = _uiStatePassageiro,
            uiStateVeiculo = _uiStateVeiculo,
            constanteRepository = constanteRepository,
            viagemRepository = viagemRepository,
            agenteRepository = agenteRepository,
            passagemRepository = passagemRepository
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
        validacaoFormPassagemHelper = ValidacaoFormPassagemHelper(
            uiState = _uiStatePassagem,
            uiStatePassageiro = _uiStatePassageiro
        )
        validacaoFormPassageiroHelper = ValidacaoFormPassageiroHelper(
            uiState = _uiStatePassageiro,
            uiStatePassagem = _uiStatePassagem
        )
        validacaoFormVeiculoHelper = ValidacaoFormVeiculoHelper(
            uiState = _uiStateVeiculo
        )
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
        val usuarioLogado = usuarioRepository.obterUltimoUsuarioLogado()

        return usuarioLogado?.let {
            val id = formPassagemHelper.salvarPassagem(
                idPassagem = idPassagem,
                funcionarioResponsavel = it.nome
            )
            limparStates()
            context.toastMessage(context.resources.getString(R.string.msg_transmissao_passagem))
            id
        }
    }

    fun validarFormularios(): Boolean {
        val formPassagemvalido = validacaoFormPassagemHelper.isFormularioPassagemValido()
        val formPassageiroVeiculoValido = if (_uiStatePassagem.value.isVeiculoChecked) {
            validacaoFormVeiculoHelper.isFormularioVeiculoValido()
        } else {
            validacaoFormPassageiroHelper.isFormularioPassageiroValido()
        }

        return formPassagemvalido && formPassageiroVeiculoValido
    }

    private fun limparStates() {
        formPassagemHelper.limparState()
        formPassageiroHelper.limparState()
        formVeiculoHelper.limparState()
    }
}
