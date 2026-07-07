package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem

import android.content.Context
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.extensions.isTextoNaoNulo
import dev.matheus.fluviapp.extensions.toastMessage
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Categoria.MUNICIPIO
import dev.matheus.fluviapp.model.cadastro.passagem.Agente
import dev.matheus.fluviapp.model.mapDescricao
import dev.matheus.fluviapp.services.repository.cadastro.ConstanteRepository
import dev.matheus.fluviapp.services.repository.cadastro.passagem.AgenteRepository
import dev.matheus.fluviapp.ui.states.AgenteUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking

class FormAgenteHelper(
    private val uiState: MutableStateFlow<AgenteUiState>,
    private val repository: AgenteRepository,
    private val constanteRepository: ConstanteRepository,
) {

    lateinit var onNavegaParaMainScreen: () -> Unit

    init {
        inicializarCampos()
    }

    private fun inicializarCampos() {
        uiState.update { state ->
            state.copy(
                onAgenciaChange = {
                    atualizarAgencia(it)
                },
                onAgenteChange = {
                    atualizarAgente(it)
                },
                onLotacaoChange = {
                    atualizarLotacao(it)
                },
                listaAgencia = runBlocking { repository.obterTodasAgencias() },
                listaMunicipios = runBlocking { constanteRepository.obterTodosPorCategoria(MUNICIPIO.name).mapDescricao() },
                resultadosListaAgente = runBlocking { repository.obterTodosAgentes() },
            )
        }
    }

    private fun atualizarAgencia(agencia: String) {
        uiState.update {
            it.copy(
                agencia = agencia,
                isAgenciaError = false
            )
        }
    }

    private fun atualizarAgente(agente: String) {
        uiState.update {
            it.copy(
                agente = agente,
                isAgenteError = false
            )
        }
    }

    private fun atualizarLotacao(lotacao: String) {
        uiState.update {
            it.copy(
                lotacao = lotacao,
                isLotacaoError = false,
            )
        }
    }

    fun atualizarProcessamento() {
        uiState.update {
            it.copy(
                isProcessing = !it.isProcessing
            )
        }
    }

    suspend fun salvar(
        idAgente: String,
        context: Context,
    ) {
        val state = uiState.value
        var agenteExistente: Agente? = null
        if (idAgente.isTextoNaoNulo()) {
            agenteExistente = repository.obterPorId(idAgente)
        }

        val agente = agenteExistente ?: Agente(
            id = "",
            descricaoNome = state.agente,
            agencia = state.agencia,
            lotacao = state.lotacao
        )

        try {
            repository.salvar(agente)
        } catch (e: Exception) {
            e.printStackTrace()
            context.toastMessage(context.resources.getString(R.string.error_salvar_agent))
        } finally {
            context.toastMessage(context.resources.getString(R.string.msg_salva_agent))
            onNavegaParaMainScreen()
        }
    }

    suspend fun preencherCampos(idAgente: String) {
        val agente = repository.obterPorId(idAgente)
        uiState.update {
            it.copy(
                agente = agente.descricaoNome,
                agencia = agente.agencia,
                lotacao = agente.lotacao,
                titleJanela = R.string.subtitle_editar_agente
            )
        }
    }
}
