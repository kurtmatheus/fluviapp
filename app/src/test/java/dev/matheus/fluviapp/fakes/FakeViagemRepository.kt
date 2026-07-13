package dev.matheus.fluviapp.fakes

import dev.matheus.fluviapp.model.viagem.Viagem
import dev.matheus.fluviapp.services.repository.firebase.ViagemRepository

/** Fake da porta [ViagemRepository] para testes de ViewModel. */
class FakeViagemRepository : ViagemRepository {
    var viagens: List<Viagem> = emptyList()
    val salvos = mutableListOf<SalvarArgs>()

    data class SalvarArgs(
        val id: String?,
        val navio: String,
        val empresa: String,
        val origem: String,
        val destino: String,
    )

    override fun sincronizar() = Unit
    override suspend fun salvar(id: String?, navio: String, empresa: String, origem: String, destino: String) {
        salvos += SalvarArgs(id, navio, empresa, origem, destino)
    }
    override suspend fun obterPorId(id: String): Viagem = viagens.first { it.id == id }
    override suspend fun obterPorCodigo(codigo: String): Viagem = viagens.first { it.codigo == codigo }
    override suspend fun obterTodas(): List<Viagem> = viagens
    override suspend fun deletar(id: String) = Unit
}
