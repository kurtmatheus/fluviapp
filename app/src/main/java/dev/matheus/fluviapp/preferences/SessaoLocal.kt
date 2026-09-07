package dev.matheus.fluviapp.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import dev.matheus.fluviapp.domain.operacoes.Usuario
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * **Quem entrou, guardado no aparelho** — o que era a tabela `Usuario` do Room (ADR-0017 D4, F6).
 *
 * ### Por que sai do Room
 *
 * A tabela guardava *uma* linha e usava uma coluna booleana — `ultimoUsuarioLogado` — para dizer qual das
 * linhas valia. Isso é forma de banco relacional resolvendo um problema que o banco criou: com um slot
 * só, **o último é o único**, e a coluna some sem substituto porque não tinha função. O ADR-0017 já havia
 * rejeitado manter o SQLite por dado assim — *"paga o custo inteiro de schema por dado que o DataStore
 * guarda melhor"*.
 *
 * ### O que ele não é
 *
 * Não é fonte da verdade. A fonte continua sendo `users/{uid}` + `funcionarios/{id}`, lida do servidor no
 * login (`perfilAutenticado()`); isto aqui é **projeção**, no mesmo espírito das chaves de sessão do
 * ADR-0015 §8.2. E não é credencial: quem decide o que a pessoa pode é a política, sobre o papel e o
 * cargo, e o Firestore recusa do lado dele.
 *
 * ### O defeito que a mudança corrige de graça
 *
 * A linha do Room **sobrevivia ao logout** — o `deslogar()` limpava as chaves do DataStore e não tocava a
 * tabela, então `SessaoUsuario.atual()` continuava devolvendo um contexto para quem já tinha saído. Só a
 * navegação impedia que isso aparecesse. Aqui as duas metades da projeção moram juntas e são limpas pelo
 * mesmo gesto.
 */
@Singleton
class SessaoLocal @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Grava quem acabou de entrar. Substitui o `registrarLogin` do repositório e o `dao.salvar`. */
    suspend fun registrarLogin(usuario: Usuario) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKey.USUARIO_ID] = usuario.id
            preferences[PreferencesKey.USUARIO_USERNAME] = usuario.username
            preferences[PreferencesKey.PAPEL_ATUAL] = usuario.papel
            preferences[PreferencesKey.USUARIO_FUNCIONARIO_ID] = usuario.funcionarioId
            preferences[PreferencesKey.ULTIMO_EMAIL] = usuario.email
        }
    }

    /**
     * O logado, ou `null` quando ninguém entrou nesta instalação — ou quando saiu.
     *
     * O `id` é o que decide: sem ele não há usuário, e devolver um [Usuario] de campos vazios seria
     * afirmar que alguém está logado. O `email` vem do [PreferencesKey.ULTIMO_EMAIL] porque é o mesmo
     * e-mail, e guardá-lo duas vezes criaria duas respostas para uma pergunta.
     */
    suspend fun logado(): Usuario? = observarLogado().first()

    /**
     * **O logado como estado observável** ([ADR-0032] D2) — quem entrou, quem saiu, e a troca no meio.
     *
     * O DataStore já é um `Flow`; o que faltava era a projeção acompanhar. É esta função que permite ao
     * menu largar a leitura crua das chaves sem perder reatividade — antes ele assinava `dataStore.data`
     * direto **porque a porta só sabia responder uma vez**, e a leitura crua não era desleixo, era a
     * única saída.
     */
    fun observarLogado(): Flow<Usuario?> = context.dataStore.data.map { preferences ->
        val id = preferences[PreferencesKey.USUARIO_ID]?.takeIf { it.isNotBlank() } ?: return@map null

        Usuario(
            id = id,
            email = preferences[PreferencesKey.ULTIMO_EMAIL].orEmpty(),
            username = preferences[PreferencesKey.USUARIO_USERNAME].orEmpty(),
            papel = preferences[PreferencesKey.PAPEL_ATUAL].orEmpty(),
            funcionarioId = preferences[PreferencesKey.USUARIO_FUNCIONARIO_ID].orEmpty(),
        )
    }

    /** O e-mail de quem entrou por último, para o campo do login já vir preenchido. Sobrevive ao logout. */
    suspend fun ultimoEmail(): String? =
        context.dataStore.data.first()[PreferencesKey.ULTIMO_EMAIL]?.takeIf { it.isNotBlank() }

    /** Esquece quem entrou — **menos o e-mail**, que é conveniência do próximo login, não sessão. */
    suspend fun limpar() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKey.USUARIO_ID)
            preferences.remove(PreferencesKey.USUARIO_USERNAME)
            preferences.remove(PreferencesKey.USUARIO_FUNCIONARIO_ID)
            preferences.remove(PreferencesKey.PAPEL_ATUAL)
        }
    }
}
