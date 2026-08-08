package dev.matheus.fluviapp.ui.viewmodel.rota

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.matheus.fluviapp.domain.rota.Rota
import dev.matheus.fluviapp.services.repository.cadastro.localidade.LocalidadeRepository
import dev.matheus.fluviapp.services.repository.cadastro.porto.PortoRepository
import dev.matheus.fluviapp.services.repository.cadastro.rota.RotaRepository
import dev.matheus.fluviapp.services.repository.operacoes.SessaoUsuario
import dev.matheus.fluviapp.ui.states.ErroParRota
import dev.matheus.fluviapp.ui.states.FormRotaUiState
import dev.matheus.fluviapp.ui.states.PortoOpcao
import dev.matheus.fluviapp.ui.viewmodel.helpers.rota.validarRota
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
 * **Criação** de rota (F7.2) — e só criação: rota é imutável (ADR-0016 §7.1). O que noutras telas seria
 * "editar" aqui é criar outra e inativar a anterior, e o descartado fica como registro.
 *
 * As duas fontes são independentes, pela mesma razão que o Porto aprendeu na rc.3: o dropdown de portos
 * não pode ficar preso esperando a lista de rotas, e vice-versa. Cada uma tem a sua corrotina.
 *
 * A **assinatura** (`criadoPor`) sai do funcionário do contexto, e é o que resta de responsabilidade num
 * pool sem dono. Papel puro de plataforma assina vazio — não há funcionário, e inventar um id ali seria
 * gravar uma autoria falsa.
 */
@HiltViewModel
class FormRotaViewModel @Inject constructor(
    private val rotaRepository: RotaRepository,
    private val portoRepository: PortoRepository,
    private val localidadeRepository: LocalidadeRepository,
    private val sessaoUsuario: SessaoUsuario,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FormRotaUiState())
    val uiState: StateFlow<FormRotaUiState> = _uiState.asStateFlow()

    private val _sucesso = Channel<Unit>(Channel.BUFFERED)
    val sucesso = _sucesso.receiveAsFlow()

    init {
        viewModelScope.launch { carregarPortos() }
        viewModelScope.launch { carregarRotas() }
    }

    /**
     * Os portos **ativos**, com o rótulo que a pessoa lê — "Porto de Val-de-Cães · Belém/PA". A
     * localidade entra no rótulo porque é ela que distingue homônimos, e escolher entre dois "Porto
     * Central" sem saber a cidade é escolher no escuro.
     */
    private suspend fun carregarPortos() {
        val localidades = localidadeRepository.obterTodas().associate { it.id to it.rotulo }
        val opcoes = portoRepository.obterTodos()
            .filter { it.ativo }
            .map { porto ->
                PortoOpcao(
                    id = porto.id,
                    rotulo = listOfNotNull(porto.nome, localidades[porto.localidadeId])
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                )
            }
            .sortedBy { it.rotulo }

        _uiState.update { it.copy(portos = opcoes) }
    }

    private suspend fun carregarRotas() {
        val rotas = rotaRepository.obterTodas()
        _uiState.update { it.copy(rotasExistentes = rotas) }
    }

    // Mexer em qualquer um dos lados apaga a queixa do **par**: as três razões dele (faltando, mesmo
    // porto, já existe) são todas do conjunto, então trocar um porto pode ter resolvido qualquer uma.
    fun onPortoOrigemChange(v: String) =
        _uiState.update { it.copy(portoOrigem = v, erroPar = ErroParRota.NENHUM) }

    fun onPortoDestinoChange(v: String) =
        _uiState.update { it.copy(portoDestino = v, erroPar = ErroParRota.NENHUM) }

    fun onDistanciaChange(v: String) = _uiState.update {
        it.copy(distanciaMn = v.filter { c -> c.isDigit() || c == '.' }, isDistanciaError = false)
    }

    fun onTempoChange(v: String) = _uiState.update {
        it.copy(tempoMedioH = v.filter { c -> c.isDigit() || c == '.' }, isTempoError = false)
    }

    fun salvar() {
        val estado = _uiState.value
        val erros = validarRota(estado)
        if (!erros.valido) {
            _uiState.update {
                it.copy(
                    erroPar = erros.par,
                    isDistanciaError = erros.distancia,
                    isTempoError = erros.tempo,
                )
            }
            return
        }

        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            try {
                val contexto = sessaoUsuario.atual()
                rotaRepository.criar(
                    Rota(
                        id = "",
                        portoOrigemId = estado.portos.first { it.rotulo == estado.portoOrigem }.id,
                        portoDestinoId = estado.portos.first { it.rotulo == estado.portoDestino }.id,
                        distanciaMn = estado.distanciaMn.toDouble(),
                        tempoMedioH = estado.tempoMedioH.toDouble(),
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
        const val TAG = "formRotaViewModel"
    }
}