package dev.matheus.fluviapp.ui.viewmodel.viagem

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.extensions.filtrarPor
import dev.matheus.fluviapp.extensions.toastMessage
import dev.matheus.fluviapp.model.mappers.ViagemDadosViagemMapper
import dev.matheus.fluviapp.model.screendata.DadosViagemCard
import dev.matheus.fluviapp.services.repository.cadastro.ConstanteRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.NavioRepository
import dev.matheus.fluviapp.services.repository.firebase.ViagemFirestoreRepository
import dev.matheus.fluviapp.ui.states.PesquisarViagemUiState
import dev.matheus.fluviapp.ui.viewmodel.helpers.viagem.FormPesquisarViagemHelper
import dev.matheus.fluviapp.ui.viewmodel.helpers.viagem.ValidacaoFormPesquisarViagemHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PesquisarViagemViewModel @Inject constructor(
    private val empresaRepository: EmpresaRepository,
    private val navioRepository: NavioRepository,
    private val constanteRepository: ConstanteRepository,
    private val viagemRepository: ViagemFirestoreRepository,
    private val viagemDadosViagemMapper: ViagemDadosViagemMapper
) : ViewModel() {

    private val _uiState = MutableStateFlow(PesquisarViagemUiState())

    val uiState: StateFlow<PesquisarViagemUiState>
        get() = _uiState.asStateFlow()

    private lateinit var formPesquisarViagemHelper: FormPesquisarViagemHelper
    lateinit var validacaoFormPesquisarViagemHelper: ValidacaoFormPesquisarViagemHelper

    lateinit var onNavegaParaMainScreen: () -> Unit

    init {
        viewModelScope.launch {
            inicializarHelpers()
        }
    }

    private fun inicializarHelpers() {
        formPesquisarViagemHelper = FormPesquisarViagemHelper(
            uiState = _uiState,
            empresaRepository = empresaRepository,
            navioRepository = navioRepository,
            constanteRepository = constanteRepository
        )
        validacaoFormPesquisarViagemHelper = ValidacaoFormPesquisarViagemHelper(
            uiState = _uiState
        )
    }

    suspend fun carregarViagensPesquisadas() {
        val state = _uiState.value
        var viagemListaFiltered: List<DadosViagemCard>
        val viagemCardList = viagemRepository.obterTodas().map {
            viagemDadosViagemMapper.map(it)
        }

        viagemListaFiltered = filtrarPor(state.isCheckedEmpresa, viagemCardList) { it.empresa == state.empresa }
        viagemListaFiltered = filtrarPor(state.isCheckedNavio, viagemListaFiltered) { it.navio == state.navio }
        viagemListaFiltered = filtrarPor(state.filtrarPorOrigem, viagemListaFiltered) { (it.origem == state.origem) }
        viagemListaFiltered = filtrarPor(state.filtrarPorDestino, viagemListaFiltered) { (it.destino == state.destino) }
        viagemListaFiltered = filtrarPor(state.filtrarPorOrigemDestino, viagemListaFiltered) {
            (it.destino == state.destino) && (it.origem == state.origem)
        }

        _uiState.update {
            it.copy(
                listaResultadoViagens = viagemListaFiltered
            )
        }
    }

    fun exibirConfirmDeleteDialog() {
        formPesquisarViagemHelper.exibirConfirmDeleteDialog()
    }

    suspend fun deletarViagem(idViagem: String, context: Context) {
        try {
            viagemRepository.deletar(idViagem)
        } catch (e: Exception) {
            context.toastMessage(context.resources.getString(R.string.error_transmissao_exclusao))
        } finally {
            context.toastMessage(context.resources.getString(R.string.msg_exclusao_viagem))
            exibirConfirmDeleteDialog()
            onNavegaParaMainScreen()
        }
    }

    fun carregarDadosSelecionados(idViagem: String) {
        formPesquisarViagemHelper.atualizarDadosViagemCard(
            uiState.value.listaResultadoViagens.first { it.idViagem == idViagem }
        )
    }
}
