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
                listaNomeResponsavelRetirada = passagemRepository.getListaNome(),
            )
        }
    }

    /** Carga suspensa das listas (molde ADR-0006): sem `runBlocking` na thread principal no init. */
    suspend fun carregarListas() {
        uiState.update {
            it.copy(listaTipoVeiculo = constanteRepository.obterTodosPorCategoria(VEICULO.name))
        }
    }

    internal fun atualizarNomeResponsavelRetirada(nome: String) {
        uiState.update {
            it.copy(
                nomeResponsavelRetirada = nome,
                isNomeResponsavelRetiradaError = false
            )
        }
    }

    internal fun atualizarTipoDocumentoResponsavelRetirada(tipoDocumento: String) {
        uiState.update {
            it.copy(
                tipoDocumentoResponsavelRetirada = tipoDocumento,
                isTipoDocumentoResponsavelRetiradaError = false,
                isDocumentoResponsavelRetiradaReadOnly = false,
            )
        }
    }

    internal fun atualizarDocumentoResponsavelRetirada(documento: String) {
        uiState.update { uiState ->
            verificaTipoDocumento(
                documento.extrairLetrasOuNumeros(),
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

    internal fun limparCamposDocumento() {
        uiState.update {
            it.copy(
                tipoDocumentoResponsavelRetirada = "",
                documentoResponsavelRetirada = "",
                isDocumentoResponsavelRetiradaReadOnly = true
            )
        }
    }

    internal fun atualizarTipoVeiculo(tipo: String) {
        uiState.update {
            it.copy(
                tipoVeiculo = tipo,
                isTipoVeiculoError = false
            )
        }
    }

    internal fun atualizarModeloVeiculo(modelo: String) {
        uiState.update {
            it.copy(
                modeloVeiculo = modelo,
                isModeloVeiculoError = false
            )
        }
    }

    internal fun atualizarPlacaVeiculo(placa: String) {
        uiState.update {
            it.copy(
                placaVeiculo = placa,
                isPlacaVeiculoError = false
            )
        }
    }

    internal fun atualizaCorVeiculo(cor: String) {
        uiState.update {
            it.copy(
                corVeiculo = cor,
                isCorVeiculoError = false
            )
        }
    }

    internal fun atualizarCilindrada(valor: String) {
        uiState.update {
            it.copy(
                // Filtro de dígito: guarda contra qualquer caractere acidental não-numérico (ADR-0013).
                cilindrada = valor.extrairNumeros(),
                isCilindradaError = false
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
                corVeiculo = passagem.corVeiculo.orEmpty(),
                cilindrada = passagem.cilindrada.orEmpty()
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
                cilindrada = "",
            )
        }
    }

}