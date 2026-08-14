package dev.matheus.fluviapp.ui.viewmodel.passagem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.matheus.fluviapp.domain.operacoes.PermissoesUsuario
import dev.matheus.fluviapp.domain.operacoes.PermissoesUsuario.EscopoEmpresa
import dev.matheus.fluviapp.domain.passagem.CategoriaPassagem
import dev.matheus.fluviapp.domain.passagem.StatusPassagem
import dev.matheus.fluviapp.services.repository.operacoes.SessaoUsuario
import dev.matheus.fluviapp.services.repository.passagem.CriterioPassagem
import dev.matheus.fluviapp.services.repository.passagem.PassagemRepository
import dev.matheus.fluviapp.services.repository.passagem.RecorteTemporal
import dev.matheus.fluviapp.ui.states.passagem.PesquisaPassagemUiState
import dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.paraLista
import dev.matheus.fluviapp.util.Relogio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * **A busca de bilhetes** — e é ela que faz o **recorte por agência** sair do papel (F9.6).
 *
 * O escopo vem da política (`PermissoesUsuario.escopoDeEmpresa`), não de um campo: papel de plataforma
 * atravessa empresas, vínculo vê a sua, e **sem vínculo não se vê nada** — o terceiro caso é o perigoso, e é
 * por ele que o `CriterioPassagem` faz *"sem empresa"* virar **não consultar**, em vez de consultar sem
 * filtro ([ADR-0025] D2).
 *
 * A lista **não lê os pools**: nome e placa não entram na projeção ([PassagemNaLista]), então cinquenta
 * bilhetes custam uma consulta e nenhuma leitura de dado pessoal. Quem precisa do nome abre o bilhete.
 */
@HiltViewModel
class PesquisaPassagemViewModel @Inject constructor(
    private val passagemRepository: PassagemRepository,
    private val sessaoUsuario: SessaoUsuario,
    relogio: Relogio,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PesquisaPassagemUiState(data = relogio.agora().toLocalDate()))
    val uiState: StateFlow<PesquisaPassagemUiState> = _uiState.asStateFlow()

    init {
        // A tela abre já respondendo à pergunta mais comum: os bilhetes de hoje.
        buscar()
    }

    fun escolherData(data: LocalDate) {
        _uiState.update { it.copy(data = data) }
        buscar()
    }

    /** Tocar no status já marcado **desmarca** — o filtro é um estreitamento, e desfazê-lo é um toque só. */
    fun alternarStatus(status: StatusPassagem) {
        _uiState.update { it.copy(status = if (it.status == status) null else status) }
        buscar()
    }

    fun alternarCategoria(categoria: CategoriaPassagem) {
        _uiState.update { it.copy(categoria = if (it.categoria == categoria) null else categoria) }
        buscar()
    }

    fun buscar() {
        _uiState.update { it.copy(buscando = true) }

        viewModelScope.launch {
            val contexto = sessaoUsuario.atual()
            val escopo = PermissoesUsuario.escopoDeEmpresa(contexto?.papel, contexto?.vinculoAtivo)

            if (escopo is EscopoEmpresa.Nenhuma) {
                _uiState.update {
                    it.copy(buscando = false, buscou = true, semEscopo = true, resultados = emptyList())
                }
                return@launch
            }

            val estado = _uiState.value
            val encontradas = runCatching {
                passagemRepository.consultar(
                    CriterioPassagem(
                        recorte = RecorteTemporal.Dia(estado.data),
                        escopo = escopo,
                        status = estado.status,
                        categoria = estado.categoria,
                    ),
                )
            }.getOrDefault(emptyList())

            _uiState.update {
                it.copy(
                    buscando = false,
                    buscou = true,
                    semEscopo = false,
                    // Mais recente primeiro: numa lista de bilhetes do dia, o que interessa é o que acabou
                    // de sair — e o `criadoEm` é ISO, então ordenar texto é ordenar tempo (ADR-0024 D2).
                    resultados = encontradas
                        .sortedByDescending { passagem -> passagem.metadados.criadoEm }
                        .map { passagem -> passagem.paraLista() },
                )
            }
        }
    }
}