package dev.matheus.fluviapp.services.repository.firebase.documents

data class VeiculoDocumento(
    val nomeResponsavelRetirada: String? = null,
    val documentoResponsavelRetirada: String? = null,
    val numeroDocumentoResponsavelRetirada: String? = null,
    val tipoVeiculo: String? = null,
    val modeloVeiculo: String? = null,
    val placaVeiculo: String? = null,
    val corVeiculo: String? = null,
    // Cilindrada da moto (ADR-0013): registra o cc que justificou a tarifaBase (regra por cilindrada).
    val cilindrada: String? = null,
)
