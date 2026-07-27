package dev.matheus.fluviapp.services.repository.operacoes

import dev.matheus.fluviapp.model.operacoes.Funcionario

/**
 * Porta do repositório de funcionários (DIP) — os ViewModels dependem desta interface, não da impl
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

    /**
     * Busca **no servidor**, por e-mail — a única leitura desta porta que não passa pelo espelho Room
     * (ADR-0003). É o primeiro acesso (ADR-0015 §2.1): a pessoa acabou de autenticar, nada foi
     * sincronizado ainda, e é justamente este achado que distingue "primeiro acesso" de "não é da casa".
     */
    suspend fun obterPorEmailDoServidor(email: String): Funcionario?

    companion object {
        const val COLLECTION_FUNCIONARIOS = "funcionarios"
    }
}
