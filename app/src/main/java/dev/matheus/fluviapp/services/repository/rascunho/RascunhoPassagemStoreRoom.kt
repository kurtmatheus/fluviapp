package dev.matheus.fluviapp.services.repository.rascunho

import com.google.gson.Gson
import dev.matheus.fluviapp.database.RascunhoPassagemEntity
import dev.matheus.fluviapp.database.dao.passagem.RascunhoPassagemDao
import dev.matheus.fluviapp.model.rascunho.RascunhoPassagemSnapshot
import javax.inject.Inject

/**
 * Impl Room-JSON da [RascunhoStore] (ADR-0004). Serializa o snapshot com Gson e guarda o blob no
 * slot único da tabela `rascunho_passagem`. Único ponto que conhece o mecanismo de persistência.
 */
class RascunhoPassagemStoreRoom @Inject constructor(
    private val dao: RascunhoPassagemDao,
    private val gson: Gson,
) : RascunhoStore {

    override suspend fun salvar(snapshot: RascunhoPassagemSnapshot) {
        dao.salvar(RascunhoPassagemEntity(json = gson.toJson(snapshot)))
    }

    override suspend fun recuperar(): RascunhoPassagemSnapshot? {
        return dao.recuperar()?.json?.let { gson.fromJson(it, RascunhoPassagemSnapshot::class.java) }
    }

    override suspend fun remover() {
        dao.remover()
    }
}