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
 * As chaves de papel e cargo são **novas** (`papel_atual`/`cargo_funcionario_atual`) de propósito: a
 * antiga `cargo_atual` guardava o cargo do vocabulário velho, e reaproveitá-la faria uma sessão
 * pré-divisão ser lida como cargo de negócio. Chave nova = valor ausente = sem permissão (fail-closed),
 * que é o que se quer de uma sessão obsoleta.
 */
object PreferencesKey {
    val LOGADO = booleanPreferencesKey("logado")
    val USUARIO_ATUAL = stringPreferencesKey("usuario_atual")
    val PAPEL_ATUAL = stringPreferencesKey("papel_atual")
    val CARGO_ATUAL = stringPreferencesKey("cargo_funcionario_atual")
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

    /**
     * Em nome de qual empresa se está operando (F6.4) — a resposta à seleção de contexto do ADR-0016 §6.
     * **Preferência, não credencial**: ver `EscolhaDeVinculo`.
     */
    val EMPRESA_ATIVA = stringPreferencesKey("empresa_ativa")
}