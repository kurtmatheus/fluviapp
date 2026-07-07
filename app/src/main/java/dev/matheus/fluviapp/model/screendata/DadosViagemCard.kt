package dev.matheus.fluviapp.model.screendata

data class DadosViagemCard(
    val idViagem: String = "",
    val codigo: String = "",
    val empresa: String = "",
    val navio: String = "",
    val origem: String = "",
    val destino: String = "",
    val data: String = "",
    val hora:String = "",
    val capacidadeVeiculos: String = "0",
    val capacidadeSuites: String = "0",
    val capacidadeSuites2Pessoas: String = "0",
    val capacidadeSuites3Pessoas: String = "0",
    val capacidadeCamarotes: String = "0"
)
