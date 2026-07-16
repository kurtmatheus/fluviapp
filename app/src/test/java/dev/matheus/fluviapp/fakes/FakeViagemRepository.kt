package dev.matheus.fluviapp.fakes

import dev.matheus.fluviapp.model.viagem.Viagem
import dev.matheus.fluviapp.services.repository.firebase.ViagemRepository

/** Fake da porta [ViagemRepository] para testes de ViewModel. */
class FakeViagemRepository : ViagemRepository {
    var viagens: List<Viagem> = emptyList()
    val salvos = mutableListOf<Viagem>()
    val deletados = mutableListOf<String>()
    var falharAoDeletar = false

    override fun sincronizar() = Unit
    override suspend fun salvar(viagem: Viagem) { salvos += viagem }
    override suspend fun obterPorId(id: String): Viagem = viagens.first { it.id == id }
    override suspend fun obterTodas(): List<Viagem> = viagens
    override suspend fun deletar(id: String) {
        if (falharAoDeletar) throw RuntimeException("falha simulada")
        deletados += id
    }
}
