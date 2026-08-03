package dev.matheus.fluviapp.services.repository.rascunho

import dev.matheus.fluviapp.domain.rascunho.RascunhoPassagemSnapshot

/** Store em memória para testes de VM do flip (slot único). */
class FakeRascunhoStore : RascunhoStore {

    var atual: RascunhoPassagemSnapshot? = null
        private set

    var vezesSalvo = 0
        private set

    override suspend fun salvar(snapshot: RascunhoPassagemSnapshot) {
        atual = snapshot
        vezesSalvo++
    }

    override suspend fun recuperar(): RascunhoPassagemSnapshot? = atual

    override suspend fun remover() {
        atual = null
    }
}