package dev.matheus.fluviapp.fakes

import dev.matheus.fluviapp.domain.operacoes.Convite
import dev.matheus.fluviapp.services.repository.operacoes.ConviteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake da porta [ConviteRepository] — o id é o e-mail, como na impl.
 *
 * A lista é **observável** pelo mesmo motivo do fake dos perfis: *convite pendente* é a diferença entre as
 * duas coleções, e uma delas fotografada faria o número parar de cair quando alguém entra.
 */
class FakeConviteRepository : ConviteRepository {
    private val _convites = MutableStateFlow<List<Convite>>(emptyList())

    var convites: List<Convite>
        get() = _convites.value
        set(valor) { _convites.value = valor }

    val salvos = mutableListOf<Convite>()

    override suspend fun salvar(convite: Convite) {
        salvos += convite
        convites = convites.filterNot { it.email == convite.email } + convite
    }

    /** Quantas vezes alguém pediu a lista — o par do contador dos perfis, e pela mesma razão. */
    var leituras = 0
        private set

    override fun observarTodos(): StateFlow<List<Convite>> = _convites.asStateFlow()

    override suspend fun obterTodos(): List<Convite> {
        leituras++
        return convites
    }

    override suspend fun obterPorEmail(email: String): Convite? =
        convites.find { it.email.equals(email.trim(), ignoreCase = true) }

    override suspend fun marcarComoUsado(email: String) {
        convites = convites.map { if (it.email.equals(email, ignoreCase = true)) it.copy(usado = true) else it }
    }
}
