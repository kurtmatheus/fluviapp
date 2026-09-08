package dev.matheus.fluviapp.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "login")

/**
 * Projeção de sessão: o que a UI precisa a cada decisão de tela, gravado no login (ADR-0015 §8.2).
 * A **fonte** continua sendo `users/{uid}` + `funcionarios/{id}` — isto aqui é derivado e morre no
 * logout.
 *
 * ### Três chaves saíram em 2026-09-07 ([ADR-0032] D2)
 *
 * Eram `logado`, `usuario_atual` e `cargo_funcionario_atual`, e as três existiam para alimentar a leitura
 * crua que o menu fazia deste DataStore. Com `SessaoUsuario.observar()`, o menu passou a assinar o
 * **contexto**, e elas perderam a função — cada uma por uma razão própria, que vale distinguir:
 *
 * - **`logado`** nunca teve leitor. Era resíduo de uma decisão do ADR-0005 (*"o DataStore guarda o estado
 *   derivado para roteamento no Splash"*) que a execução superou por um caminho melhor: quem decide se há
 *   sessão é `currentUser` + `SessaoUsuario`, ou seja, a autoridade em vez de uma cópia;
 * - **`usuario_atual`** guardava o nome exibido, **congelado no login**. `ContextoUsuario.nomeExibicao`
 *   resolve melhor, porque lê o funcionário de agora;
 * - **`cargo_funcionario_atual`** guardava o cargo, que é do funcionário e vem com ele.
 *
 * Sobra o `papel_atual`, que **não** é duplicata: ele é do `Usuario`, e é o único campo do perfil de
 * sistema que a projeção precisa guardar para montar o contexto antes de qualquer leitura remota.
 */
object PreferencesKey {
    val PAPEL_ATUAL = stringPreferencesKey("papel_atual")
    val TEMA_ESCURO = booleanPreferencesKey("tema_escuro")

    /**
     * O **usuário logado**, que até 2026-09-07 era uma linha do Room marcada com
     * `ultimoUsuarioLogado = true` (ADR-0017 D4/F6). São os campos que a [SessaoUsuario] precisa para
     * montar o contexto — o `papel` reaproveita [PAPEL_ATUAL], que já existia e guardava o mesmo valor.
     *
     * Elas morrem no logout, junto com as de sessão: um cache de quem entrou não pode sobreviver a quem
     * saiu. A tabela sobrevivia, e essa era a diferença silenciosa entre as duas projeções do login.
     */
    val USUARIO_ID = stringPreferencesKey("usuario_id")
    val USUARIO_USERNAME = stringPreferencesKey("usuario_username")
    val USUARIO_FUNCIONARIO_ID = stringPreferencesKey("usuario_funcionario_id")

    /**
     * O e-mail de quem entrou por último — **e este fica**. É o que o campo do login já vem preenchido,
     * e a conveniência é justamente para *depois* de sair; apagá-la no logout seria apagar a única razão
     * de ela existir.
     */
    val ULTIMO_EMAIL = stringPreferencesKey("ultimo_email")
}