package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem

import dev.matheus.fluviapp.extensions.extrairLetrasOuNumeros
import dev.matheus.fluviapp.extensions.extrairNumeros
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Categoria.VEICULO
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.CNPJ
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.CPF
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.PASSAPORTE
import dev.matheus.fluviapp.model.passagem.Passagem
import dev.matheus.fluviapp.services.repository.cadastro.ConstanteRepository
import dev.matheus.fluviapp.services.repository.firebase.PassagemFirestoreRepository
import dev.matheus.fluviapp.ui.states.passagem.FormVeiculoUiState
import dev.matheus.fluviapp.ui.viewmodel.passagem.TAMANHO_CNPJ
import dev.matheus.fluviapp.ui.viewmodel.passagem.TAMANHO_CPF
import dev.matheus.fluviapp.ui.viewmodel.passagem.TAMANHO_PASS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking

class FormVeiculoHelper(
    private val uiState: MutableStateFlow<FormVeiculoUiState>,
    private val constanteRepository: ConstanteRepository,
    private val passagemRepository: PassagemFirestoreRepository
) {

    init {
        atualizaCampos()
    }

    private fun atualizaCampos() {
        uiState.update { stateVeiculo ->
            stateVeiculo.copy(
                onNomeResponsavelRetiradaChange = {
                    atualizarNomeResponsavelRetirada(it)
                },
                onTipoDocumentoResponsavelRetiradaChange = {
                    atualizarTipoDocumentoResponsavelRetirada(it)
                },
                onClickLimparTipoDocumentoResponsavelRetirada = {
                    limparCamposDocumento()
                },
                onDocumentoResponsavelRetiradaChange = { documento ->
                    atualizarDocumentoResponsavelRetirada(documento.extrairLetrasOuNumeros())
                },
                onTipoVeiculoChange = {
                    atualizarTipoVeiculo(it)
                },
                onModeloVeiculoChange = {
                    atualizarModeloVeiculo(it)
                },
                onPlacaVeiculoChange = {
                    atualizarPlacaVeiculo(it)
                },
                onCorVeiculoChange = {
                    atualizaCorVeiculo(it)
                },
                listaNomeResponsavelRetirada = passagemRepository.getListaNome(),
                listaTipoVeiculo = runBlocking { constanteRepository.obterTodosPorCategoria(VEICULO.name) }
            )
        }
    }

    private fun atualizarNomeResponsavelRetirada(nome: String) {
        uiState.update {
            it.copy(
                nomeResponsavelRetirada = nome,
                isNomeResponsavelRetiradaError = false
            )
        }
    }

    private fun atualizarTipoDocumentoResponsavelRetirada(tipoDocumento: String) {
        uiState.update {
            it.copy(
                tipoDocumentoResponsavelRetirada = tipoDocumento,
                isTipoDocumentoResponsavelRetiradaError = false,
                isDocumentoResponsavelRetiradaReadOnly = false,
            )
        }
    }

    private fun atualizarDocumentoResponsavelRetirada(documento: String) {
        uiState.update { uiState ->
            verificaTipoDocumento(
                documento,
                uiState.tipoDocumentoResponsavelRetirada,
                uiState
            ) { state, documento ->
                state.copy(
                    documentoResponsavelRetirada = documento,
                    isDocumentoResponsavelRetiradaError = false
                )
            }
        }
    }

    private fun verificaTipoDocumento(
        documento: String,
        tipoDocumento: String,
        uiState: FormVeiculoUiState,
        onAtualizarDocumento: (FormVeiculoUiState, String) -> FormVeiculoUiState,
    ): FormVeiculoUiState {
        return when (tipoDocumento) {
            CPF.name -> {
                if (documento.extrairNumeros().length <= TAMANHO_CPF) {
                    onAtualizarDocumento(uiState, documento.extrairNumeros())
                } else uiState
            }

            CNPJ.name -> {
                if ((documento.extrairNumeros().length <= TAMANHO_CNPJ)) {
                    onAtualizarDocumento(uiState, documento.extrairNumeros())
                } else uiState
            }

            PASSAPORTE.name -> {
                if (documento.length <= TAMANHO_PASS) {
                    onAtualizarDocumento(uiState, documento)
                } else uiState
            }

            else -> onAtualizarDocumento(uiState, documento.extrairNumeros())
        }
    }

    private fun limparCamposDocumento() {
        uiState.update {
            it.copy(
                tipoDocumentoResponsavelRetirada = "",
                documentoResponsavelRetirada = "",
                isDocumentoResponsavelRetiradaReadOnly = true
            )
        }
    }

    private fun atualizarTipoVeiculo(tipo: String) {
        uiState.update {
            it.copy(
                tipoVeiculo = tipo,
                isTipoVeiculoError = false
            )
        }
    }

    private fun atualizarModeloVeiculo(modelo: String) {
        uiState.update {
            it.copy(
                modeloVeiculo = modelo,
                isModeloVeiculoError = false
            )
        }
    }

    private fun atualizarPlacaVeiculo(placa: String) {
        uiState.update {
            it.copy(
                placaVeiculo = placa,
                isPlacaVeiculoError = false
            )
        }
    }

    private fun atualizaCorVeiculo(cor: String) {
        uiState.update {
            it.copy(
                corVeiculo = cor,
                isCorVeiculoError = false
            )
        }
    }

    fun preencherDadosVeiculo(passagem: Passagem) {
        val temResponsavelRetirada = !passagem.nomeResponsavelRetirada.isNullOrEmpty() &&
                !passagem.documentoResponsavelRetirada.isNullOrEmpty() &&
                !passagem.numeroDocumentoResponsavelRetirada.isNullOrEmpty()

        uiState.update {
            it.copy(
                tipoDocumentoResponsavelRetirada = passagem.documentoResponsavelRetirada.orEmpty(),
                isDocumentoResponsavelRetiradaReadOnly = !temResponsavelRetirada,
                documentoResponsavelRetirada = passagem.numeroDocumentoResponsavelRetirada.orEmpty(),
                nomeResponsavelRetirada = passagem.nomeResponsavelRetirada.orEmpty(),
                tipoVeiculo = passagem.tipoVeiculo.orEmpty(),
                modeloVeiculo = passagem.modeloVeiculo.orEmpty(),
                placaVeiculo = passagem.placaVeiculo.orEmpty(),
                corVeiculo = passagem.corVeiculo.orEmpty()
            )
        }
    }

    fun limparState() {
        uiState.update {
            it.copy(
                tipoDocumentoResponsavelRetirada = "",
                documentoResponsavelRetirada = "",
                nomeResponsavelRetirada = "",
                tipoVeiculo = "",
                modeloVeiculo = "",
                placaVeiculo = "",
                corVeiculo = "",
            )
        }
    }

}