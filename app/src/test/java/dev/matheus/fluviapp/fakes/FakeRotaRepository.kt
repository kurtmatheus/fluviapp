package dev.matheus.fluviapp.fakes

import dev.matheus.fluviapp.domain.rota.Rota
import dev.matheus.fluviapp.services.repository.cadastro.rota.RotaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake da porta [RotaRepository].
 *
 * O `inativar` **não remove** — como a impl. Se ele removesse, o teste do pool passaria por um motivo
 * diferente do que acontece em produção, e a diferença entre "sumiu porque foi apagada" e "sumiu porque
 * quem lista filtra `ativo`" ficaria sem cobertura.
 */
class FakeRotaRepository : RotaRepository {
    private val _rotas = MutableStateFlow<List<Rota>>(emptyList())

    var rotas: List<Rota>
        get() = _rotas.value
        set(valor) { _rotas.value = valor }

    val criadas = mutableListOf<Rota>()
    var falharAoCriar = false

    var sincronizou = false
        private set

    override fun sincronizar() { sincronizou = true }

    override fun observarTodas(): StateFlow<List<Rota>> = _rotas.asStateFlow()

    override suspend fun criar(rota: Rota): String {
        if (falharAoCriar) throw RuntimeException("falha simulada")
        val id = "rota-gerada-${criadas.size + 1}"
        val comId = rota.copy(id = id)
        criadas += comId
        rotas = rotas + comId
        return id
    }

    override suspend fun obterTodas(): List<Rota> = rotas

    override suspend fun obterPorId(id: String): Rota? = rotas.find { it.id == id }

    override suspend fun inativar(id: String) {
        rotas = rotas.map { if (it.id == id) it.copy(ativo = false) else it }
    }
}