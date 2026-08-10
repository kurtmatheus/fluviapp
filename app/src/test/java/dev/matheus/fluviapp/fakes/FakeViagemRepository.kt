package dev.matheus.fluviapp.fakes

import dev.matheus.fluviapp.domain.viagem.Viagem
import dev.matheus.fluviapp.services.repository.cadastro.viagem.ViagemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake da porta [ViagemRepository].
 *
 * O `inativar` **não remove** — como a impl. Se removesse, o teste passaria por um motivo diferente do
 * que acontece em produção, e a diferença entre "sumiu porque foi apagada" e "sumiu porque quem lista
 * filtra `ativo`" ficaria sem cobertura.
 */
class FakeViagemRepository : ViagemRepository {
    private val _viagens = MutableStateFlow<List<Viagem>>(emptyList())

    var viagens: List<Viagem>
        get() = _viagens.value
        set(valor) { _viagens.value = valor }

    val criadas = mutableListOf<Viagem>()
    var falharAoCriar = false

    var sincronizou = false
        private set

    override fun sincronizar() { sincronizou = true }

    override fun observarTodas(): StateFlow<List<Viagem>> = _viagens.asStateFlow()

    override suspend fun criar(viagem: Viagem): String {
        if (falharAoCriar) throw RuntimeException("falha simulada")
        val id = "viagem-gerada-${criadas.size + 1}"
        val comId = viagem.copy(id = id)
        criadas += comId
        viagens = viagens + comId
        return id
    }

    override suspend fun obterTodas(): List<Viagem> = viagens

    override suspend fun obterPorId(id: String): Viagem? = viagens.find { it.id == id }

    override suspend fun inativar(id: String) {
        viagens = viagens.map { if (it.id == id) it.copy(ativo = false) else it }
    }
}