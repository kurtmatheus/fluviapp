package br.com.gruponaveg.ui.viewmodel.helpers.passagem

import br.com.gruponaveg.extensions.extrairLetrasOuNumeros
import br.com.gruponaveg.extensions.extrairNumeros
import br.com.gruponaveg.model.cadastro.constantes.Constante.Categoria.ACOMODACAO
import br.com.gruponaveg.model.cadastro.constantes.Constante.Categoria.GRATUIDADE
import br.com.gruponaveg.model.cadastro.constantes.Constante.Categoria.TIPO_PASSAGEM
import br.com.gruponaveg.model.cadastro.constantes.Constante.Descricao.CNPJ
import br.com.gruponaveg.model.cadastro.constantes.Constante.Descricao.CPF
import br.com.gruponaveg.model.cadastro.constantes.Constante.Descricao.PASSAPORTE
import br.com.gruponaveg.model.cadastro.constantes.obterCategoriaFormatada
import br.com.gruponaveg.model.passagem.Passagem
import br.com.gruponaveg.services.repository.cadastro.ConstanteRepository
import br.com.gruponaveg.services.repository.firebase.PassagemFirestoreRepository
import br.com.gruponaveg.ui.states.passagem.FormPassageiroUiState
import br.com.gruponaveg.ui.states.passagem.FormPassagemUiState
import br.com.gruponaveg.ui.viewmodel.passagem.TAMANHO_CNPJ
import br.com.gruponaveg.ui.viewmodel.passagem.TAMANHO_CPF
import br.com.gruponaveg.ui.viewmodel.passagem.TAMANHO_PASS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking

