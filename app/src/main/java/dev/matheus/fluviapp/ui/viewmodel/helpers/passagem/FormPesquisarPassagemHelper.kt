package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem

import dev.matheus.fluviapp.services.repository.operacoes.FuncionarioRepository
import dev.matheus.fluviapp.ui.states.passagem.PesquisarPassagemUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking

class FormPesquisarPassagemHelper(
    private val uiState: MutableStateFlow<PesquisarPassagemUiState>,
    private val funcionarioRepository: FuncionarioRepository,
    /** Recorte da listagem (ADR-0015 §4.1): em branco = todas as agências. */
    private val agenciaDoEscopo: String = "",
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
                // A lista de status saiu daqui com o `runBlocking` que a buscava (ADR-0020 F2): são os
                // três estados da FSM, e o UiState já nasce com eles.
                // O dropdown de operador acompanha o escopo: o supervisor filtra pelos nomes da
                // PRÓPRIA agência — oferecer nomes de fora seria oferecer uma busca que volta vazia.
                listaOperadores = runBlocking {
                    if (agenciaDoEscopo.isBlank()) funcionarioRepository.obterTodosFuncionarios()
                    else funcionarioRepository.obterFuncionariosPorAgencia(agenciaDoEscopo)
                }.map { it.descricaoNome }
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
