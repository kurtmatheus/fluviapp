package dev.matheus.fluviapp.fakes

import dev.matheus.fluviapp.domain.operacoes.Funcionario
import dev.matheus.fluviapp.services.repository.operacoes.FuncionarioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Fake da porta [FuncionarioRepository] para testes de ViewModel (sem Firestore). */
class FakeFuncionarioRepository : FuncionarioRepository {
    private val _funcionarios = MutableStateFlow<List<Funcionario>>(emptyList())

    var funcionarios: List<Funcionario>
        get() = _funcionarios.value
        set(valor) { _funcionarios.value = valor }

    val salvos = mutableListOf<Funcionario>()
    val deletados = mutableListOf<String>()

    var sincronizou = false
        private set

    override fun sincronizar() { sincronizou = true }

    override fun observarTodos(): StateFlow<List<Funcionario>> = _funcionarios.asStateFlow()

    override suspend fun salvar(funcionario: Funcionario): String {
        salvos += funcionario
        val id = funcionario.id.ifBlank { "id-gerado-${salvos.size}" }
        funcionarios = funcionarios.filterNot { it.id == id } + funcionario.copy(id = id)
        return id
    }

    override suspend fun obterPorId(id: String): Funcionario? = funcionarios.find { it.id == id }
    override suspend fun obterTodosFuncionarios(): List<Funcionario> = funcionarios

    /** No fake não há "servidor": a mesma lista responde — o que importa é o casamento por e-mail. */
    override suspend fun obterPorEmailDoServidor(email: String): Funcionario? =
        funcionarios.find { it.email.equals(email.trim(), ignoreCase = true) }

    override suspend fun deletar(id: String) {
        deletados += id
        funcionarios = funcionarios.filterNot { it.id == id }
    }
}
