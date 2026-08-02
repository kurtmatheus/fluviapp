package dev.matheus.fluviapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import dev.matheus.fluviapp.ui.states.SplashScreenState
import dev.matheus.fluviapp.ui.states.SplashScreenUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SplashScreenViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashScreenUiState())
    val uiState: StateFlow<SplashScreenUiState>
        get() = _uiState.asStateFlow()

    init {
        setInitialDestination()
    }

    /**
     * Resolve o destino inicial **na velocidade da checagem**, sem espera artificial: a splash existe para
     * cobrir o tempo real de decidir, não para ser vista (E1 do roadmap).
     */
    private fun setInitialDestination() {
        // Autoridade da sessão = sessão persistida do Firebase (offline-capaz, ADR-0005).
        val splashScreenState = if (firebaseAuth.currentUser != null) {
            SplashScreenState.Logado
        } else {
            SplashScreenState.Deslogado
        }
        _uiState.value = _uiState.value.copy(splashScreenState = splashScreenState)
    }
}