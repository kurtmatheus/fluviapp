package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem

import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Categoria.STATUS_PASSAGEM
import dev.matheus.fluviapp.services.repository.cadastro.ConstanteRepository
import dev.matheus.fluviapp.services.repository.operacoes.UsuarioRepository
import dev.matheus.fluviapp.ui.states.passagem.PesquisarPassagemUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking

class FormPesquisarPassagemHelper(
    private val uiState: MutableStateFlow<PesquisarPassagemUiState>,
    private val constanteRepository: ConstanteRepository,
    private val usuarioRepository: UsuarioRepository
) {

    init {
        atualizarCampos()
    }

    private fun atualizarCampos() {
        uiState.update { state ->
            state.copy(
                onDataChange = {
                    atualizarFiltroData(it)
                },
                onSituacaoChange = {
                    atualizarFiltroSituacao(it)
                },
                onOperadorChange = {
                    atualizarOperador(it)
                },
                onCheckVeiculo = {
                    checkVeiculo()
                },
                onCheckPassageiro = {
                    checkPassageiro()
                },
                onPesquisaChange = {
                    atualizarBarraPesquisa(it)
                },
                listaSituacaoPassagem = runBlocking { constanteRepository.obterTodosPorCategoria(STATUS_PASSAGEM.name) },
                listaOperadores = runBlocking { usuarioRepository.obterTodos().map { it.nome } }
            )
        }
    }

    private fun atualizarFiltroData(data: String) {
        uiState.update {
            it.copy(
                data = data,
                isDataError = false
            )
        }
    }

    private fun atualizarFiltroSituacao(situacao: String) {
        uiState.update {
            it.copy(
                situacao = situacao,
                isSituacaoError = false
            )
        }
    }

    private fun atualizarOperador(operador: String) {
        uiState.update {
            it.copy(
                operador = operador
            )
        }
    }

    private fun checkVeiculo() {
        uiState.update {
            it.copy(
                isVeiculoChecked = !it.isVeiculoChecked,
                isFiltroError = false
            )
        }
    }

    private fun checkPassageiro() {
        uiState.update {
            it.copy(
                isPassageiroChecked = !it.isPassageiroChecked,
                isFiltroError = false
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

    private fun atualizarBarraPesquisa(pesquisa: String) {
        uiState.update {
            it.copy(
                pesquisa = pesquisa
            )
        }
    }

    fun showBarraPesquisa() {
        uiState.update {
            it.copy(
                isShowBarraPesquisa = !it.isShowBarraPesquisa
            )
        }
    }

    fun atualizaPermissaoEspecial() {
        uiState.update {
            it.copy(
                temPermissaoEspecial = !it.temPermissaoEspecial
            )
        }
    }


}
