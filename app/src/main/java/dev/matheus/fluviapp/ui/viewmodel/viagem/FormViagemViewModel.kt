package dev.matheus.fluviapp.ui.viewmodel.viagem

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.matheus.fluviapp.domain.porto.Porto
import dev.matheus.fluviapp.domain.rota.Rota
import dev.matheus.fluviapp.domain.viagem.DIAS_DA_SEMANA
import dev.matheus.fluviapp.domain.viagem.concedeu
import dev.matheus.fluviapp.domain.viagem.digitosDaHora
import dev.matheus.fluviapp.domain.viagem.Viagem
import dev.matheus.fluviapp.domain.viagem.noEscopo
import dev.matheus.fluviapp.domain.viagem.rotulo
import dev.matheus.fluviapp.services.repository.cadastro.localidade.LocalidadeRepository
import dev.matheus.fluviapp.services.repository.cadastro.porto.PortoRepository
import dev.matheus.fluviapp.services.repository.cadastro.rota.RotaRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmbarcacaoRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.ViagemRepository
import dev.matheus.fluviapp.services.repository.operacoes.EscopoDaSessao
import dev.matheus.fluviapp.services.repository.operacoes.SessaoUsuario
import dev.matheus.fluviapp.ui.states.EmbarcacaoOpcao
import dev.matheus.fluviapp.ui.states.ErroHoraViagem
import dev.matheus.fluviapp.ui.states.FormViagemUiState
import dev.matheus.fluviapp.ui.states.RotaOpcao
import dev.matheus.fluviapp.ui.viewmodel.helpers.viagem.chaveDaViagem
import dev.matheus.fluviapp.ui.viewmodel.helpers.viagem.validarViagem
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * **Criação** de viagem (F8.2) — e só criação: a viagem é imutável (ADR-0016 §7.1), e aqui o argumento é
 * mais forte que na rota, porque é ela que a passagem aponta.
 *
 * **O formulário só oferece o concedido** (decisão do analista, 2026-08-10). Não é validação, é o que
 * existe no dropdown: a empresa não escolhe uma rota que não pode ofertar nem um navio que não lhe foi
 * dado, então o erro não chega a poder ser cometido. Para quem administra a plataforma, o escopo é o pool
 * inteiro — é ela quem cura.
 *
 * A **assinatura** (`criadoPor`) sai do funcionário do contexto. Papel puro de plataforma assina vazio:
 * não há funcionário, e inventar um id ali seria gravar autoria falsa.
 */
