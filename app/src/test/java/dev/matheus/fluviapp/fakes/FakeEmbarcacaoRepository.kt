package dev.matheus.fluviapp.fakes

import dev.matheus.fluviapp.domain.viagem.Embarcacao
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmbarcacaoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Fake da porta [EmbarcacaoRepository] para testes de ViewModel (sem Firestore). */
class FakeEmbarcacaoRepository : EmbarcacaoRepository {
    private val _embarcacoes = MutableStateFlow<List<Embarcacao>>(emptyList())

    /** Continua sendo `var` para os testes montarem o cenário numa linha; espelha no fluxo reativo. */
    var embarcacoes: List<Embarcacao>
        get() = _embarcacoes.value
        set(valor) { _embarcacoes.value = valor }

    val salvos = mutableListOf<Embarcacao>()
    val deletados = mutableListOf<String>()
    var falharAoSalvar = false

    /** Quem observa tem de ligar o listener: sem isto o StateFlow do repositório real fica vazio. */
    var sincronizou = false
        private set

    override fun sincronizar() { sincronizou = true }

    override fun observarTodas(): StateFlow<List<Embarcacao>> = _embarcacoes.asStateFlow()

    override suspend fun obterTodos(): List<Embarcacao> = embarcacoes

    override suspend fun obterPorId(id: String): Embarcacao? = embarcacoes.find { it.id == id }

    override suspend fun salvar(embarcacao: Embarcacao): String {
        if (falharAoSalvar) throw RuntimeException("falha simulada")
        salvos += embarcacao
        return embarcacao.id.ifBlank { "id-gerado-${salvos.size}" }
    }

    override suspend fun deletar(id: String) {
        deletados += id
        embarcacoes = embarcacoes.filterNot { it.id == id }
    }
}