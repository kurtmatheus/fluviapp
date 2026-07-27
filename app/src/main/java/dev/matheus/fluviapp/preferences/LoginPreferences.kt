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
}