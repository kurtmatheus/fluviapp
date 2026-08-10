package dev.matheus.fluviapp.ui.viewmodel.rota

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.matheus.fluviapp.domain.operacoes.PermissoesUsuario
import dev.matheus.fluviapp.domain.porto.Porto
import dev.matheus.fluviapp.domain.rota.Rota
import dev.matheus.fluviapp.domain.viagem.EscopoDoPool
import dev.matheus.fluviapp.domain.viagem.noEscopo
import dev.matheus.fluviapp.services.repository.cadastro.localidade.LocalidadeRepository
import dev.matheus.fluviapp.services.repository.cadastro.porto.PortoRepository
import dev.matheus.fluviapp.services.repository.cadastro.rota.RotaRepository
import dev.matheus.fluviapp.services.repository.operacoes.EscopoDaSessao
import dev.matheus.fluviapp.services.repository.operacoes.SessaoUsuario
import dev.matheus.fluviapp.ui.states.PesquisaRotaUiState
import dev.matheus.fluviapp.ui.states.RotaResultado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Busca de rotas — o pool compartilhado **recortado pela atuação** de quem olha.
 *
 * Três coisas a separam das outras buscas do app:
 *
 * - o recorte por concessão (F8.3, alinhando a Rota à Viagem): a empresa vê as ligações entre portos que
 *   lhe foram concedidos; a plataforma vê o pool inteiro, porque é ela quem o cura. *A versão da F7
 *   mostrava tudo a todos e falava numa "lista de negadas" — ela não existe mais: com a visualização já
 *   recortada, não sobrou trabalho para uma deny-list;*
 * - ela mostra **também as inativas**, marcadas. O descartado é registro (§7.1): um pool em que só se
 *   vê o que está em uso não responde por que um bilhete antigo aponta para onde aponta;
 * - **inativar é ato de plataforma** (ADR-0022 D3). Tirar uma rota do pool afeta quem nem sabe que ela
 *   existe — é o único poder deste conjunto que atinge terceiros.
 */
@HiltViewModel
class PesquisaRotaViewModel @Inject constructor(
    private val rotaRepository: RotaRepository,
    private val portoRepository: PortoRepository,
    private val localidadeRepository: LocalidadeRepository,
    private val escopoDaSessao: EscopoDaSessao,
    private val sessaoUsuario: SessaoUsuario,
) : ViewModel() {

    private var rotas: List<Rota> = emptyList()
    private var portosPorId: Map<String, String> = emptyMap()

    private val _uiState = MutableStateFlow(PesquisaRotaUiState())
    val uiState: StateFlow<PesquisaRotaUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val contexto = sessaoUsuario.atual()
            _uiState.update {
                it.copy(podeInativar = PermissoesUsuario.podeInativarRota(contexto?.papel))
            }
            carregar()
        }
    }

    fun onPortoChange(porto: String) = _uiState.update {
        it.copy(porto = porto, resultados = filtrar(porto))
    }

    fun onInativar(id: String) {
        // Segunda barreira do mesmo recorte: a tela esconde o botão, e o VM também recusa — inativar
        // uma rota compartilhada não é reversível pela pessoa que a inativou.
        if (!_uiState.value.podeInativar) return
        viewModelScope.launch {
            rotaRepository.inativar(id)
            carregar()
        }
    }

    private suspend fun carregar() {
        val escopo = escopoDaSessao.atual()

        val localidades = localidadeRepository.obterTodas().associate { it.id to it.rotulo }
        portosPorId = portoRepository.obterTodos().associate { it.id to it.rotuloCom(localidades) }
        rotas = rotaRepository.obterTodas().noEscopo(escopo)

        _uiState.update {
            it.copy(
                resultados = filtrar(it.porto),
                // Distingue "não recebeu porto nenhum" de "não há rota entre os que recebeu": a primeira
                // se resolve com a plataforma, a segunda com o botão de criar.
                semConcessao = escopo == EscopoDoPool.Nenhum,
            )
        }
    }

    /** Casa o texto contra **os dois** portos: "o que liga daqui" inclui chegar aqui. */
    private fun filtrar(porto: String): List<RotaResultado> = rotas
        .map { rota ->
            RotaResultado(
                id = rota.id,
                origem = portosPorId[rota.portoOrigemId] ?: rota.portoOrigemId,
                destino = portosPorId[rota.portoDestinoId] ?: rota.portoDestinoId,
                medidas = "${rota.distanciaMn} mn · ${rota.tempoMedioH} h",
                ativa = rota.ativo,
            )
        }
        .filter {
            porto.isBlank() ||
                it.origem.contains(porto, ignoreCase = true) ||
                it.destino.contains(porto, ignoreCase = true)
        }
}

/** O rótulo do porto para a rota: nome e cidade, que é o que distingue homônimos. */
private fun Porto.rotuloCom(localidades: Map<String, String>): String =
    listOfNotNull(nome, localidades[localidadeId]).filter { it.isNotBlank() }.joinToString(" · ")