package dev.matheus.fluviapp.model.rascunho

/**
 * Rascunho da passagem em edição (memória CACHEADA — ADR-0003/0004): só VALORES, sem lambdas nem
 * flags de erro (que são recomputados na validação) nem listas (que vêm de repositório). É o que
 * viaja como JSON e sobrevive a um crash do processo. Restore é puro (ver RascunhoPassagemMapper).
 */
data class RascunhoPassagemSnapshot(
    // passagem
    val dataViagem: String = "",
    val horaViagem: String = "",
    val agencia: String = "",
    val agente: String = "",
    val isVeiculoChecked: Boolean = false,
    val valorPago: String = "",
    val isPixChecked: Boolean = false,
    val valorPix: String = "",
    val isDinheiroChecked: Boolean = false,
    val valorDinheiro: String = "",
    val isDebitoChecked: Boolean = false,
    val valorDebito: String = "",
    val isCreditoChecked: Boolean = false,
    val valorCredito: String = "",
    val observacao: String = "",
    val viagemId: String = "",
    val navioId: String = "",
    val empresaId: String = "",
    val empresaViagem: String = "",
    val navioViagem: String = "",
    val origemViagem: String = "",
    val destinoViagem: String = "",
    val codigoViagem: String = "",

    // passageiro(s) + acomodação/tipo
    val tipoDocumentoPassageiro1: String = "",
    val documentoPassageiro1: String = "",
    val nomePassageiro1: String = "",
    val dataNascimentoPassageiro1: String = "",
    val isPassageiro2Checked: Boolean = false,
    val tipoDocumentoPassageiro2: String = "",
    val documentoPassageiro2: String = "",
    val nomePassageiro2: String = "",
    val dataNascimentoPassageiro2: String = "",
    val isPassageiro3Checked: Boolean = false,
    val tipoDocumentoPassageiro3: String = "",
    val documentoPassageiro3: String = "",
    val nomePassageiro3: String = "",
    val dataNascimentoPassageiro3: String = "",
    val acomodacao: String = "",
    val tipoPassagem: String = "",
    val tipoGratuidade: String = "",

    // veículo + responsável pela retirada
    val tipoDocumentoResponsavelRetirada: String = "",
    val documentoResponsavelRetirada: String = "",
    val nomeResponsavelRetirada: String = "",
    val tipoVeiculo: String = "",
    val modeloVeiculo: String = "",
    val placaVeiculo: String = "",
    val corVeiculo: String = "",
)