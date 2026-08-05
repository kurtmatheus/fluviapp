package dev.matheus.fluviapp.ui.viewmodel.embarcacao

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.viagem.Embarcacao
import dev.matheus.fluviapp.domain.viagem.TipoEmbarcacao
import dev.matheus.fluviapp.navigation.navcomposables.embarcacao.ID_EMBARCACAO_ARGUMENT
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmbarcacaoRepository
import dev.matheus.fluviapp.ui.states.FormEmbarcacaoUiState
import dev.matheus.fluviapp.ui.viewmodel.helpers.embarcacao.validarEmbarcacao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Cadastro/edição de embarcação no molde refatorado (cadastro-modulos §7.2): VM dona do estado; eventos
 * são métodos; validação pura; sucesso via evento one-shot; cargas suspensas (sem runBlocking); arg
 * de rota opcional; sem Context. Vínculo N-1 com Empresa por id (ADR-0008): o dropdown seleciona o
 * nome, mas o `salvar` resolve e persiste o `empresaId`; a edição resolve o nome de volta do id.
 *
 * O **tipo** entra pelo rótulo (é o que o dropdown devolve) e sai como tipo de domínio: quem traduz é
 * `TipoEmbarcacao.porRotulo`, não a tela. E trocar de tipo tem consequência de estado — ver [onTipoChange].
 */
@HiltViewModel
class FormEmbarcacaoViewModel @Inject constructor(
    private val embarcacaoRepository: EmbarcacaoRepository,
    private val empresaRepository: EmpresaRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // "" = criação; id preenchido = edição (arg de rota opcional, sem sentinela "null").
    private val idEmbarcacao: String = savedStateHandle.get<String>(ID_EMBARCACAO_ARGUMENT).orEmpty()

    private val _uiState = MutableStateFlow(FormEmbarcacaoUiState())
    val uiState: StateFlow<FormEmbarcacaoUiState> = _uiState.asStateFlow()

    private val _sucesso = Channel<Unit>(Channel.BUFFERED)
    val sucesso = _sucesso.receiveAsFlow()

    init {
        // Sequenciado: a edição resolve o nome da empresa a partir do empresaId, então precisa da
        // `listaEmpresas` pronta antes (ADR-0008, Fase 3).
        viewModelScope.launch {
            carregarFontes()
            if (idEmbarcacao.isNotBlank()) carregar()
        }
    }

    private suspend fun carregarFontes() {
        _uiState.update { it.copy(listaEmpresas = empresaRepository.obterTodas()) }
    }

    fun onNomeChange(v: String) = _uiState.update { it.copy(nome = v, isNomeError = false) }
    fun onEmpresaChange(v: String) = _uiState.update { it.copy(empresa = v, isEmpresaError = false) }

    /**
     * Escolher o tipo pode **apagar** a capacidade de veículos, e isso é deliberado: quem digitou 12 vagas
     * e depois marcou "Lancha" não tem mais onde pôr as 12: o campo some da tela. Mantê-lo no estado
     * gravaria, invisível, uma lancha que leva carro — a contradição sobreviveria à interface que a
     * escondeu. Zerar é a única saída que deixa o estado igual ao que a pessoa vê.
     */
    fun onTipoChange(rotulo: String) = _uiState.update {
        val tipo = TipoEmbarcacao.porRotulo(rotulo)
        it.copy(
            tipo = tipo,
            isTipoError = false,
            capacidadeVeiculo = if (tipo?.levaVeiculo == true) it.capacidadeVeiculo else "",
        )
    }

    fun onCapacidadeVeiculoChange(v: String) = _uiState.update { it.copy(capacidadeVeiculo = v.filter(Char::isDigit)) }
    fun onCapacidadeSuite2Change(v: String) = _uiState.update { it.copy(capacidadeSuite2 = v.filter(Char::isDigit)) }
    fun onCapacidadeSuite3Change(v: String) = _uiState.update { it.copy(capacidadeSuite3 = v.filter(Char::isDigit)) }
    fun onCapacidadeCamaroteChange(v: String) = _uiState.update { it.copy(capacidadeCamarote = v.filter(Char::isDigit)) }

    private suspend fun carregar() {
        embarcacaoRepository.obterPorId(idEmbarcacao)?.let { embarcacao ->
            // Nome exibido no dropdown resolvido do empresaId (a embarcação não guarda mais o nome).
            val nomeEmpresa = _uiState.value.listaEmpresas.firstOrNull { it.id == embarcacao.empresaId }?.nome.orEmpty()
            _uiState.update {
                it.copy(
                    titulo = R.string.subtitle_editar_embarcacao,
                    nome = embarcacao.descricaoNome,
                    empresa = nomeEmpresa,
                    tipo = embarcacao.tipo,
                    capacidadeVeiculo = embarcacao.capacidadeVeiculo.toString(),
                    capacidadeSuite2 = embarcacao.capacidadeSuite2.toString(),
                    capacidadeSuite3 = embarcacao.capacidadeSuite3.toString(),
                    capacidadeCamarote = embarcacao.capacidadeCamarote.toString(),
                )
            }
        }
    }

    fun salvar() {
        val estado = _uiState.value
        val erros = validarEmbarcacao(estado)
        // `tipo == null` é redundante com `erros.tipo` — e está aqui de propósito: é o compilador, e não
        // um comentário, que garante que nenhuma embarcação sem tipo chegue ao domínio.
        val tipo = estado.tipo
        if (!erros.valido || tipo == null) {
            _uiState.update {
                it.copy(isNomeError = erros.nome, isEmpresaError = erros.empresa, isTipoError = erros.tipo)
            }
            return
        }

        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            val s = _uiState.value
            // ADR-0008 (Fase 0/1): resolve o link estável na fronteira de escrita, a partir da lista
            // já em cache. `empresa` (nome) segue gravado (dormente); ninguém lê `empresaId` ainda.
            val empresaId = s.listaEmpresas.firstOrNull { it.nome == s.empresa }?.id.orEmpty()
            try {
                embarcacaoRepository.salvar(
                    Embarcacao(
                        id = idEmbarcacao, // "" na criação → auto-id no repo
                        descricaoNome = s.nome,
                        tipo = tipo,
                        capacidadeVeiculo = s.capacidadeVeiculo.toIntOrNull() ?: 0,
                        capacidadeSuite2 = s.capacidadeSuite2.toIntOrNull() ?: 0,
                        capacidadeSuite3 = s.capacidadeSuite3.toIntOrNull() ?: 0,
                        capacidadeCamarote = s.capacidadeCamarote.toIntOrNull() ?: 0,
                        empresaId = empresaId,
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
        const val TAG = "formEmbarcacaoViewModel"
    }
}
