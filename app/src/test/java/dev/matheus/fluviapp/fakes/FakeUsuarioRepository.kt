package dev.matheus.fluviapp.fakes

import dev.matheus.fluviapp.domain.operacoes.Usuario
import dev.matheus.fluviapp.services.repository.operacoes.UsuarioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake da porta [UsuarioRepository] — o id é o `uid` do Auth, como na impl.
 *
 * A lista é **observável**, como na impl: as métricas de acesso vivem no Início do painel e têm de mudar
 * sozinhas quando alguém entra. Um fake que só respondesse uma vez deixaria passar exatamente o defeito de
 * 2026-08-17 — a tela ficando com a fotografia do instante em que nasceu.
 *
 * `definirAcesso` **aplica** a escrita: os gestos da seção Usuários recarregam a lista em seguida, e um
 * fake que não aplicasse aprovaria um ViewModel que grava e mostra a situação anterior.
 */
class FakeUsuarioRepository : UsuarioRepository {
    private val _usuarios = MutableStateFlow<List<Usuario>>(emptyList())

    var usuarios: List<Usuario>
        get() = _usuarios.value
        set(valor) { _usuarios.value = valor }

    val acessosDefinidos = mutableListOf<Triple<String, Boolean, Long?>>()

    /** Quando `true`, a escrita estoura — é como se testa que a falha não passa em silêncio. */
    var falharAoDefinir = false

    var sincronizou = false
        private set

    /**
     * Quantas vezes alguém pediu a lista — e existe para medir **quem não pediu**: o Início do painel só
     * liga estes listeners para o `ADM`, porque a leitura ampla de convites é dele ([ADR-0032] D6).
     */
    var leituras = 0
        private set

    override fun sincronizar() { sincronizou = true }

    override fun observarTodos(): StateFlow<List<Usuario>> = _usuarios.asStateFlow()

    override suspend fun obterTodos(): List<Usuario> {
        leituras++
        return usuarios
    }

    override suspend fun definirAcesso(id: String, ativo: Boolean, expiraEm: Long?) {
        if (falharAoDefinir) throw IllegalStateException("falha de rede simulada")

        acessosDefinidos += Triple(id, ativo, expiraEm)
        usuarios = usuarios.map {
            if (it.id == id) it.copy(ativo = ativo, expiraEm = expiraEm) else it
        }
    }
}
