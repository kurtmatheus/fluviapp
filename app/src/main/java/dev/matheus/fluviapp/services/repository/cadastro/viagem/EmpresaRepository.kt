package dev.matheus.fluviapp.services.repository.cadastro.viagem

import dev.matheus.fluviapp.model.viagem.Empresa

/**
 * Porta do repositório de empresas (DIP) — os ViewModels dependem desta interface, não da impl
 * Firestore. Testes usam um fake; produção usa [EmpresaFirestoreRepository].
 */
interface EmpresaRepository {
    fun sincronizar()
    suspend fun salvar(empresa: Empresa)
    suspend fun obterTodas(): List<Empresa>
    suspend fun obterPorId(id: String): Empresa?
    suspend fun obterPorNome(nome: String): Empresa
    suspend fun deletar(id: String)
}
