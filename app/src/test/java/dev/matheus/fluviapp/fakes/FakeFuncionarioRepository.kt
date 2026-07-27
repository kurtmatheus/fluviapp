package dev.matheus.fluviapp.fakes

import dev.matheus.fluviapp.model.operacoes.Funcionario
import dev.matheus.fluviapp.services.repository.operacoes.FuncionarioRepository

/** Fake da porta [FuncionarioRepository] para testes de ViewModel (sem Firestore/Room). */
class FakeFuncionarioRepository : FuncionarioRepository {
    var agentes: List<Funcionario> = emptyList()
    val salvos = mutableListOf<Funcionario>()
    val deletados = mutableListOf<String>()

    override fun sincronizar() = Unit
    override suspend fun salvar(agente: Funcionario) { salvos += agente }
    override suspend fun obterPorId(id: String): Funcionario? = agentes.find { it.id == id }
    override suspend fun obterTodasAgencias(): List<String> = agentes.map { it.agencia }.distinct()
    override suspend fun obterTodosFuncionarios(): List<Funcionario> = agentes
    override suspend fun obterFuncionariosPorAgencia(agencia: String): List<Funcionario> =
        agentes.filter { it.agencia == agencia }

    override suspend fun deletar(id: String) {
        deletados += id
        agentes = agentes.filterNot { it.id == id }
    }
}
