package dev.matheus.fluviapp.ui.viewmodel.helpers.viagem

import android.content.Context
import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.extensions.isTextoNaoNulo
import dev.matheus.fluviapp.extensions.toastMessage
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Categoria.MUNICIPIO
import dev.matheus.fluviapp.model.extrairPorDescricao
import dev.matheus.fluviapp.services.repository.cadastro.ConstanteRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaRepository
import dev.matheus.fluviapp.services.repository.cadastro.viagem.NavioRepository
import dev.matheus.fluviapp.services.repository.firebase.ViagemFirestoreRepository
import dev.matheus.fluviapp.ui.states.FormViagemUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking

class FormViagemHelper(
    private val uiState: MutableStateFlow<FormViagemUiState>,
    private val empresaRepository: EmpresaRepository,
    private val navioRepository: NavioRepository,
    private val constanteRepository: ConstanteRepository,
    private val viagemRepository: ViagemFirestoreRepository,
) {

    lateinit var onNavegaParaMainScreen: () -> Unit

    init {
        atualizaCampos()
    }

    private fun atualizaCampos() {
        uiState.update { state ->
            state.copy(
                onEmpresaChange = {
                    atualizarEmpresa(it)
                    atualizarListaNavio(it)
                },
                onNavioChange = {
                    atualizaNavio(it)
                },
                onTrechoOrigemChange = {
                    atualizaTrechoOrigem(it)
                },
                onClickLimparTrechoOrigem = {
                    limparTrechoOrigem()
                },
                onTrechoDestinoChange = {
                    atualizaTrechoDestino(it)
                },
                onClickLimparTrechoDestino = {
                    atualizaTrechoDestino("")
                },
                listaNavios = runBlocking { navioRepository.obterTodos() },
                listaMunicipios = runBlocking { constanteRepository.obterTodosPorCategoria(MUNICIPIO.name) },
                listaEmpresas = runBlocking { empresaRepository.obterTodas() },
            )
        }
    }

    private fun atualizarEmpresa(nome: String) {
        uiState.update {
            it.copy(
                empresa = nome,
                isNavioDisable = false
            )
        }
    }

    private fun atualizarListaNavio(empresaNome: String) {
        val listaNavio = runBlocking {
            navioRepository.obterTodos().filter {
                it.empresa == empresaNome
            }
        }

        uiState.update {
            it.copy(
                listaNavios = listaNavio
            )
        }
    }

    private fun atualizaNavio(navio: String) {
        uiState.update {
            it.copy(
                navio = navio,
                isNavioError = false
            )
        }
    }

    private fun atualizaTrechoOrigem(trecho: String) {
        uiState.update {
            it.copy(
                trechoOrigem = trecho,
                isTrechoOrigemError = false,
                isTrechoDestinoDisabled = false
            )
        }
    }

    private fun limparTrechoOrigem() {
        uiState.update {
            it.copy(
                trechoOrigem = "",
                isTrechoOrigemError = false,
                isTrechoDestinoDisabled = true
            )
        }
    }

    private fun atualizaTrechoDestino(trecho: String) {
        uiState.update {
            it.copy(
                trechoDestino = trecho,
                isTrechoDestinoError = false
            )
        }
    }

    fun atualizaIsProcessando() {
        uiState.update {
            it.copy(
                isProcessando = !it.isProcessando
            )
        }
    }

    suspend fun salvarViagem(
        idViagem: String,
        context: Context,
    ) {
        val formViagemUiState = uiState.value

        try {
            // Dentro do try: os lookups por descrição podem lançar NoSuchElementException se o texto
            // não estiver na lista — vira toast de erro em vez de crash.
            val navio = formViagemUiState.listaNavios.extrairPorDescricao(formViagemUiState.navio)
            val empresa = formViagemUiState.listaEmpresas.first { it.nome == formViagemUiState.empresa }
            val trechoOrigem = formViagemUiState.listaMunicipios.extrairPorDescricao(formViagemUiState.trechoOrigem)
            val trechoDestino = formViagemUiState.listaMunicipios.extrairPorDescricao(formViagemUiState.trechoDestino)

            viagemRepository.salvar(
                id = if (idViagem.isTextoNaoNulo()) idViagem else null,
                navio = navio.descricaoNome,
                empresa = empresa.nome,
                origem = trechoOrigem.descricaoNome,
                destino = trechoDestino.descricaoNome,
            )
            context.toastMessage(context.resources.getString(R.string.msg_transmissao_viagem))
            onNavegaParaMainScreen()
        } catch (e: Exception) {
            context.toastMessage(context.resources.getString(R.string.error_transmissao_viagem))
            atualizaIsProcessando()
        }
    }
}
