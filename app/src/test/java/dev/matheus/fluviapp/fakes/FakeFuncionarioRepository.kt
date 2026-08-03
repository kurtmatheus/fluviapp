package dev.matheus.fluviapp.fakes

import dev.matheus.fluviapp.domain.operacoes.Funcionario
import dev.matheus.fluviapp.services.repository.operacoes.FuncionarioRepository

/** Fake da porta [FuncionarioRepository] para testes de ViewModel (sem Firestore/Room). */
class FakeFuncionarioRepository : FuncionarioRepository {
    var funcionarios: List<Funcionario> = emptyList()
    val salvos = mutableListOf<Funcionario>()
    val deletados = mutableListOf<String>()

    override fun sincronizar() = Unit
    override suspend fun salvar(funcionario: Funcionario) { salvos += funcionario }
    override suspend fun obterPorId(id: String): Funcionario? = funcionarios.find { it.id == id }
    override suspend fun obterTodasAgencias(): List<String> = funcionarios.map { it.agencia }.distinct()
    override suspend fun obterTodosFuncionarios(): List<Funcionario> = funcionarios
    override suspend fun obterFuncionariosPorAgencia(agencia: String): List<Funcionario> =
        funcionarios.filter { it.agencia == agencia }

    /** No fake não há "servidor": a mesma lista responde — o que importa é o casamento por e-mail. */
    override suspend fun obterPorEmailDoServidor(email: String): Funcionario? =
        funcionarios.find { it.email.equals(email.trim(), ignoreCase = true) }

    override suspend fun deletar(id: String) {
        deletados += id
        funcionarios = funcionarios.filterNot { it.id == id }
    }
}
