package dev.matheus.fluviapp.services.repository.operacoes

import dev.matheus.fluviapp.database.dao.operacoes.UsuarioDao
import dev.matheus.fluviapp.domain.operacoes.Usuario
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Quem está logado, **localmente**. Autenticação vive na porta
 * [dev.matheus.fluviapp.services.repository.firebase.autenticacao.AutenticacaoRepository], e é de lá que
 * vem o perfil autoritativo.
 *
 * ### O que saiu, e por que era uma bomba-relógio
 *
 * Este repositório espelhava a coleção `users` inteira com um `addSnapshotListener` disparado **antes do
 * login**, e o login então procurava o autenticado nesse espelho (`obterPorEmail`). Dois problemas, um
 * deles fatal:
 *
 *  - a regra do servidor é `allow read: if autenticado()` — uma leitura pré-login é **negada**. Enquanto
 *    o `firestore.rules` do repo não foi deployado, isso passou; no dia do deploy, o espelho ficaria
 *    vazio e **o login pararia de funcionar**, com "Falha na Autenticação" e nenhuma pista do porquê;
 *  - o app baixava o perfil de *todo mundo* para conferir o de *uma* pessoa.
 *
 * Agora é o contrário: o perfil vem do servidor já autenticado (`perfilAutenticado()`), e o Room recebe
 * **só o usuário logado**, por [registrarLogin]. O espelho deixa de ser condição para entrar e volta a
 * ser o que devia: cache de quem já entrou, que é o que a [SessaoUsuario] lê depois.
 */
@Singleton
class UsuarioRepository @Inject constructor(
    private val dao: UsuarioDao,
) {
    suspend fun salvar(usuario: Usuario) = dao.salvar(usuario)

    /**
     * Grava o usuário que acabou de entrar e o marca como o último — a origem é o perfil lido do
     * servidor, não uma busca no espelho. Devolve o que ficou gravado.
     */
    suspend fun registrarLogin(usuario: Usuario): Usuario {
        dao.limparUltimoUsuarioLogado()
        val logado = usuario.copy(ultimoUsuarioLogado = true)
        dao.salvar(logado)
        return logado
    }

    suspend fun obterUltimoUsuarioLogado() = dao.obterUltimoUsuarioLogado().first()

    companion object {
        const val COLLECTION_USERS = "users"
    }
}