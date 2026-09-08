package dev.matheus.fluviapp.fakes

import dev.matheus.fluviapp.domain.operacoes.Usuario
import dev.matheus.fluviapp.services.repository.operacoes.UsuarioRepository

/**
 * Fake da porta [UsuarioRepository] — o id é o `uid` do Auth, como na impl.
 *
 * `definirAcesso` **aplica** a escrita à lista, e não só a registra: os gestos da seção Usuários
 * recarregam a lista em seguida, e um fake que não aplicasse deixaria passar um ViewModel que grava e
 * mostra a situação anterior.
 */
class FakeUsuarioRepository : UsuarioRepository {
    var usuarios: List<Usuario> = emptyList()
    val acessosDefinidos = mutableListOf<Triple<String, Boolean, Long?>>()

    /** Quando `true`, a escrita estoura — é como se testa que a falha não passa em silêncio. */
    var falharAoDefinir = false

    override fun sincronizar() = Unit

    override suspend fun obterTodos(): List<Usuario> = usuarios

    override suspend fun definirAcesso(id: String, ativo: Boolean, expiraEm: Long?) {
        if (falharAoDefinir) throw IllegalStateException("falha de rede simulada")

        acessosDefinidos += Triple(id, ativo, expiraEm)
        usuarios = usuarios.map {
            if (it.id == id) it.copy(ativo = ativo, expiraEm = expiraEm) else it
        }
    }
}
