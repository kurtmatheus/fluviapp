package dev.matheus.fluviapp.ui.viewmodel.localidade

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.localidade.Localidade
import dev.matheus.fluviapp.domain.localidade.Uf
import dev.matheus.fluviapp.navigation.navcomposables.localidade.ID_LOCALIDADE_ARGUMENT
import dev.matheus.fluviapp.services.ibge.ConsultaMunicipioIbge
import dev.matheus.fluviapp.services.ibge.ResultadoConsultaIbge
import dev.matheus.fluviapp.services.repository.cadastro.localidade.LocalidadeRepository
import dev.matheus.fluviapp.ui.states.BuscaIbge
import dev.matheus.fluviapp.ui.states.FormLocalidadeUiState
import dev.matheus.fluviapp.ui.viewmodel.helpers.localidade.validarLocalidade
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Cadastro/edição de localidade no molde do ADR-0006: VM dona do estado, eventos como métodos, validação
 * pura, sucesso por evento one-shot, sem Context e sem runBlocking.
 *
 * O que esta tela tem de próprio é a **consulta ao IBGE** ([consultarIbge]): ajuda de digitação que
 * preenche município e UF a partir do código. Ela é opcional em todos os sentidos — não dispara sozinha,
 * não impede salvar e não desfaz o que a pessoa escreveu quando falha.
 */
@HiltViewModel
class FormLocalidadeViewModel @Inject constructor(
    private val localidadeRepository: LocalidadeRepository,
    private val consultaMunicipioIbge: ConsultaMunicipioIbge,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // "" = criação; id preenchido = edição (arg de rota opcional, sem sentinela "null").
    private val idLocalidade: String = savedStateHandle.get<String>(ID_LOCALIDADE_ARGUMENT).orEmpty()

    private val _uiState = MutableStateFlow(FormLocalidadeUiState())
    val uiState: StateFlow<FormLocalidadeUiState> = _uiState.asStateFlow()

    private val _sucesso = Channel<Unit>(Channel.BUFFERED)
    val sucesso = _sucesso.receiveAsFlow()

    init {
        if (idLocalidade.isNotBlank()) viewModelScope.launch { carregar() }
    }

    /**
     * Digitar o código **descarta o resultado anterior** da consulta: a mensagem "não encontrado" que
     * ficasse na tela passaria a acusar um código que já não é o que está escrito.
     */
    fun onCodigoIbgeChange(v: String) = _uiState.update {
        it.copy(
            codigoIbge = v.filter(Char::isDigit).take(TAMANHO_CODIGO),
            isCodigoIbgeError = false,
            buscaIbge = BuscaIbge.Ociosa,
        )
    }

    fun onMunicipioChange(v: String) = _uiState.update { it.copy(municipio = v, isMunicipioError = false) }

    /**
     * Aceita o **rótulo** do dropdown ("Pará (PA)") e também a sigla crua — a primeira é o que a tela
     * devolve, a segunda é o que o resto do app chama de UF. Tentar só uma delas seria escolher entre
     * funcionar na tela e funcionar em todo o resto.
     *
     * Mexer na UF limpa o erro do **código**, e não por generosidade: a coerência entre os dois é
     * cruzada, então corrigir um lado pode ter consertado a queixa do outro.
     */
    fun onUfChange(valor: String) = _uiState.update {
        it.copy(
            uf = Uf.porRotulo(valor) ?: Uf.de(valor),
            isUfError = false,
            isCodigoIbgeError = false,
        )
    }

    /**
     * Pergunta ao IBGE quem é o município daquele código e **preenche** município e UF.
     *
     * Só sobrescreve quando *encontra*: falha de rede não apaga o que a pessoa já tinha digitado. E o
     * desfecho negativo é dito com a palavra certa — *não é município* quando o serviço respondeu,
     * *não deu para consultar* quando ele não respondeu. Confundir os dois é acusar quem digitou de um
     * erro que foi da rede.
     */
    fun consultarIbge() {
        val estado = _uiState.value
        if (!estado.podeConsultarIbge || estado.buscaIbge == BuscaIbge.Consultando) return

        _uiState.update { it.copy(buscaIbge = BuscaIbge.Consultando) }
        viewModelScope.launch {
            when (val resultado = consultaMunicipioIbge.consultar(estado.codigoIbge)) {
                is ResultadoConsultaIbge.Encontrado -> _uiState.update {
                    it.copy(
                        municipio = resultado.municipio,
                        uf = resultado.uf,
                        isMunicipioError = false,
                        isUfError = false,
                        isCodigoIbgeError = false,
                        // O sucesso também é dito. Preencher em silêncio deixava a pessoa sem saber se o
                        // que está na tela veio do IBGE ou já estava lá.
                        buscaIbge = BuscaIbge.Encontrado("${resultado.municipio}/${resultado.uf.sigla}"),
                    )
                }

                ResultadoConsultaIbge.NaoEncontrado ->
                    _uiState.update { it.copy(buscaIbge = BuscaIbge.NaoEncontrado) }

                ResultadoConsultaIbge.Indisponivel ->
                    _uiState.update { it.copy(buscaIbge = BuscaIbge.Indisponivel) }
            }
        }
    }

    private suspend fun carregar() {
        localidadeRepository.obterPorId(idLocalidade)?.let { localidade ->
            _uiState.update {
                it.copy(
                    titulo = R.string.subtitle_editar_localidade,
                    codigoIbge = localidade.codigoIbge,
                    municipio = localidade.municipio,
                    uf = localidade.uf,
                )
            }
        }
    }

    fun salvar() {
        val estado = _uiState.value
        val erros = validarLocalidade(estado)
        // `uf == null` é redundante com `erros.uf` — está aqui para o smart-cast, como na Embarcação: é o
        // compilador, e não um comentário, que impede uma localidade sem UF de chegar ao domínio.
        val uf = estado.uf
        if (!erros.valido || uf == null) {
            _uiState.update {
                it.copy(
                    isMunicipioError = erros.municipio,
                    isUfError = erros.uf,
                    isCodigoIbgeError = erros.codigoIbge,
                )
            }
            return
        }

        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            try {
                localidadeRepository.salvar(
                    Localidade(
                        id = idLocalidade, // "" na criação → auto-id no repo
                        municipio = estado.municipio.trim(),
                        uf = uf,
                        codigoIbge = estado.codigoIbge,
                        // Editar não ressuscita nem enterra: o cadastro trata do conteúdo, e o `ativo` é
                        // do gesto de excluir. Toda gravação daqui é de localidade em uso.
                        ativo = true,
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
        const val TAG = "formLocalidadeViewModel"
        const val TAMANHO_CODIGO = 7
    }
}