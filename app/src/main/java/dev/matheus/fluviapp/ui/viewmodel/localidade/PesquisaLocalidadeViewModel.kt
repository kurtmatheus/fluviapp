package dev.matheus.fluviapp.ui.viewmodel.localidade

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.matheus.fluviapp.domain.localidade.Localidade
import dev.matheus.fluviapp.services.repository.cadastro.localidade.LocalidadeRepository
import dev.matheus.fluviapp.ui.states.LocalidadeResultado
import dev.matheus.fluviapp.ui.states.PesquisaLocalidadeUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Busca de localidades — uma fonte reativa (ADR-0017 D1) e um filtro por texto do município.
 *
 * **É aqui que o delete lógico se cumpre**: a lista mostra só as ativas. O repositório entrega tudo de
 * propósito (a inativa precisa continuar resolvível por id, senão o porto histórico apontaria para o
 * nada), e o filtro é responsabilidade de quem lista — como a porta documenta.
 */
@HiltViewModel
class PesquisaLocalidadeViewModel @Inject constructor(
    private val localidadeRepository: LocalidadeRepository,
) : ViewModel() {

    private var localidades: List<Localidade> = emptyList()

    private val _uiState = MutableStateFlow(PesquisaLocalidadeUiState())
    val uiState: StateFlow<PesquisaLocalidadeUiState> = _uiState.asStateFlow()

    init {
        // `observarTodas` é só a janela para o StateFlow: sem ligar o listener, ninguém o alimenta.
        localidadeRepository.sincronizar()

        viewModelScope.launch {
            localidadeRepository.observarTodas().collect { lista ->
                localidades = lista
                _uiState.update { it.copy(resultados = filtrar(it.municipio)) }
            }
        }
    }

    fun onMunicipioChange(municipio: String) = _uiState.update {
        it.copy(municipio = municipio, resultados = filtrar(municipio))
    }

    /** Não recarrega: inativar emite um snapshot novo, e é por ele que a linha sai da lista. */
    fun onDeletar(id: String) {
        viewModelScope.launch { localidadeRepository.deletar(id) }
    }

    private fun filtrar(municipio: String): List<LocalidadeResultado> = localidades
        .filter { it.ativo }
        .filter { municipio.isBlank() || it.municipio.startsWith(municipio, ignoreCase = true) }
        .map { LocalidadeResultado(id = it.id, rotulo = it.rotulo, codigoIbge = it.codigoIbge) }
}