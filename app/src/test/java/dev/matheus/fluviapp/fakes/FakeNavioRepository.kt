package dev.matheus.fluviapp.fakes

import dev.matheus.fluviapp.domain.viagem.Navio
import dev.matheus.fluviapp.services.repository.cadastro.viagem.NavioRepository

/** Fake da porta [NavioRepository] para testes de ViewModel (sem Firestore/Room). */
class FakeNavioRepository : NavioRepository {
    var navios: List<Navio> = emptyList()
    val salvos = mutableListOf<Navio>()
    val deletados = mutableListOf<String>()
    var falharAoSalvar = false

    override fun sincronizar() = Unit
    override suspend fun obterTodos(): List<Navio> = navios
    override suspend fun obterPorId(id: String): Navio? = navios.find { it.id == id }
    override suspend fun salvar(navio: Navio) {
        if (falharAoSalvar) throw RuntimeException("falha simulada")
        salvos += navio
    }
    override suspend fun obterPorNome(nome: String): Navio = navios.first { it.descricaoNome == nome }
    override suspend fun deletar(id: String) {
        deletados += id
        navios = navios.filterNot { it.id == id }
    }
}
