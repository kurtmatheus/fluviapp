package dev.matheus.fluviapp.fakes

import dev.matheus.fluviapp.domain.porto.Porto
import dev.matheus.fluviapp.services.repository.cadastro.porto.PortoRepository
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake da porta [PortoRepository].
 *
 * O `deletar` **inativa**, como a impl real — se ele removesse da lista, o teste da busca passaria por um
 * motivo diferente do que acontece em produção, e a diferença entre "sumiu porque foi apagado" e "sumiu
 * porque quem lista filtra `ativo`" ficaria sem cobertura nenhuma.
 */
class FakePortoRepository : PortoRepository {
    private val _portos = MutableStateFlow<List<Porto>>(emptyList())

    var portos: List<Porto>
        get() = _portos.value
        set(valor) { _portos.value = valor }

    val salvos = mutableListOf<Porto>()
    var falharAoSalvar = false

    /**
     * Simula o que a coleção faz quando o **primeiro snapshot não chega** — listener negado pela regra,
     * rede fora: `obterTodos` fica suspenso, e não devolve lista vazia (`ColecaoFirestore` espera de
     * propósito, para não confundir *vazio* com *ainda não chegou*).
     *
     * Existe porque isso aconteceu de verdade: as regras de `portos` ainda não estavam publicadas, e a
     * espera por elas engolia a carga das **localidades** no formulário.
     */
    var travarObterTodos = false

    var sincronizou = false
        private set

    override fun sincronizar() { sincronizou = true }

    override fun observarTodos(): StateFlow<List<Porto>> = _portos.asStateFlow()

    override suspend fun salvar(porto: Porto): String {
        if (falharAoSalvar) throw RuntimeException("falha simulada")
        salvos += porto
        val id = porto.id.ifBlank { "id-gerado-${salvos.size}" }
        val comId = porto.copy(id = id)
        portos = portos.filterNot { it.id == id } + comId
        return id
    }

    override suspend fun obterTodos(): List<Porto> {
        if (travarObterTodos) awaitCancellation()
        return portos
    }

    override suspend fun obterPorId(id: String): Porto? = portos.find { it.id == id }

    override suspend fun deletar(id: String) {
        val porto = obterPorId(id) ?: return
        portos = portos.map { if (it.id == id) porto.copy(ativo = false) else it }
    }
}