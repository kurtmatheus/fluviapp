package dev.matheus.fluviapp.services.repository.operacoes

import dev.matheus.fluviapp.domain.operacoes.Funcionario
import kotlinx.coroutines.flow.StateFlow

/**
 * Porta do repositório de funcionários (DIP) — os ViewModels dependem desta interface, não da impl
 * Firestore. Testes usam um fake; produção usa [FuncionarioFirestoreRepository].
 *
 * Desde a F6.2 a coleção vive **só no Firestore** (ADR-0017 D1), e a porta ganhou o que faltava para
 * isso: [observarTodos], a janela para o `StateFlow` que o listener alimenta. As duas consultas por
 * String de agência saíram na F6.5, com o campo que as sustentava: recortar por empresa é filtrar por
 * `empresaIds` na lista que o listener já entregou.
 */
interface FuncionarioRepository {
    fun sincronizar()
    fun observarTodos(): StateFlow<List<Funcionario>>

    /** Salva e devolve **o id** — necessário porque a criação o gera. */
    suspend fun salvar(funcionario: Funcionario): String

    suspend fun obterPorId(id: String): Funcionario?
    suspend fun obterTodosFuncionarios(): List<Funcionario>
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
