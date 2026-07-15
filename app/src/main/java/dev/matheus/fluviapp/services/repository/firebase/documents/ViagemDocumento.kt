package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.model.viagem.Viagem

data class ViagemDocumento(
    val codigo: String = "",
    val empresa: String = "",
    val navio: String = "",
    val origem: String = "",
    val destino: String = "",
    // Vínculo vivo por id (ADR-0008). empresa/navio (nomes) coexistem: são substrato do snapshot da
    // Passagem e da derivação do código. Default "" cobre docs antigos (schemaless).
    val empresaId: String = "",
    val navioId: String = "",
)

fun ViagemDocumento.toViagem(id: String): Viagem {
    // empresa/navio (nomes) do doc são ignorados na entidade — o vínculo é por id (ADR-0008 Fase 3);
    // os nomes seguem no doc só para o papel de snapshot embutido na Passagem.
    return Viagem(
        id = id,
        codigo = codigo,
        origem = origem,
        destino = destino,
        empresaId = empresaId,
        navioId = navioId,
    )
}
