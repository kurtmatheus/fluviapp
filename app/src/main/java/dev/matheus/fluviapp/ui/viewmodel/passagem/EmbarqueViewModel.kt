package dev.matheus.fluviapp.ui.viewmodel.passagem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.matheus.fluviapp.domain.passagem.ResultadoEmbarque
import dev.matheus.fluviapp.services.repository.firebase.PassagemFirestoreRepository
import dev.matheus.fluviapp.services.repository.operacoes.SessaoUsuario
import dev.matheus.fluviapp.ui.states.passagem.EmbarqueUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel da validação de embarque (ADR-0012). Dono do estado; injeta o MESMO repositório da
 * emissão (sem repo novo) e a sessão (identidade do operador que carimba o embarque).
 */
@HiltViewModel
class EmbarqueViewModel @Inject constructor(
    private val passagemRepository: PassagemFirestoreRepository,
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
            _uiState.update {
                if (passagem == null) it.copy(processando = false, resultado = ResultadoEmbarque.NaoEncontrada)
                else it.copy(processando = false, passagem = passagem)
            }
        }
    }

    /** Confirma o embarque da passagem resolvida (transição EMITIDA→EMBARCADA + carimbo do operador). */
    fun confirmarEmbarque() {
        val passagem = _uiState.value.passagem ?: return
        _uiState.update { it.copy(processando = true) }
        viewModelScope.launch {
            // O carimbo tem DUAS naturezas: `embarcadaPorId` continua sendo o **uid** — é o que a regra
            // do servidor confere contra `request.auth.uid` (ADR-0012), e forjar autoria continua
            // impossível. O nome exibido, esse, é do FUNCIONÁRIO (ADR-0015 §8.1); sem vínculo, o username.
            val contexto = sessaoUsuario.atual()
            val resultado = if (contexto == null) {
                ResultadoEmbarque.NaoEncontrada
            } else {
                passagemRepository.confirmarEmbarque(
                    passagem.id,
                    contexto.usuario.id,
                    contexto.nomeExibicao,
                )
            }
            _uiState.update { it.copy(processando = false, passagem = null, resultado = resultado) }
        }
    }

    /** Volta para o modo de escaneamento (novo bilhete). */
    fun reiniciar() {
        _uiState.update { EmbarqueUiState() }
    }
}
