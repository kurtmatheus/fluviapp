package dev.matheus.fluviapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dev.matheus.fluviapp.ui.states.SplashScreenState
import dev.matheus.fluviapp.ui.states.SplashScreenUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class SplashScreenViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashScreenUiState())
    val uiState: StateFlow<SplashScreenUiState>
        get() = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            setInitialDestination()
        }
    }

    private suspend fun setInitialDestination() {
        delay(Random.nextLong(300, 1000))
        // Autoridade da sessão = sessão persistida do Firebase (offline-capaz, ADR-0005).
        val splashScreenState = if (firebaseAuth.currentUser != null) {
            SplashScreenState.Logado
        } else {
            SplashScreenState.Deslogado
        }
        _uiState.value = _uiState.value.copy(splashScreenState = splashScreenState)
    }
}