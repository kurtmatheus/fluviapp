package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.model.viagem.Navio

data class NavioDocumento(
    val nome: String = "",
    val capacidadeVeiculo: Int = 0,
    val capacidadeSuite2: Int = 0,
    val capacidadeSuite3: Int = 0,
    val capacidadeCamarote: Int = 0,
    val empresa: String = "",
    // Link estável para Empresa (ADR-0008, Fase 0/1). Dormente: coexiste com `empresa` (nome),
    // que segue sendo o campo lido. Default "" cobre docs antigos (schemaless).
    val empresaId: String = "",
)

fun NavioDocumento.toNavio(id: String): Navio {
    return Navio(
        id = id,
        descricaoNome = nome,
        capacidadeVeiculo = capacidadeVeiculo,
        capacidadeSuite2 = capacidadeSuite2,
        capacidadeSuite3 = capacidadeSuite3,
        capacidadeCamarote = capacidadeCamarote,
        empresa = empresa,
        empresaId = empresaId,
    )
}