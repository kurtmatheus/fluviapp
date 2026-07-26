package dev.matheus.fluviapp.model.rascunho

import dev.matheus.fluviapp.ui.states.passagem.FormPassageiroUiState
import dev.matheus.fluviapp.ui.states.passagem.FormPassagemUiState
import dev.matheus.fluviapp.ui.states.passagem.FormVeiculoUiState

/**
 * Transições volátil↔cacheada do rascunho de passagem (mappers puros, ADR-0003).
 * - [montarRascunho]: volátil (3 states) → cacheada (snapshot). Só copia valores.
 * - [aplicarEm]: cacheada (snapshot) → volátil. Faz `copy` sobre os states EXISTENTES, então as
 *   lambdas e listas já injetadas são preservadas; restaura apenas os valores. Nunca deriva valor
 *   por cima do restaurado (lição do AS).
 */
fun montarRascunho(
    passagem: FormPassagemUiState,
    passageiro: FormPassageiroUiState,
    veiculo: FormVeiculoUiState,
): RascunhoPassagemSnapshot = RascunhoPassagemSnapshot(
    dataViagem = passagem.dataViagem,
    horaViagem = passagem.horaViagem,
    agencia = passagem.agencia,
    agente = passagem.agente,
    isVeiculoChecked = passagem.isVeiculoChecked,
    valorPago = passagem.valorPago,
    isPixChecked = passagem.isPixChecked,
    valorPix = passagem.valorPix,
    isDinheiroChecked = passagem.isDinheiroChecked,
    valorDinheiro = passagem.valorDinheiro,
    isDebitoChecked = passagem.isDebitoChecked,
    valorDebito = passagem.valorDebito,
    isCreditoChecked = passagem.isCreditoChecked,
    valorCredito = passagem.valorCredito,
    observacao = passagem.observacao,
    viagemId = passagem.viagemId,
    navioId = passagem.navioId,
    empresaId = passagem.empresaId,
    empresaViagem = passagem.empresaViagem,
    navioViagem = passagem.navioViagem,
    origemViagem = passagem.origemViagem,
    destinoViagem = passagem.destinoViagem,
    codigoViagem = passagem.codigoViagem,
    tipoDocumentoPassageiro1 = passageiro.tipoDocumentoPassageiro1,
    documentoPassageiro1 = passageiro.documentoPassageiro1,
    nomePassageiro1 = passageiro.nomePassageiro1,
    dataNascimentoPassageiro1 = passageiro.dataNascimentoPassageiro1,
    isPassageiro2Checked = passageiro.isPassageiro2Checked,
    tipoDocumentoPassageiro2 = passageiro.tipoDocumentoPassageiro2,
    documentoPassageiro2 = passageiro.documentoPassageiro2,
    nomePassageiro2 = passageiro.nomePassageiro2,
    dataNascimentoPassageiro2 = passageiro.dataNascimentoPassageiro2,
    isPassageiro3Checked = passageiro.isPassageiro3Checked,
    tipoDocumentoPassageiro3 = passageiro.tipoDocumentoPassageiro3,
    documentoPassageiro3 = passageiro.documentoPassageiro3,
    nomePassageiro3 = passageiro.nomePassageiro3,
    dataNascimentoPassageiro3 = passageiro.dataNascimentoPassageiro3,
    acomodacao = passageiro.acomodacao,
    tipoPassagem = passageiro.tipoPassagem,
    tipoGratuidade = passageiro.tipoGratuidade,
    tipoDocumentoResponsavelRetirada = veiculo.tipoDocumentoResponsavelRetirada,
    documentoResponsavelRetirada = veiculo.documentoResponsavelRetirada,
    nomeResponsavelRetirada = veiculo.nomeResponsavelRetirada,
    tipoVeiculo = veiculo.tipoVeiculo,
    modeloVeiculo = veiculo.modeloVeiculo,
    placaVeiculo = veiculo.placaVeiculo,
    corVeiculo = veiculo.corVeiculo,
)

data class EstadosPassagemRestaurados(
    val passagem: FormPassagemUiState,
    val passageiro: FormPassageiroUiState,
    val veiculo: FormVeiculoUiState,
)

fun RascunhoPassagemSnapshot.aplicarEm(
    passagem: FormPassagemUiState,
    passageiro: FormPassageiroUiState,
    veiculo: FormVeiculoUiState,
): EstadosPassagemRestaurados = EstadosPassagemRestaurados(
    passagem = passagem.copy(
        dataViagem = dataViagem,
        horaViagem = horaViagem,
        agencia = agencia,
        agente = agente,
        isVeiculoChecked = isVeiculoChecked,
        valorPago = valorPago,
        isPixChecked = isPixChecked,
        valorPix = valorPix,
        isDinheiroChecked = isDinheiroChecked,
        valorDinheiro = valorDinheiro,
        isDebitoChecked = isDebitoChecked,
        valorDebito = valorDebito,
        isCreditoChecked = isCreditoChecked,
        valorCredito = valorCredito,
        observacao = observacao,
        viagemId = viagemId,
        navioId = navioId,
        empresaId = empresaId,
        empresaViagem = empresaViagem,
        navioViagem = navioViagem,
        origemViagem = origemViagem,
        destinoViagem = destinoViagem,
        codigoViagem = codigoViagem,
    ),
    passageiro = passageiro.copy(
        tipoDocumentoPassageiro1 = tipoDocumentoPassageiro1,
        documentoPassageiro1 = documentoPassageiro1,
        nomePassageiro1 = nomePassageiro1,
        dataNascimentoPassageiro1 = dataNascimentoPassageiro1,
        isPassageiro2Checked = isPassageiro2Checked,
        tipoDocumentoPassageiro2 = tipoDocumentoPassageiro2,
        documentoPassageiro2 = documentoPassageiro2,
        nomePassageiro2 = nomePassageiro2,
        dataNascimentoPassageiro2 = dataNascimentoPassageiro2,
        isPassageiro3Checked = isPassageiro3Checked,
        tipoDocumentoPassageiro3 = tipoDocumentoPassageiro3,
        documentoPassageiro3 = documentoPassageiro3,
        nomePassageiro3 = nomePassageiro3,
        dataNascimentoPassageiro3 = dataNascimentoPassageiro3,
        acomodacao = acomodacao,
        tipoPassagem = tipoPassagem,
        tipoGratuidade = tipoGratuidade,
    ),
    veiculo = veiculo.copy(
        tipoDocumentoResponsavelRetirada = tipoDocumentoResponsavelRetirada,
        documentoResponsavelRetirada = documentoResponsavelRetirada,
        nomeResponsavelRetirada = nomeResponsavelRetirada,
        tipoVeiculo = tipoVeiculo,
        modeloVeiculo = modeloVeiculo,
        placaVeiculo = placaVeiculo,
        corVeiculo = corVeiculo,
    ),
)