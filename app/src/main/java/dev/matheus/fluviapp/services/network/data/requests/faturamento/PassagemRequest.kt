package dev.matheus.fluviapp.services.network.data.requests.faturamento

data class PassagemRequest(
    val id: String,
    val numeroBilhete: Int,
    val idViagem: String,
    val dataViagem: String,
    val horaViagem: String,
    val idAgente: Int,
    val valorAvulso: Double,
    val valorPix: Double,
    val valorDinheiro: Double,
    val valorDebito: Double,
    val valorCredito: Double,
    val valorDesconto: Double,
    val idCategoria: Int,
    val idSituacao: Int,
    val idFuncionario: Int
)
