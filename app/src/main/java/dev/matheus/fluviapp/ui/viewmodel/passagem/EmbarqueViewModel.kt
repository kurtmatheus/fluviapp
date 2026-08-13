package dev.matheus.fluviapp.ui.viewmodel.passagem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.matheus.fluviapp.domain.passagem.ResultadoEmbarque
import dev.matheus.fluviapp.services.repository.operacoes.SessaoUsuario
import dev.matheus.fluviapp.services.repository.passagem.PassagemRepository
import dev.matheus.fluviapp.ui.states.passagem.EmbarqueUiState
import dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.ColetorDeReferencias
import dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.ReferenciasDaPassagem
import dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.paraConferencia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel da validação de embarque (ADR-0012). Dono do estado; injeta a **porta** da passagem, o coletor
 * de referências e a sessão (identidade do operador que carimba o embarque).
 *
 * É o primeiro ViewModel de passagem a falar com [PassagemRepository] em vez da classe concreta — e o mais
 * barato de converter, porque as duas operações que ele usa (`obterDoServidorPorId` e `confirmarEmbarque`) já
 * eram as que a porta descreve. Com ela, este fluxo passa a ser **testável em JVM** com fakes ([ADR-0025] D1).
 *
 * ### O que a junção mudou aqui, e onde ela aparece
 *
 * O ViewModel **coleta** ([ColetorDeReferencias]) e depois **traduz** ([paraConferencia], função pura). As
 * duas linhas ficam uma embaixo da outra de propósito: é isso que faz *"esta tela vai buscar cliente, viagem,
 * rota e portos"* estar escrito onde alguém lê, em vez de escondido dentro de um `map`.
 */
@HiltViewModel
class EmbarqueViewModel @Inject constructor(
    private val passagemRepository: PassagemRepository,
    private val coletorDeReferencias: ColetorDeReferencias,
    private val sessaoUsuario: SessaoUsuario,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmbarqueUiState())
    val uiState: StateFlow<EmbarqueUiState> = _uiState.asStateFlow()

    /**
     * Chamado pelo leitor de QR. Resolve a passagem ao vivo para exibição/conferência. Ignora leituras
     * enquanto já há um fluxo em andamento (a câmera dispara o mesmo QR em vários frames).
     */
    fun aoLerQr(idPassagem: String) {
        val estado = _uiState.value
        if (!estado.escaneando || idPassagem.isBlank()) return
        _uiState.update { it.copy(processando = true) }
        viewModelScope.launch {
            val passagem = runCatching { passagemRepository.obterDoServidorPorId(idPassagem) }.getOrNull()
            if (passagem == null) {
                _uiState.update { it.copy(processando = false, resultado = ResultadoEmbarque.NaoEncontrada) }
                return@launch
            }

            // Coletar e traduzir, nesta ordem e à vista. Falha de referência **não derruba a conferência**:
            // sem cliente, viagem ou rota, o bilhete continua conferível pelo que ele carrega em si — e
            // recusar o embarque porque um lookup falhou seria deixar gente na doca por causa de rede.
            val referencias = runCatching { coletorDeReferencias.de(passagem) }
                .getOrDefault(ReferenciasDaPassagem())

            _uiState.update {
                it.copy(
                    processando = false,
                    passagem = passagem,
                    conferencia = passagem.paraConferencia(referencias),
                )
            }
        }
    }

    /** Confirma o embarque da passagem resolvida (transição EMITIDA→EMBARCADA + carimbo do operador). */
    fun confirmarEmbarque() {
        val passagem = _uiState.value.passagem ?: return
        _uiState.update { it.copy(processando = true) }
        viewModelScope.launch {
            // O carimbo carrega **só o uid** — é ele que a regra do servidor confere contra
            // `request.auth.uid` (ADR-0012), e é o que torna forjar autoria impossível. O nome de quem
            // validou **deixou de ser gravado ao lado**: no domínio nada é congelado (ADR-0023 D8), e o
            // nome se resolve por referência na leitura. Era mais uma cópia que podia discordar da origem.
            val contexto = sessaoUsuario.atual()
            val resultado = if (contexto == null) {
                ResultadoEmbarque.NaoEncontrada
            } else {
                passagemRepository.confirmarEmbarque(passagem.id, contexto.usuario.id)
            }
            _uiState.update {
                it.copy(processando = false, passagem = null, conferencia = null, resultado = resultado)
            }
        }
    }

    /** Volta para o modo de escaneamento (novo bilhete). */
    fun reiniciar() {
        _uiState.update { EmbarqueUiState() }
    }
}
