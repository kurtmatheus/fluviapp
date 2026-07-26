package dev.matheus.fluviapp.ui.viewmodel.helpers.passagem

import dev.matheus.fluviapp.extensions.extrairLetrasOuNumeros
import dev.matheus.fluviapp.extensions.extrairNumeros
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Categoria.ACOMODACAO
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Categoria.GRATUIDADE
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Categoria.TIPO_PASSAGEM
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.CNPJ
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.CPF
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.PASSAPORTE
import dev.matheus.fluviapp.model.cadastro.constantes.Constante.Descricao.REDE
import dev.matheus.fluviapp.model.passagem.Passagem
import dev.matheus.fluviapp.services.repository.cadastro.ConstanteRepository
import dev.matheus.fluviapp.services.repository.firebase.PassagemFirestoreRepository
import dev.matheus.fluviapp.ui.states.passagem.FormPassageiroUiState
import dev.matheus.fluviapp.ui.states.passagem.FormPassagemUiState
import dev.matheus.fluviapp.ui.viewmodel.passagem.TAMANHO_CNPJ
import dev.matheus.fluviapp.ui.viewmodel.passagem.TAMANHO_CPF
import dev.matheus.fluviapp.ui.viewmodel.passagem.TAMANHO_PASS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

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
                listaNomePassageiro = passagemRepository.getListaNome(),
            )
        }
    }

    /** Carga suspensa das listas (molde ADR-0006): sem `runBlocking` na thread principal no init. */
    suspend fun carregarListas() {
        uiState.update {
            it.copy(
                listaAcomodacao = constanteRepository.obterTodosPorCategoria(ACOMODACAO.name),
                listaTipoPassagem = constanteRepository.obterTodosPorCategoria(TIPO_PASSAGEM.name),
                listaTipoGratuidade = constanteRepository.obterTodosPorCategoria(GRATUIDADE.name),
            )
        }
    }

    internal fun atualizarAcomodacao(acomodacao: String) {
        uiState.update { state ->
            state.copy(
                acomodacao = acomodacao,
                isAcomodacaoError = false,
                // Tipo tarifário só na REDE (ADR-0013): ao sair da rede, limpa tipo/gratuidade — senão fica
                // valor obsoleto que precificaria errado (ex.: suíte herdando GRATUIDADE de uma rede anterior).
                tipoPassagem = if (acomodacao == REDE.name) state.tipoPassagem else "",
                tipoGratuidade = if (acomodacao == REDE.name) state.tipoGratuidade else "",
                isTipoPassagemError = false,
                isTipoGratuidadeError = false,
            )
        }
        // Fora da rede é inteira (paga): reabilita os campos de pagamento (ramo não-gratuidade).
        if (acomodacao != REDE.name) {
            formatarCamposValores("")
        }
    }

    internal fun atualizarTipoPassagem(tipo: String) {
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
                    isValorPagoError = false,
                    isValorPagoEnabled = false
                )
            }
        } else {
            uiStatePassagem.update {
                it.copy(
                    isValorPagoError = false,
                    isValorPagoEnabled = true
                )
            }
        }
    }

    internal fun atualizarTipoGratuidade(tipo: String) {
        uiState.update {
            it.copy(
                tipoGratuidade = tipo,
                isTipoGratuidadeError = false
            )
        }
    }

    internal fun atualizarTipoDocumentoPassageiro1(tipoDocumento: String) {
        uiState.update {
            it.copy(
                tipoDocumentoPassageiro1 = tipoDocumento,
                isTipoDocumentoPassageiro1Error = false,
                isDocumentoPassageiro1Disabled = false
            )
        }
    }

    internal fun limparDocumentoPassageiro1() {
        uiState.update {
            it.copy(
                tipoDocumentoPassageiro1 = "",
                documentoPassageiro1 = "",
                isDocumentoPassageiro1Disabled = true
            )
        }
    }

    internal fun atualizaDocumentoPassageiro1(documento: String) {
        uiState.update { uiState ->
            verificaTipoDocumento(documento.extrairLetrasOuNumeros(), uiState.tipoDocumentoPassageiro1, uiState) { state, documento ->
                state.copy(
                    documentoPassageiro1 = documento,
                    isDocumentoPassageiro1Error = false
                )
            }
        }
    }

    internal fun atualizarNomePassageiro1(nome: String) {
        uiState.update {
            it.copy(
                nomePassageiro1 = nome,
                isNomePassageiro1Error = false
            )
        }
    }

    internal fun atualizarDataNascimentoPassageiro1(dataNascimento: String) {
        uiState.update {
            it.copy(
                dataNascimentoPassageiro1 = dataNascimento,
                isDataNascimentoPassageiro1Error = false
            )
        }
    }

    internal fun checkPassageiro2() {
        uiState.update {
            it.copy(
                isPassageiro2Checked = !it.isPassageiro2Checked
            )
        }
    }

    internal fun atualizarTipoDocumentoPassageiro2(tipoDocumento: String) {
        uiState.update {
            it.copy(
                tipoDocumentoPassageiro2 = tipoDocumento,
                isTipoDocumentoPassageiro2Error = false,
                isDocumentoPassageiro2Disabled = false
            )
        }
    }

    internal fun limparDocumentoPassageiro2() {
        uiState.update {
            it.copy(
                tipoDocumentoPassageiro2 = "",
                documentoPassageiro2 = "",
                isDocumentoPassageiro2Disabled = true
            )
        }
    }

    internal fun atualizaDocumentoPassageiro2(documento: String) {
        uiState.update { uiState ->
            verificaTipoDocumento(documento.extrairLetrasOuNumeros(), uiState.tipoDocumentoPassageiro2, uiState) { state, documento ->
                state.copy(
                    documentoPassageiro2 = documento,
                    isDocumentoPassageiro2Error = false
                )
            }
        }
    }

    internal fun atualizarNomePassageiro2(nome: String) {
        uiState.update {
            it.copy(
                nomePassageiro2 = nome,
                isNomePassageiro2Error = false
            )
        }
    }

    internal fun atualizarDataNascimentoPassageiro2(dataNascimento: String) {
        uiState.update {
            it.copy(
                dataNascimentoPassageiro2 = dataNascimento,
                isDataNascimentoPassageiro2Error = false
            )
        }
    }

    internal fun checkPassageiro3() {
        uiState.update {
            it.copy(
                isPassageiro3Checked = !it.isPassageiro3Checked
            )
        }
    }

    internal fun atualizarTipoDocumentoPassageiro3(tipoDocumento: String) {
        uiState.update {
            it.copy(
                tipoDocumentoPassageiro3 = tipoDocumento,
                isTipoDocumentoPassageiro3Error = false,
                isDocumentoPassageiro3Disabled = false
            )
        }
    }

    internal fun limparDocumentoPassageiro3() {
        uiState.update {
            it.copy(
                tipoDocumentoPassageiro3 = "",
                documentoPassageiro3 = "",
                isDocumentoPassageiro3Disabled = true
            )
        }
    }

    internal fun atualizaDocumentoPassageiro3(documento: String) {
        uiState.update { uiState ->
            verificaTipoDocumento(documento.extrairLetrasOuNumeros(), uiState.tipoDocumentoPassageiro3, uiState) { state, documento ->
                state.copy(
                    documentoPassageiro3 = documento,
                    isDocumentoPassageiro3Error = false
                )
            }
        }
    }

    internal fun atualizarNomePassageiro3(nome: String) {
        uiState.update {
            it.copy(
                nomePassageiro3 = nome,
                isNomePassageiro3Error = false
            )
        }
    }

    internal fun atualizarDataNascimentoPassageiro3(dataNascimento: String) {
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