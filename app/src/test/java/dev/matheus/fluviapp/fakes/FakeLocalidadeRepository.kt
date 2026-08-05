package dev.matheus.fluviapp.fakes

import dev.matheus.fluviapp.domain.localidade.Localidade
import dev.matheus.fluviapp.services.repository.cadastro.localidade.LocalidadeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake da porta [LocalidadeRepository].
 *
 * O `deletar` **inativa**, como a impl real — se ele removesse da lista, o teste da busca passaria por
 * um motivo diferente do que acontece em produção, e a diferença entre "sumiu porque foi apagada" e
 * "sumiu porque quem lista filtra `ativo`" ficaria sem cobertura nenhuma.
 */
class FakeLocalidadeRepository : LocalidadeRepository {
    private val _localidades = MutableStateFlow<List<Localidade>>(emptyList())

    var localidades: List<Localidade>
        get() = _localidades.value
        set(valor) { _localidades.value = valor }

    val salvos = mutableListOf<Localidade>()
    var falharAoSalvar = false

    var sincronizou = false
        private set

    override fun sincronizar() { sincronizou = true }

    override fun observarTodas(): StateFlow<List<Localidade>> = _localidades.asStateFlow()

    override suspend fun salvar(localidade: Localidade): String {
        if (falharAoSalvar) throw RuntimeException("falha simulada")
        salvos += localidade
        val id = localidade.id.ifBlank { "id-gerado-${salvos.size}" }
        val comId = localidade.copy(id = id)
        localidades = localidades.filterNot { it.id == id } + comId
        return id
    }

    override suspend fun obterTodas(): List<Localidade> = localidades

    override suspend fun obterPorId(id: String): Localidade? = localidades.find { it.id == id }

    override suspend fun deletar(id: String) {
        val localidade = obterPorId(id) ?: return
        localidades = localidades.map { if (it.id == id) localidade.copy(ativo = false) else it }
    }
}