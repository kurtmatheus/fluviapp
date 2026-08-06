package dev.matheus.fluviapp.ui.viewmodel.porto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.matheus.fluviapp.domain.localidade.Localidade
import dev.matheus.fluviapp.domain.porto.Porto
import dev.matheus.fluviapp.services.repository.cadastro.localidade.LocalidadeRepository
import dev.matheus.fluviapp.services.repository.cadastro.porto.PortoRepository
import dev.matheus.fluviapp.ui.states.PesquisaPortoUiState
import dev.matheus.fluviapp.ui.states.PortoResultado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Busca de portos — e a primeira tela do app que **junta duas coleções**.
 *
 * A junção é em memória, por `combine` das duas fontes reativas (ADR-0017 D1): as coleções da plataforma
 * são pequenas e vêm inteiras pelo listener, então resolver o `localidadeId` de cada linha custa uma
 * busca num mapa — não uma leitura por linha. É o que torna a referência por id (ADR-0008) barata aqui:
 * o preço de não copiar o rótulo dentro do porto é este `associateBy`.
 *
 * E é reativa dos dois lados. Corrigir a grafia de um município repinta a lista de portos sem que
 * ninguém recarregue nada — que é exatamente o ganho que a cópia embutida não teria.
 */
@HiltViewModel
class PesquisaPortoViewModel @Inject constructor(
    private val portoRepository: PortoRepository,
    private val localidadeRepository: LocalidadeRepository,
) : ViewModel() {

    private var portos: List<Porto> = emptyList()
    private var localidadesPorId: Map<String, Localidade> = emptyMap()

    private val _uiState = MutableStateFlow(PesquisaPortoUiState())
    val uiState: StateFlow<PesquisaPortoUiState> = _uiState.asStateFlow()

    init {
        // `observar*` é só a janela para o StateFlow: sem ligar os listeners, ninguém os alimenta.
        portoRepository.sincronizar()
        localidadeRepository.sincronizar()

        viewModelScope.launch {
            combine(
                portoRepository.observarTodos(),
                localidadeRepository.observarTodas(),
            ) { portos, localidades -> portos to localidades }
                .collect { (novosPortos, localidades) ->
                    portos = novosPortos
                    // Sem filtrar por `ativo`: aqui se **resolve por id**, e a localidade fora de uso
                    // continua sendo o lugar dos portos que a referenciam.
                    localidadesPorId = localidades.associateBy { it.id }
                    _uiState.update { it.copy(resultados = filtrar(it.nome)) }
                }
        }
    }

    fun onNomeChange(nome: String) = _uiState.update {
        it.copy(nome = nome, resultados = filtrar(nome))
    }

    /** Não recarrega: inativar emite um snapshot novo, e é por ele que a linha sai da lista. */
    fun onDeletar(id: String) {
        viewModelScope.launch { portoRepository.deletar(id) }
    }

    private fun filtrar(nome: String): List<PortoResultado> = portos
        .filter { it.ativo }
        .filter { nome.isBlank() || it.nome.startsWith(nome, ignoreCase = true) }
        .map {
            PortoResultado(
                id = it.id,
                nome = it.nome,
                // Localidade que ainda não chegou (ou que sumiu) deixa a linha **sem lugar**, e não com
                // um lugar inventado: melhor a ausência visível do que um rótulo que mente.
                rotuloLocalidade = localidadesPorId[it.localidadeId]?.rotulo.orEmpty(),
            )
        }
}