package dev.matheus.fluviapp.ui.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matheus.fluviapp.extensions.isTextoNaoNulo
import dev.matheus.fluviapp.navigation.navcomposables.agente.ID_AGENTE_ARGUMENT
import dev.matheus.fluviapp.services.repository.cadastro.ConstanteRepository
import dev.matheus.fluviapp.services.repository.cadastro.passagem.AgenteRepository
import dev.matheus.fluviapp.ui.states.AgenteUiState
import dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.FormAgenteHelper
import dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.validacao.ValidacaoFormAgenteHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AgenteViewModel @Inject constructor(
    private val repository: AgenteRepository,
    private val constanteRepository: ConstanteRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AgenteUiState())

    val uiState: StateFlow<AgenteUiState>
        get() = _uiState.asStateFlow()

    lateinit var formAgenteHelper: FormAgenteHelper
    lateinit var validaFormAgenteHelper: ValidacaoFormAgenteHelper

    private val idAgente: String = checkNotNull(savedStateHandle[ID_AGENTE_ARGUMENT])

    init {
        viewModelScope.launch {
            inicializarHelper()
            preencherEditor()
        }
    }

    private fun inicializarHelper() {
        formAgenteHelper = FormAgenteHelper(
            uiState = _uiState,
            repository = repository,
            constanteRepository = constanteRepository
        )
        validaFormAgenteHelper = ValidacaoFormAgenteHelper(
            uiState = _uiState
        )
    }

    private suspend fun preencherEditor() {
        if (idAgente.isTextoNaoNulo()) {
            formAgenteHelper.preencherCampos(idAgente)
        }
    }

    suspend fun salvar(context: Context) {
        formAgenteHelper.salvar(idAgente, context)
    }
}
