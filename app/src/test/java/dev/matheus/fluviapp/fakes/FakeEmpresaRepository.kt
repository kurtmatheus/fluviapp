package dev.matheus.fluviapp.fakes

import dev.matheus.fluviapp.model.viagem.Empresa
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaRepository

/** Fake da porta [EmpresaRepository] para testes de ViewModel (sem Firestore/Room). */
class FakeEmpresaRepository : EmpresaRepository {
    var empresas: List<Empresa> = emptyList()
    val salvos = mutableListOf<Empresa>()
    val deletados = mutableListOf<String>()
    var falharAoSalvar = false

    override fun sincronizar() = Unit
    override suspend fun salvar(empresa: Empresa) {
        if (falharAoSalvar) throw RuntimeException("falha simulada")
        salvos += empresa
    }
    override suspend fun obterTodas(): List<Empresa> = empresas
    override suspend fun obterPorId(id: String): Empresa? = empresas.find { it.id == id }
    override suspend fun obterPorNome(nome: String): Empresa = empresas.first { it.nome == nome }
    override suspend fun deletar(id: String) {
        deletados += id
        empresas = empresas.filterNot { it.id == id }
    }
}
