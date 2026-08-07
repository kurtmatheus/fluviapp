package dev.matheus.fluviapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaRepository
import dev.matheus.fluviapp.services.repository.operacoes.SessaoUsuario
import dev.matheus.fluviapp.ui.states.SelecaoVinculoUiState
import dev.matheus.fluviapp.ui.states.VinculoOpcao
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A **seleção de contexto** (ADR-0016 §6, F6.4): quem serve a mais de uma empresa diz em nome de qual
 * está operando, e é essa resposta que decide o menu, o recorte das listas e a agência do bilhete.
 *
 * A tela é uma escolha e nada mais — não tem "voltar" nem "pular". Não é rigidez de UI: entrar sem
 * responder significaria montar o painel de alguém que o app não sabe quem é, que é exatamente o que a
 * splash passou a impedir na F6.4.
 */
@HiltViewModel
class SelecaoVinculoViewModel @Inject constructor(
    private val sessaoUsuario: SessaoUsuario,
    private val empresaRepository: EmpresaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SelecaoVinculoUiState())
    val uiState: StateFlow<SelecaoVinculoUiState> = _uiState.asStateFlow()

    private val _escolhido = Channel<Unit>(Channel.BUFFERED)
    val escolhido = _escolhido.receiveAsFlow()

    init {
        viewModelScope.launch { carregar() }
    }

    private suspend fun carregar() {
        val contexto = sessaoUsuario.atual()
        val empresasPorId = empresaRepository.obterTodas().associate { it.id to it.nome }

        _uiState.update {
            it.copy(
                nome = contexto?.nomeExibicao.orEmpty(),
                opcoes = contexto?.vinculos.orEmpty().map { vinculo ->
                    VinculoOpcao(
                        empresaId = vinculo.empresaId,
                        // Empresa que não resolve continua escolhível, com o id à mostra: esconder a
                        // opção deixaria a pessoa presa numa tela sem a alternativa que ela tem.
                        empresa = empresasPorId[vinculo.empresaId] ?: vinculo.empresaId,
                        cargo = vinculo.cargo.name,
                    )
                },
                carregando = false,
            )
        }
    }

    /** Guarda a escolha e avisa uma vez — quem navega é o grafo (evento one-shot, molde do ADR-0006). */
    fun escolher(empresaId: String) {
        viewModelScope.launch {
            sessaoUsuario.escolherEmpresa(empresaId)
            _escolhido.send(Unit)
        }
    }
}