package dev.matheus.fluviapp.ui.viewmodel.viagem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.matheus.fluviapp.domain.operacoes.PermissoesUsuario
import dev.matheus.fluviapp.domain.rota.Rota
import dev.matheus.fluviapp.domain.viagem.EscopoDoPool
import dev.matheus.fluviapp.domain.viagem.Viagem
import dev.matheus.fluviapp.domain.viagem.chegadaEstimada
import dev.matheus.fluviapp.domain.viagem.formatarHora
import dev.matheus.fluviapp.domain.viagem.noEscopo
import dev.matheus.fluviapp.domain.viagem.rotulo
import dev.matheus.fluviapp.domain.viagem.rotuloCurto
import dev.matheus.fluviapp.services.repository.cadastro.localidade.LocalidadeRepository
import dev.matheus.fluviapp.services.repository.cadastro.porto.PortoRepository
import dev.matheus.fluviapp.services.repository.cadastro.rota.RotaRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmbarcacaoRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.ViagemRepository
import dev.matheus.fluviapp.services.repository.operacoes.EscopoDaSessao
import dev.matheus.fluviapp.services.repository.operacoes.SessaoUsuario
import dev.matheus.fluviapp.ui.states.PesquisaViagemUiState
import dev.matheus.fluviapp.ui.states.ViagemResultado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Busca de viagens — o pool **recortado pela atuação** de quem olha.
 *
 * É onde a decisão do analista de 2026-08-10 aparece em tela: a plataforma vê o pool inteiro (é ela quem o
 * cura, e o que ela não vê, não conserta); a empresa vê o que a concessão dela alcança. Ver e vender
 * viraram a mesma pergunta, então a lista **já é** o que se pode ofertar.
 *
 * Como a Rota, ela mostra **também as inativas**, marcadas: o descartado é registro, e um pool em que só
 * se vê o que está em uso não responde por que um bilhete antigo aponta para onde aponta.
 */
@HiltViewModel
class PesquisaViagemViewModel @Inject constructor(
    private val viagemRepository: ViagemRepository,
    private val rotaRepository: RotaRepository,
    private val embarcacaoRepository: EmbarcacaoRepository,
    private val portoRepository: PortoRepository,
    private val localidadeRepository: LocalidadeRepository,
    private val escopoDaSessao: EscopoDaSessao,
    private val sessaoUsuario: SessaoUsuario,
) : ViewModel() {

    private var viagens: List<Viagem> = emptyList()
    private var rotasPorId: Map<String, Rota> = emptyMap()
    private var rotulosDeRota: Map<String, String> = emptyMap()
    private var embarcacoesPorId: Map<String, String> = emptyMap()

    private val _uiState = MutableStateFlow(PesquisaViagemUiState())
    val uiState: StateFlow<PesquisaViagemUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val contexto = sessaoUsuario.atual()
            _uiState.update {
                it.copy(podeInativar = PermissoesUsuario.podeInativarViagem(contexto?.papel))
            }
            carregar()
        }
    }

    fun onFiltroChange(filtro: String) = _uiState.update {
        it.copy(filtro = filtro, resultados = filtrar(filtro))
    }

    fun onInativar(id: String) {
        // Segunda barreira do mesmo recorte: a tela esconde o botão, e o VM também recusa — tirar do ar
        // uma viagem compartilhada atinge bilhetes de quem nem sabe que ela existe.
        if (!_uiState.value.podeInativar) return
        viewModelScope.launch {
            viagemRepository.inativar(id)
            carregar()
        }
    }

    /**
     * Quatro leituras, e a ordem entre elas não importa — mas o **recorte** só pode acontecer depois das
     * rotas: são elas que trazem os portos, e é por eles que a concessão responde.
     */
    private suspend fun carregar() {
        val escopo = escopoDaSessao.atual()

        val localidades = localidadeRepository.obterTodas().associate { it.id to it.rotulo }
        val portosPorId = portoRepository.obterTodos().associate { it.id to it.rotuloCom(localidades) }

        rotasPorId = rotaRepository.obterTodas().associateBy { it.id }
        rotulosDeRota = rotasPorId.mapValues { (_, rota) -> rota.rotuloCom(portosPorId) }
        embarcacoesPorId = embarcacaoRepository.obterTodos().associate { it.id to it.descricaoNome }

        viagens = viagemRepository.obterTodas().noEscopo(escopo, rotasPorId)

        _uiState.update {
            it.copy(resultados = filtrar(it.filtro), semConcessao = semConcessao(escopo))
        }
    }

    /**
     * Distingue **"não recebeu nada"** de **"não há viagem no que recebeu"** — a primeira se resolve com a
     * plataforma, a segunda com o botão de criar, e a mesma tela vazia para as duas mandaria a pessoa
     * para o lugar errado.
     *
     * Basta olhar as **rotas**: sem rota no escopo, nenhuma viagem pode estar nele. E a plataforma nunca
     * está sem concessão — quando ela vê a lista vazia, é porque o pool está vazio mesmo.
     */
    private fun semConcessao(escopo: EscopoDoPool): Boolean = when (escopo) {
        EscopoDoPool.Todo -> false
        EscopoDoPool.Nenhum -> true
        is EscopoDoPool.Concedido -> rotasPorId.values.toList().noEscopo(escopo).isEmpty()
    }

    /** Casa o texto contra a rota inteira e contra a embarcação — é como se procura uma saída. */
    private fun filtrar(filtro: String): List<ViagemResultado> = viagens
        .map { viagem ->
            val rota = rotasPorId[viagem.rotaId]
            val chegada = rota?.let { viagem.chegadaEstimada(it) }

            ViagemResultado(
                id = viagem.id,
                rota = rotulosDeRota[viagem.rotaId] ?: viagem.rotaId,
                embarcacao = embarcacoesPorId[viagem.embarcacaoId] ?: viagem.embarcacaoId,
                partida = "${viagem.diaSemana.rotulo} · ${formatarHora(viagem.horaMin)}",
                // O dia só entra quando a travessia o atravessa: repeti-lo numa viagem que chega no mesmo
                // dia seria ruído, e omiti-lo numa que não chega seria engano.
                chegada = chegada?.let {
                    if (it.diasDepois > 0) "${it.diaSemana.rotuloCurto} ${formatarHora(it.horaMin)}"
                    else formatarHora(it.horaMin)
                }.orEmpty(),
                ativa = viagem.ativo,
            )
        }
        .filter {
            filtro.isBlank() ||
                it.rota.contains(filtro, ignoreCase = true) ||
                it.embarcacao.contains(filtro, ignoreCase = true)
        }
}