class FormPassageiroHelper(
    private val uiState: MutableStateFlow<FormPassageiroUiState>,
    private val uiStatePassagem: MutableStateFlow<FormPassagemUiState>,
    private val constanteRepository: ConstanteRepository,
    private val passagemRepository: PassagemFirestoreRepository
) {

    init {
        atualizaCampos()
    }

    private fun atualizaCampos() {
        uiState.update { state ->
            state.copy(
                onAcomodacaoChange = {
                    atualizarAcomodacao(it)
                },
                onTipoPassagemChange = {
                    atualizarTipoPassagem(it)
                },
                onTipoGratuidadeChange = {
                    atualizarTipoGratuidade(it)
                },
                onNomePassageiro1Change = {
                    atualizarNomePassageiro1(it)
                },
                onTipoDocumentoPassageiro1Change = {
                    atualizarTipoDocumentoPassageiro1(it)
                },
                onClickLimparDocumentoPassageiro1 = {
                    limparDocumentoPassageiro1()
                },
                onDocumentoPassageiro1Change = { documento ->
                    atualizaDocumentoPassageiro1(documento.extrairLetrasOuNumeros())
                },
                onDataNascimentoPassageiro1Change = {
                    atualizarDataNascimentoPassageiro1(it)
                },
                onCheckPassageiro2 = {
                    checkPassageiro2()
                },
                onNomePassageiro2Change = {
                    atualizarNomePassageiro2(it)
                },
                onTipoDocumentoPassageiro2Change = {
                    atualizarTipoDocumentoPassageiro2(it)
                },
                onClickLimparDocumentoPassageiro2 = {
                    limparDocumentoPassageiro2()
                },
                onDocumentoPassageiro2Change = { documento ->
                    atualizaDocumentoPassageiro2(documento.extrairLetrasOuNumeros())
                },
                onDataNascimentoPassageiro2Change = {
                    atualizarDataNascimentoPassageiro2(it)
                },
                onCheckPassageiro3 = {
                    checkPassageiro3()
                },
                onNomePassageiro3Change = {
                    atualizarNomePassageiro3(it)
                },
                onTipoDocumentoPassageiro3Change = {
                    atualizarTipoDocumentoPassageiro3(it)
                },
                onClickLimparDocumentoPassageiro3 = {
                    limparDocumentoPassageiro3()
                },
                onDocumentoPassageiro3Change = { documento ->
                    atualizaDocumentoPassageiro3(documento.extrairLetrasOuNumeros())
                },
                onDataNascimentoPassageiro3Change = {
                    atualizarDataNascimentoPassageiro3(it)
                },
                listaNomePassageiro = passagemRepository.getListaNome(),
                listaAcomodacao = runBlocking { constanteRepository.obterTodosPorCategoria(ACOMODACAO.name) },
                listaTipoPassagem = runBlocking { constanteRepository.obterTodosPorCategoria(TIPO_PASSAGEM.obterCategoriaFormatada()) },
                listaTipoGratuidade = runBlocking { constanteRepository.obterTodosPorCategoria(GRATUIDADE.name) }
            )
        }
    }

    private fun atualizarAcomodacao(acomodacao: String) {
        uiState.update { state ->
            state.copy(
                acomodacao = acomodacao,
                isAcomodacaoError = false
            )
        }
    }

    private fun atualizarTipoPassagem(tipo: String) {
        uiState.update {
            it.copy(
                tipoPassagem = tipo,
                isTipoPassagemError = false
            )
        }

        formatarCamposValores(tipo)
    }

    private fun formatarCamposValores(tipo: String) {
        if (tipo == GRATUIDADE.name) {
            uiStatePassagem.update {
                it.copy(
                    valorPago = "0",
                    desconto = "0",
                    isValorPagoError = false,
                    isValorPagoEnabled = false,
                    isDescontoEnabled = false
                )
            }
        } else {
            uiStatePassagem.update {
                it.copy(
                    isValorPagoError = false,
                    isValorPagoEnabled = true,
                    isDescontoEnabled = true
                )
            }
        }
    }

    private fun atualizarTipoGratuidade(tipo: String) {
        uiState.update {
            it.copy(
                tipoGratuidade = tipo,
                isTipoGratuidadeError = false
            )
        }
    }

    private fun atualizarTipoDocumentoPassageiro1(tipoDocumento: String) {
        uiState.update {
            it.copy(
                tipoDocumentoPassageiro1 = tipoDocumento,
                isTipoDocumentoPassageiro1Error = false,
                isDocumentoPassageiro1Disabled = false
            )
        }
    }

    private fun limparDocumentoPassageiro1() {
        uiState.update {
            it.copy(
                tipoDocumentoPassageiro1 = "",
                documentoPassageiro1 = "",
                isDocumentoPassageiro1Disabled = true
            )
        }
    }

    private fun atualizaDocumentoPassageiro1(documento: String) {
        uiState.update { uiState ->
            verificaTipoDocumento(documento, uiState.tipoDocumentoPassageiro1, uiState) { state, documento ->
                state.copy(
                    documentoPassageiro1 = documento,
                    isDocumentoPassageiro1Error = false
                )
            }
        }
    }

    private fun atualizarNomePassageiro1(nome: String) {
        uiState.update {
            it.copy(
                nomePassageiro1 = nome,
                isNomePassageiro1Error = false
            )
        }
    }

    private fun atualizarDataNascimentoPassageiro1(dataNascimento: String) {
        uiState.update {
            it.copy(
                dataNascimentoPassageiro1 = dataNascimento,
                isDataNascimentoPassageiro1Error = false
            )
        }
    }

    private fun checkPassageiro2() {
        uiState.update {
            it.copy(
                isPassageiro2Checked = !it.isPassageiro2Checked
            )
        }
    }

    private fun atualizarTipoDocumentoPassageiro2(tipoDocumento: String) {
        uiState.update {
            it.copy(
                tipoDocumentoPassageiro2 = tipoDocumento,
                isTipoDocumentoPassageiro2Error = false,
                isDocumentoPassageiro2Disabled = false
            )
        }
    }

    private fun limparDocumentoPassageiro2() {
        uiState.update {
            it.copy(
                tipoDocumentoPassageiro2 = "",
                documentoPassageiro2 = "",
                isDocumentoPassageiro2Disabled = true
            )
        }
    }

    private fun atualizaDocumentoPassageiro2(documento: String) {
        uiState.update { uiState ->
            verificaTipoDocumento(documento, uiState.tipoDocumentoPassageiro2, uiState) { state, documento ->
                state.copy(
                    documentoPassageiro2 = documento,
                    isDocumentoPassageiro2Error = false
                )
            }
        }
    }

    private fun atualizarNomePassageiro2(nome: String) {
        uiState.update {
            it.copy(
                nomePassageiro2 = nome,
                isNomePassageiro2Error = false
            )
        }
    }

    private fun atualizarDataNascimentoPassageiro2(dataNascimento: String) {
        uiState.update {
            it.copy(
                dataNascimentoPassageiro2 = dataNascimento,
                isDataNascimentoPassageiro2Error = false
            )
        }
    }

    private fun checkPassageiro3() {
        uiState.update {
            it.copy(
                isPassageiro3Checked = !it.isPassageiro3Checked
            )
        }
    }

    private fun atualizarTipoDocumentoPassageiro3(tipoDocumento: String) {
        uiState.update {
            it.copy(
                tipoDocumentoPassageiro3 = tipoDocumento,
                isTipoDocumentoPassageiro3Error = false,
                isDocumentoPassageiro3Disabled = false
            )
        }
    }

    private fun limparDocumentoPassageiro3() {
        uiState.update {
            it.copy(
                tipoDocumentoPassageiro3 = "",
                documentoPassageiro3 = "",
                isDocumentoPassageiro3Disabled = true
            )
        }
    }

    private fun atualizaDocumentoPassageiro3(documento: String) {
        uiState.update { uiState ->
            verificaTipoDocumento(documento, uiState.tipoDocumentoPassageiro3, uiState) { state, documento ->
                state.copy(
                    documentoPassageiro3 = documento,
                    isDocumentoPassageiro3Error = false
                )
            }
        }
    }

    private fun atualizarNomePassageiro3(nome: String) {
        uiState.update {
            it.copy(
                nomePassageiro3 = nome,
                isNomePassageiro3Error = false
            )
        }
    }

    private fun atualizarDataNascimentoPassageiro3(dataNascimento: String) {
        uiState.update {
            it.copy(
                dataNascimentoPassageiro3 = dataNascimento,
                isDataNascimentoPassageiro3Error = false
            )
        }
    }


    private fun verificaTipoDocumento(
        documento: String,
        tipoDocumento: String,
        uiState: FormPassageiroUiState,
        onAtualizarDocumento: (FormPassageiroUiState, String) -> FormPassageiroUiState,
    ): FormPassageiroUiState {
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

    fun preencherDadosPassageiros(passagem: Passagem) {
        val temPassageiro2 = passagem.temPassageiro2
        val temPassageiro3 = passagem.temPassageiro3

        uiState.update { state ->
            state.copy(
                acomodacao = passagem.acomodacao.orEmpty(),
                tipoPassagem = passagem.tipoPassagem.orEmpty(),
                tipoGratuidade = passagem.gratuidade.orEmpty(),
                tipoDocumentoPassageiro1 = passagem.documentoPassageiro1.orEmpty(),
                isDocumentoPassageiro1Disabled = false,
                documentoPassageiro1 = passagem.numeroDocumentoPassageiro1.orEmpty(),
                nomePassageiro1 = passagem.nomePassageiro1.orEmpty(),
                dataNascimentoPassageiro1 = passagem.dataNascimentoPassageiro1.orEmpty(),
                isPassageiro2Checked = temPassageiro2,
                tipoDocumentoPassageiro2 = passagem.documentoPassageiro2.orEmpty(),
                isDocumentoPassageiro2Disabled = !temPassageiro2,
                documentoPassageiro2 = passagem.numeroDocumentoPassageiro2.orEmpty(),
                nomePassageiro2 = passagem.nomePassageiro2.orEmpty(),
                dataNascimentoPassageiro2 = passagem.dataNascimentoPassageiro2.orEmpty(),
                isPassageiro3Checked = temPassageiro3,
                tipoDocumentoPassageiro3 = passagem.tipoDocumentoPassageiro3.orEmpty(),
                isDocumentoPassageiro3Disabled = !temPassageiro3,
                documentoPassageiro3 = passagem.numeroDocumentoPassageiro3.orEmpty(),
                nomePassageiro3 = passagem.nomePassageiro3.orEmpty(),
                dataNascimentoPassageiro3 = passagem.dataNascimentoPassageiro3.orEmpty(),
            )
        }

        passagem.tipoPassagem?.let { formatarCamposValores(it) }
    }

    fun limparState() {
        uiState.update {
            it.copy(
                tipoDocumentoPassageiro1 = "",
                documentoPassageiro1 = "",
                nomePassageiro1 = "",
                dataNascimentoPassageiro1 = "",
                textDataNascimentoError = 0,
                tipoDocumentoPassageiro2 = "",
                documentoPassageiro2 = "",
                nomePassageiro2 = "",
                dataNascimentoPassageiro2 = "",
                tipoDocumentoPassageiro3 = "",
                documentoPassageiro3 = "",
                nomePassageiro3 = "",
                dataNascimentoPassageiro3 = "",
                acomodacao = "",
                tipoPassagem = "",
                tipoGratuidade = ""
            )
        }
    }
}