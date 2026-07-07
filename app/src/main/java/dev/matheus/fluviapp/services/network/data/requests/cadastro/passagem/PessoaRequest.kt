package dev.matheus.fluviapp.services.network.data.requests.cadastro.passagem

data class PessoaRequest(
    val id: String,
    val nome: String,
    val idTipoDocumento: Int,
    val numeroDocumento: String,
    val dataNascimento: String? = null
)
