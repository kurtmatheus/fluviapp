package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.model.viagem.Navio

data class NavioDocumento(
    val nome: String = "",
    val capacidadeVeiculo: Int = 0,
    val capacidadeSuite2: Int = 0,
    val capacidadeSuite3: Int = 0,
    val capacidadeCamarote: Int = 0,
    val empresa: String = ""
)

fun NavioDocumento.toNavio(id: String): Navio {
    return Navio(
        id = id,
        descricaoNome = nome,
        capacidadeVeiculo = capacidadeVeiculo,
        capacidadeSuite2 = capacidadeSuite2,
        capacidadeSuite3 = capacidadeSuite3,
        capacidadeCamarote = capacidadeCamarote,
        empresa = empresa
    )
}