package dev.matheus.fluviapp.fakes

import dev.matheus.fluviapp.domain.cadastro.constantes.Constante
import dev.matheus.fluviapp.services.repository.cadastro.ConstanteRepository

/** Fake da porta [ConstanteRepository] para testes de ViewModel. */
class FakeConstanteRepository : ConstanteRepository {
    var constantes: List<Constante> = emptyList()

    override fun sincronizar() = Unit
    override suspend fun obterTodosPorCategoria(categoria: String): List<Constante> =
        constantes.filter { it.categoria == categoria }
    override suspend fun obterTodas(): List<Constante> = constantes
}
