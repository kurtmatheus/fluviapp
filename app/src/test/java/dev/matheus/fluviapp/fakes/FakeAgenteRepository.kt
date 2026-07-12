package dev.matheus.fluviapp.fakes

import dev.matheus.fluviapp.model.cadastro.passagem.Agente
import dev.matheus.fluviapp.services.repository.cadastro.passagem.AgenteRepository

/** Fake da porta [AgenteRepository] para testes de ViewModel (sem Firestore/Room). */
class FakeAgenteRepository : AgenteRepository {
    var agentes: List<Agente> = emptyList()
    val salvos = mutableListOf<Agente>()

    override fun sincronizar() = Unit
    override suspend fun salvar(agente: Agente) { salvos += agente }
    override suspend fun obterPorId(id: String): Agente? = agentes.find { it.id == id }
    override suspend fun obterTodasAgencias(): List<String> = agentes.map { it.agencia }.distinct()
    override suspend fun obterTodosAgentes(): List<Agente> = agentes
    override suspend fun obterAgentesPorAgencia(agencia: String): List<Agente> =
        agentes.filter { it.agencia == agencia }
}
