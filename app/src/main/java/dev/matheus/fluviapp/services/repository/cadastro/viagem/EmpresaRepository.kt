package dev.matheus.fluviapp.services.repository.cadastro.viagem

import dev.matheus.fluviapp.domain.viagem.Empresa
import kotlinx.coroutines.flow.StateFlow

/**
 * Porta do repositório de empresas (DIP) — os ViewModels dependem desta interface, não da impl
 * Firestore. Testes usam um fake; produção usa [EmpresaFirestoreRepository].
 *
 * [observarTodas] é a **fonte reativa** que o ADR-0017 D1 põe no lugar do `Flow` do DAO. [obterTodas]
 * continua para quem lê uma vez só, e espera o primeiro snapshot — não devolve vazio quando o dado
 * apenas ainda não chegou.
 *
 * `obterPorNome` **saiu**: não tinha chamador de produção desde que o
 * `PassagemDadosPassagemMapper` passou a resolver por id (ADR-0008) — casar por nome era justamente o
 * que estourava quando a empresa era renomeada.
 */
interface EmpresaRepository {
    fun sincronizar()
    fun observarTodas(): StateFlow<List<Empresa>>
    suspend fun salvar(empresa: Empresa)
    suspend fun obterTodas(): List<Empresa>
    suspend fun obterPorId(id: String): Empresa?
    suspend fun deletar(id: String)
}
