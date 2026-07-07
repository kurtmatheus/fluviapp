package br.com.gruponaveg.ui.viewmodel.helpers.viagem

import br.com.gruponaveg.model.cadastro.constantes.Constante.Categoria.MUNICIPIO
import br.com.gruponaveg.model.screendata.DadosViagemCard
import br.com.gruponaveg.services.repository.cadastro.ConstanteRepository
import br.com.gruponaveg.services.repository.cadastro.viagem.EmpresaRepository
import br.com.gruponaveg.services.repository.cadastro.viagem.NavioRepository
import br.com.gruponaveg.ui.states.PesquisarViagemUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking

class FormPesquisarViagemHelper(
    private val uiState: MutableStateFlow<PesquisarViagemUiState>,
    private val empresaRepository: EmpresaRepository,
    private val navioRepository: NavioRepository,
    private val constanteRepository: ConstanteRepository,
) {

    init {
        atualizarCampos()
    }

    private fun atualizarCampos() {
        uiState.update { state ->
            state.copy(
                onCheckEmpresa = {
                    checkEmpresa()
                },
                onEmpresaChange = {
                    atualizarFiltroEmpresa(it)
                },
                onCheckNavio = {
                    checkNavio()
                },
                onNavioChange = {
                    atualizarFiltroNavio(it)
                },
                onCheckTrecho = {
                    checkTrecho()
                },
                onOrigemChange = {
                    atualizarFiltroOrigem(it)
                },
                onDestinoChange = {
                    atualizarFiltroDestino(it)
                },
                onExibirConfirmDeleteDialog = {
                    exibirConfirmDeleteDialog()
                },
                listaEmpresas = runBlocking { empresaRepository.obterTodas() },
                listaNavios = runBlocking { navioRepository.obterTodos() },
                listaMunicipios = runBlocking { constanteRepository.obterTodosPorCategoria(MUNICIPIO.name) }
            )
        }
    }

    private fun checkEmpresa() {
        uiState.update {
            it.copy(
                isCheckedEmpresa = !it.isCheckedEmpresa,
                isNavioError = false,
                navio = ""
            )
        }
    }

    private fun atualizarFiltroEmpresa(empresa: String) {
        uiState.update {
            it.copy(
                empresa = empresa,
                isEmpresaError = false
            )
        }
    }

    private fun checkNavio() {
        uiState.update {
            it.copy(
                isCheckedNavio = !it.isCheckedNavio,
                isNavioError = false,
                navio = ""
            )
        }
    }

    private fun atualizarFiltroNavio(navio: String) {
        uiState.update {
            it.copy(
                navio = navio,
                isNavioError = false
            )
        }
    }

    private fun checkTrecho() {
        uiState.update {
            it.copy(
                isCheckedTrecho = !it.isCheckedTrecho,
                isTrechoError = false,
                origem = "",
                destino = ""
            )
        }
    }

    private fun atualizarFiltroOrigem(situacao: String) {
        uiState.update {
            it.copy(
                origem = situacao,
                isTrechoError = false
            )
        }
    }

    private fun atualizarFiltroDestino(situacao: String) {
        uiState.update {
            it.copy(
                destino = situacao,
                isTrechoError = false
            )
        }
    }

    fun exibirConfirmDeleteDialog() {
        uiState.update {
            it.copy(
                isShowDeleteDialog = !it.isShowDeleteDialog
            )
        }
    }

    fun atualizarDadosViagemCard(dadosViagemCard: DadosViagemCard) {
        uiState.update {
            it.copy(
                dadosViagemCard = dadosViagemCard
            )
        }
    }
}
