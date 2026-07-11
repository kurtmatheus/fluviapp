package dev.matheus.fluviapp.ui.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matheus.fluviapp.preferences.PreferencesKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fonte única do tema (claro/escuro), projetada do DataStore. Escopado à Activity para que a
 * MainActivity (que dirige o [dev.matheus.fluviapp.ui.theme.FluviAppTheme]) e o switch no menu
 * principal compartilhem a mesma instância — o DataStore é a autoridade e ambos o observam.
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ViewModel() {

    // null = usuário ainda não escolheu → cai no tema do sistema.
    val temaEscuro: StateFlow<Boolean?> = dataStore.data
        .map { it[PreferencesKey.TEMA_ESCURO] }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun alternarTema(estaEscuroAtual: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[PreferencesKey.TEMA_ESCURO] = !estaEscuroAtual }
        }
    }
}