@HiltViewModel
class FormViagemViewModel @Inject constructor(
    private val viagemRepository: ViagemRepository,
    private val rotaRepository: RotaRepository,
    private val embarcacaoRepository: EmbarcacaoRepository,
    private val portoRepository: PortoRepository,
    private val localidadeRepository: LocalidadeRepository,
    private val escopoDaSessao: EscopoDaSessao,
    private val sessaoUsuario: SessaoUsuario,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        FormViagemUiState(diasDaSemana = DIAS_DA_SEMANA.map { it.rotulo }),
    )
    val uiState: StateFlow<FormViagemUiState> = _uiState.asStateFlow()

    private val _sucesso = Channel<Unit>(Channel.BUFFERED)
    val sucesso = _sucesso.receiveAsFlow()

    init {
        viewModelScope.launch { carregarOpcoes() }
        viewModelScope.launch { carregarViagens() }
    }

    /**
     * Os dois dropdowns nascem juntos porque nascem do **mesmo escopo**: separá-los em duas corrotinas
     * abriria a janela em que um está recortado e o outro não — e a tela ofereceria, por um instante, o
     * navio de outra empresa.
     */
    private suspend fun carregarOpcoes() {
        val escopo = escopoDaSessao.atual()

        val localidades = localidadeRepository.obterTodas().associate { it.id to it.rotulo }
        val portosPorId = portoRepository.obterTodos().associate { it.id to it.rotuloCom(localidades) }

        // Só as **ativas**: rota inativada é registro do passado, e oferecer uma no cadastro seria criar
        // uma partida nascida sobre o que já foi encerrado.
        val rotas = rotaRepository.obterTodas()
            .filter { it.ativo }
            .noEscopo(escopo)
            .map { RotaOpcao(id = it.id, rotulo = it.rotuloCom(portosPorId)) }
            .sortedBy { it.rotulo }

        val embarcacoes = embarcacaoRepository.obterTodos()
            .filter { escopo.concedeu(it.id) }
            .map { EmbarcacaoOpcao(id = it.id, rotulo = it.descricaoNome) }
            .sortedBy { it.rotulo }

        _uiState.update {
            it.copy(
                rotas = rotas,
                embarcacoes = embarcacoes,
                // Um lado vazio já basta: sem rota ou sem navio não há partida a montar, e mostrar um
                // dropdown cheio ao lado de um vazio faria parecer defeito.
                semConcessao = rotas.isEmpty() || embarcacoes.isEmpty(),
            )
        }
    }

    private suspend fun carregarViagens() {
        _uiState.update { it.copy(viagensExistentes = viagemRepository.obterTodas()) }
    }

    fun onRotaChange(v: String) = _uiState.update {
        // Trocar a rota pode ter desfeito a duplicidade — a chave é dos quatro campos.
        it.copy(rota = v, isRotaError = false, erroHora = ErroHoraViagem.NENHUM)
    }

    fun onEmbarcacaoChange(v: String) = _uiState.update {
        it.copy(embarcacao = v, isEmbarcacaoError = false, erroHora = ErroHoraViagem.NENHUM)
    }

    fun onDiaSemanaChange(v: String) = _uiState.update {
        it.copy(diaSemana = v, isDiaSemanaError = false, erroHora = ErroHoraViagem.NENHUM)
    }

    /**
     * O estado guarda **só os dígitos**; o `:` é desenhado pela `HoraVisualTransformation`.
     *
     * Duas correções empilhadas, e a segunda desfez a primeira: o campo pedia `HH:mm` num teclado
     * numérico, que não tem `:` (homologação); passou a mascarar o **valor**, e aí o cursor caía atrás do
     * separador a cada terceiro dígito (teste manual). Guardar dígito e pintar o resto resolve os dois —
     * não há separador a digitar nem caractere novo a reposicionar.
     */
    fun onHoraChange(v: String) = _uiState.update {
        it.copy(horaDigitada = digitosDaHora(v), erroHora = ErroHoraViagem.NENHUM)
    }

    fun salvar() {
        val estado = _uiState.value
        val erros = validarViagem(estado)
        if (!erros.valido) {
            _uiState.update {
                it.copy(
                    isRotaError = erros.rota,
                    isEmbarcacaoError = erros.embarcacao,
                    isDiaSemanaError = erros.diaSemana,
                    erroHora = erros.hora,
                )
            }
            return
        }

        // A validação já provou que a chave existe — mas quem grava lê a **mesma** função que quem
        // valida, e não uma reconstrução parecida que possa divergir dela.
        val chave = chaveDaViagem(estado) ?: return

        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            try {
                val contexto = sessaoUsuario.atual()
                viagemRepository.criar(
                    Viagem(
                        id = "",
                        rotaId = chave.rotaId,
                        embarcacaoId = chave.embarcacaoId,
                        diaSemana = chave.diaSemana,
                        horaMin = chave.horaMin,
                        criadoPor = contexto?.funcionario?.id.orEmpty(),
                        criadoEm = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    )
                )
                _sucesso.send(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "salvar: ${e.message}", e)
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    private companion object {
        const val TAG = "formViagemViewModel"
    }
}

/** O rótulo do porto para a rota: nome e cidade, que é o que distingue homônimos. */
internal fun Porto.rotuloCom(localidades: Map<String, String>): String =
    listOfNotNull(nome, localidades[localidadeId]).filter { it.isNotBlank() }.joinToString(" · ")

/** "Porto A · Belém/PA → Porto B · Parintins/AM" — o par, na ordem, que é o que a rota é. */
internal fun Rota.rotuloCom(portosPorId: Map<String, String>): String {
    val origem = portosPorId[portoOrigemId] ?: portoOrigemId
    val destino = portosPorId[portoDestinoId] ?: portoDestinoId
    return "$origem → $destino"
}