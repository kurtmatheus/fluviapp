package dev.matheus.fluviapp.fakes

import dev.matheus.fluviapp.domain.operacoes.Convite
import dev.matheus.fluviapp.services.repository.operacoes.ConviteRepository

/** Fake da porta [ConviteRepository] — o id é o e-mail, como na impl. */
class FakeConviteRepository : ConviteRepository {
    var convites: List<Convite> = emptyList()
    val salvos = mutableListOf<Convite>()

    override suspend fun salvar(convite: Convite) {
        salvos += convite
        convites = convites.filterNot { it.email == convite.email } + convite
    }

    override suspend fun obterTodos(): List<Convite> = convites

    override suspend fun obterPorEmail(email: String): Convite? =
        convites.find { it.email.equals(email.trim(), ignoreCase = true) }

    override suspend fun marcarComoUsado(email: String) {
        convites = convites.map { if (it.email.equals(email, ignoreCase = true)) it.copy(usado = true) else it }
    }
}