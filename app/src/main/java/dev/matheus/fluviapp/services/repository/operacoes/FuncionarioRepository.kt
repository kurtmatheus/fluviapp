package dev.matheus.fluviapp.services.repository.operacoes

import dev.matheus.fluviapp.model.operacoes.Funcionario

/**
 * Porta do repositório de agentes (DIP) — os ViewModels dependem desta interface, não da impl
 * Firestore. Testes usam um fake; produção usa [FuncionarioFirestoreRepository].
 */
interface FuncionarioRepository {
    fun sincronizar()
    suspend fun salvar(funcionario: Funcionario)
    suspend fun obterPorId(id: String): Funcionario?
    suspend fun obterTodasAgencias(): List<String>
    suspend fun obterTodosFuncionarios(): List<Funcionario>
    suspend fun obterFuncionariosPorAgencia(agencia: String): List<Funcionario>
    suspend fun deletar(id: String)

    companion object {
        const val COLLECTION_FUNCIONARIOS = "funcionarios"
    }
}
