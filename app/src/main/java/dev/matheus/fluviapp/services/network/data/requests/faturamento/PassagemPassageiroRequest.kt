package dev.matheus.fluviapp.services.network.data.requests.faturamento

data class PassagemPassageiroRequest(
    val idPassagem: String,
    val idPassageiro1: String,
    val idPassageiro2: String? = null,
    val idPassageiro3: String? = null,
    val idAcomodacao: Int,
    val idTipoPassagem: Int,
    val idTipoGratuidade: Int,
)
