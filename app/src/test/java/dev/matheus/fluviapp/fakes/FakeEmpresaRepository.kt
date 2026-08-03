package dev.matheus.fluviapp.fakes

import dev.matheus.fluviapp.domain.viagem.AtuacaoDaEmpresa
import dev.matheus.fluviapp.domain.viagem.Empresa
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Fake da porta [EmpresaRepository] para testes de ViewModel (sem Firestore/Room). */
class FakeEmpresaRepository : EmpresaRepository {
    private val _empresas = MutableStateFlow<List<Empresa>>(emptyList())

    /** Continua sendo `var` para os testes montarem o cenário numa linha; espelha no fluxo reativo. */
    var empresas: List<Empresa>
        get() = _empresas.value
        set(valor) { _empresas.value = valor }

    val salvos = mutableListOf<Empresa>()
    val deletados = mutableListOf<String>()
    var falharAoSalvar = false

    /** Quem observa tem de ligar o listener: sem isto o StateFlow do repositório real fica vazio. */
    var sincronizou = false
        private set

    override fun sincronizar() { sincronizou = true }
    override fun observarTodas(): StateFlow<List<Empresa>> = _empresas.asStateFlow()
    override suspend fun salvar(empresa: Empresa): String {
        if (falharAoSalvar) throw RuntimeException("falha simulada")
        salvos += empresa
        return empresa.id.ifBlank { "id-gerado-${salvos.size}" }
    }

    val atuacoesPorEmpresa = mutableMapOf<String, List<AtuacaoDaEmpresa>>()

    override suspend fun obterAtuacoes(empresaId: String): List<AtuacaoDaEmpresa> =
        atuacoesPorEmpresa[empresaId].orEmpty()

    override suspend fun salvarAtuacoes(empresaId: String, atuacoes: List<AtuacaoDaEmpresa>) {
        atuacoesPorEmpresa[empresaId] = atuacoes
    }
    override suspend fun obterTodas(): List<Empresa> = empresas
    override suspend fun obterPorId(id: String): Empresa? = empresas.find { it.id == id }
    override suspend fun deletar(id: String) {
        deletados += id
        empresas = empresas.filterNot { it.id == id }
    }
}
