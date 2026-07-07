package dev.matheus.fluviapp.ui.viewmodel.viagem

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.extensions.isTextoNaoNulo
import dev.matheus.fluviapp.model.mappers.ViagemDadosViagemMapper
import dev.matheus.fluviapp.navigation.navcomposables.viagem.ID_VIAGEM_ARGUMENT
import dev.matheus.fluviapp.services.repository.cadastro.ConstanteRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.NavioRepository
import dev.matheus.fluviapp.services.repository.firebase.ViagemFirestoreRepository
import dev.matheus.fluviapp.ui.states.FormViagemUiState
import dev.matheus.fluviapp.ui.viewmodel.helpers.viagem.FormViagemHelper
import dev.matheus.fluviapp.ui.viewmodel.helpers.viagem.ValidacaoFormViagemHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FormViagemViewModel @Inject constructor(
    private val empresaRepository: EmpresaRepository,
    private val navioRepository: NavioRepository,
    private val constanteRepository: ConstanteRepository,
    private val viagemRepository: ViagemFirestoreRepository,
    private val viagemDadosViagemMapper: ViagemDadosViagemMapper,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(FormViagemUiState())
    val uiState: StateFlow<FormViagemUiState>
        get() = _uiState.asStateFlow()

    internal lateinit var formViagemHelper: FormViagemHelper
    lateinit var validacaoFormViagemHelper: ValidacaoFormViagemHelper


    private val idViagem: String = checkNotNull(savedStateHandle[ID_VIAGEM_ARGUMENT])

    init {
        viewModelScope.launch {
            inicializarHelper()
            inicializarEditor()
        }
    }

    private fun inicializarHelper() {
        formViagemHelper = FormViagemHelper(
            uiState = _uiState,
            empresaRepository = empresaRepository,
            navioRepository = navioRepository,
            constanteRepository = constanteRepository,
            viagemRepository = viagemRepository
        )
        validacaoFormViagemHelper = ValidacaoFormViagemHelper(
            uiState = _uiState
        )
    }

    private suspend fun inicializarEditor() {
        if (idViagem.isTextoNaoNulo()) {
            val viagem = viagemRepository.obterPorId(idViagem)
            val viagemCard = viagemDadosViagemMapper.map(viagem)

            _uiState.update {
                it.copy(
                    empresa = viagemCard.empresa,
                    navio = viagemCard.navio,
                    trechoOrigem = viagemCard.origem,
                    trechoDestino = viagemCard.destino,
                    titleJanela = R.string.subtitle_editar_viagem,
                    isTrechoDestinoDisabled = it.trechoOrigem.isNotBlank()
                )
            }
        }
    }

    fun salvarViagem(context: Context) {
        formViagemHelper.atualizaIsProcessando()
        viewModelScope.launch { formViagemHelper.salvarViagem(idViagem, context) }
    }
}
