package dev.matheus.fluviapp.services.repository.cadastro

import dev.matheus.fluviapp.model.cadastro.constantes.Constante

/**
 * Porta do repositório de constantes (DIP). Testes usam um fake; produção usa
 * [ConstanteFirestoreRepository].
 */
interface ConstanteRepository {
    fun sincronizar()
    suspend fun obterTodosPorCategoria(categoria: String): List<Constante>
    suspend fun obterTodas(): List<Constante>

    companion object {
        const val COLLECTION_CONSTANTS = "constants"
    }
}
