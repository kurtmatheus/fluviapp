package dev.matheus.fluviapp.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "login")

object PreferencesKey {
    val LOGADO = booleanPreferencesKey("logado")
    val USUARIO_ATUAL = stringPreferencesKey("usuario_atual")
    val CARGO_ATUAL = stringPreferencesKey("cargo_atual")
    val TEMA_ESCURO = booleanPreferencesKey("tema_escuro")
}