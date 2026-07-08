package dev.matheus.fluviapp.services.repository.rascunho

import dev.matheus.fluviapp.model.rascunho.RascunhoPassagemSnapshot

/**
 * Porta do rascunho de passagem (ADR-0004). DIP: o fluxo não conhece o mecanismo de persistência
 * (Room-JSON hoje). Slot único — há um rascunho em edição por vez. Invariante: `existe ⇔ é rascunho`.
 */
interface RascunhoStore {
    suspend fun salvar(snapshot: RascunhoPassagemSnapshot)
    suspend fun recuperar(): RascunhoPassagemSnapshot?
    suspend fun remover()
}