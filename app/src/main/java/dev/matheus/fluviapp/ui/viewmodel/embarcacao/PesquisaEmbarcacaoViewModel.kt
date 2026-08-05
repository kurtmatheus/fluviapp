package dev.matheus.fluviapp.ui.viewmodel.embarcacao

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matheus.fluviapp.domain.viagem.Empresa
import dev.matheus.fluviapp.domain.viagem.Embarcacao
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmbarcacaoRepository
import dev.matheus.fluviapp.ui.states.EmbarcacaoResultado
import dev.matheus.fluviapp.ui.states.PesquisaEmbarcacaoUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Busca de embarcações — filtro único por empresa (dropdown). Resolve o nome da empresa a partir do
 * `empresaId` (ADR-0008) contra a lista de empresas; filtro e resolução ficam no VM.
 *
 * **Duas fontes reativas** (ADR-0017 D1), e as duas importam: a lista de embarcações e a de empresas.
 * Cadastrar uma embarcação e voltar para cá atualiza sozinho; cadastrar uma **empresa** também, e é o
 * caso menos óbvio — sem ele, o dropdown de filtro ficaria sem a empresa recém-criada até alguém
 * recarregar a tela.
 */
@HiltViewModel
class PesquisaEmbarcacaoViewModel @Inject constructor(
    private val embarcacaoRepository: EmbarcacaoRepository,
    private val empresaRepository: EmpresaRepository,
) : ViewModel() {

    private var embarcacoes: List<Embarcacao> = emptyList()
    private var empresas: List<Empresa> = emptyList()

    private val _uiState = MutableStateFlow(PesquisaEmbarcacaoUiState())
    val uiState: StateFlow<PesquisaEmbarcacaoUiState> = _uiState.asStateFlow()

    init {
        // `observarTodas` é só a janela para o StateFlow: sem ligar o listener, ninguém o alimenta.
        embarcacaoRepository.sincronizar()
        empresaRepository.sincronizar()

        viewModelScope.launch {
            embarcacaoRepository.observarTodas().collect { lista ->
                embarcacoes = lista
                recalcular()
            }
        }
        viewModelScope.launch {
            empresaRepository.observarTodas().collect { lista ->
                empresas = lista
                recalcular()
            }
        }
    }

    fun onEmpresaChange(empresa: String) = _uiState.update {
        it.copy(empresa = empresa, resultados = filtrar(empresa))
    }

    /** Não recarrega: apagar emite um snapshot novo, e é por ele que a lista encolhe. */
    fun onDeletar(id: String) {
        viewModelScope.launch { embarcacaoRepository.deletar(id) }
    }

    private fun recalcular() {
        _uiState.update {
            it.copy(
                listaEmpresas = empresas.map { empresa -> empresa.nome },
                resultados = filtrar(it.empresa),
            )
        }
    }

    private fun filtrar(empresaNome: String): List<EmbarcacaoResultado> {
        val idSelecionado = empresas.find { it.nome.equals(empresaNome, ignoreCase = true) }?.id
        return embarcacoes
            .filter { empresaNome.isBlank() || it.empresaId == idSelecionado }
            .map { embarcacao ->
                EmbarcacaoResultado(
                    id = embarcacao.id,
                    nome = embarcacao.descricaoNome,
                    tipo = embarcacao.tipo.rotulo,
                    empresaNome = empresas.find { it.id == embarcacao.empresaId }?.nome.orEmpty(),
                )
            }
    }
}
