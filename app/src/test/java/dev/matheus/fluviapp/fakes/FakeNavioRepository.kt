package dev.matheus.fluviapp.fakes

import dev.matheus.fluviapp.model.viagem.Navio
import dev.matheus.fluviapp.services.repository.cadastro.viagem.NavioRepository

/** Fake da porta [NavioRepository] para testes de ViewModel. */
class FakeNavioRepository : NavioRepository {
    var navios: List<Navio> = emptyList()

    override fun sincronizar() = Unit
    override suspend fun obterTodos(): List<Navio> = navios
    override suspend fun obterPorId(id: String): Navio = navios.first { it.id == id }
    override suspend fun salvar(navio: Navio) = Unit
    override suspend fun obterPorNome(nome: String): Navio = navios.first { it.descricaoNome == nome }
}
