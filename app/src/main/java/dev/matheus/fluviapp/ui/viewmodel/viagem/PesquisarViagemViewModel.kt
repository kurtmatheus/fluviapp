package dev.matheus.fluviapp.ui.viewmodel.viagem

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matheus.fluviapp.extensions.filtrarPor
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Categoria.MUNICIPIO
import dev.matheus.fluviapp.model.mappers.ViagemDadosViagemMapper
import dev.matheus.fluviapp.services.repository.cadastro.ConstanteRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.NavioRepository
import dev.matheus.fluviapp.services.repository.firebase.ViagemRepository
import dev.matheus.fluviapp.ui.states.PesquisarViagemUiState
import dev.matheus.fluviapp.ui.viewmodel.helpers.viagem.validarPesquisaViagem
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
 * Pesquisa de viagem no molde (ADR-0006): VM dona do estado (sem FormHelper), eventos como métodos,
 * validação pura, cargas suspensas. VM único escopado ao grafo (filtro/resultados/detalhes
 * compartilham a state). [nota: o fluxo de delete/detalhes é a fatia 2].
 */
@HiltViewModel
class PesquisarViagemViewModel @Inject constructor(
    private val empresaRepository: EmpresaRepository,
    private val navioRepository: NavioRepository,
    private val constanteRepository: ConstanteRepository,
    private val viagemRepository: ViagemRepository,
    private val viagemDadosViagemMapper: ViagemDadosViagemMapper,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PesquisarViagemUiState())
    val uiState: StateFlow<PesquisarViagemUiState> = _uiState.asStateFlow()

    private val _irParaResultados = Channel<Unit>(Channel.BUFFERED)
    val irParaResultados = _irParaResultados.receiveAsFlow()

    /** Resultado da exclusão (true = sucesso) — a nav decide toast/navegação. */
    private val _exclusao = Channel<Boolean>(Channel.BUFFERED)
    val exclusao = _exclusao.receiveAsFlow()

    init {
        carregarFontes()
    }

    private fun carregarFontes() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    listaEmpresas = empresaRepository.obterTodas(),
                    listaNavios = navioRepository.obterTodos(),
                    listaMunicipios = constanteRepository.obterTodosPorCategoria(MUNICIPIO.name),
                )
            }
        }
    }

    fun onCheckEmpresa() = _uiState.update {
        it.copy(isCheckedEmpresa = !it.isCheckedEmpresa, isEmpresaError = false, empresa = "")
    }

    fun onEmpresaChange(empresa: String) = _uiState.update { it.copy(empresa = empresa, isEmpresaError = false) }

    fun onCheckNavio() = _uiState.update {
        it.copy(isCheckedNavio = !it.isCheckedNavio, isNavioError = false, navio = "")
    }

    fun onNavioChange(navio: String) = _uiState.update { it.copy(navio = navio, isNavioError = false) }

    fun onCheckTrecho() = _uiState.update {
        it.copy(isCheckedTrecho = !it.isCheckedTrecho, isTrechoError = false, origem = "", destino = "")
    }

    fun onOrigemChange(origem: String) = _uiState.update { it.copy(origem = origem, isTrechoError = false) }

    fun onDestinoChange(destino: String) = _uiState.update { it.copy(destino = destino, isTrechoError = false) }

    fun pesquisar() {
        val erros = validarPesquisaViagem(_uiState.value)
        if (!erros.valido) {
            _uiState.update {
                it.copy(isEmpresaError = erros.empresa, isNavioError = erros.navio, isTrechoError = erros.trecho)
            }
            return
        }
        viewModelScope.launch {
            carregarViagensPesquisadas()
            _irParaResultados.send(Unit)
        }
    }

    private suspend fun carregarViagensPesquisadas() {
        val state = _uiState.value
        val cards = buildList {
            viagemRepository.obterTodas().forEach { add(viagemDadosViagemMapper.map(it)) }
        }

        var filtradas = filtrarPor(state.isCheckedEmpresa, cards) { it.empresa == state.empresa }
        filtradas = filtrarPor(state.isCheckedNavio, filtradas) { it.navio == state.navio }
        filtradas = filtrarPor(state.filtrarPorOrigem, filtradas) { it.origem == state.origem }
        filtradas = filtrarPor(state.filtrarPorDestino, filtradas) { it.destino == state.destino }
        filtradas = filtrarPor(state.filtrarPorOrigemDestino, filtradas) {
            it.destino == state.destino && it.origem == state.origem
        }

        _uiState.update { it.copy(listaResultadoViagens = filtradas) }
    }

    fun carregarDadosSelecionados(idViagem: String) = _uiState.update {
        it.copy(dadosViagemCard = it.listaResultadoViagens.first { card -> card.idViagem == idViagem })
    }

    fun exibirConfirmDeleteDialog() = _uiState.update { it.copy(isShowDeleteDialog = !it.isShowDeleteDialog) }

    fun deletarViagem(idViagem: String) {
        viewModelScope.launch {
            val sucesso = try {
                viagemRepository.deletar(idViagem)
                true
            } catch (e: Exception) {
                Log.e(TAG, "deletarViagem: ${e.message}", e)
                false
            }
            _uiState.update { it.copy(isShowDeleteDialog = false) }
            // Recarrega os resultados para o item deletado sumir da lista da pesquisa (editar/deletar
            // por item). Reaplica os filtros correntes sobre a lista já sem a viagem removida.
            if (sucesso) carregarViagensPesquisadas()
            _exclusao.send(sucesso) // a nav faz toast + navega (só no sucesso)
        }
    }

    private companion object {
        const val TAG = "pesquisarViagemViewModel"
    }
}
