package dev.matheus.fluviapp.ui.viewmodel.contagem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matheus.fluviapp.domain.mappers.ContagemPassagensMapper
import dev.matheus.fluviapp.services.repository.firebase.PassagemFirestoreRepository
import dev.matheus.fluviapp.services.repository.firebase.documents.PassagemDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.toPassagem
import dev.matheus.fluviapp.ui.states.contagem.ContagemPassagemUiState
import dev.matheus.fluviapp.ui.viewmodel.helpers.contagem.ContagemPassagemHelper
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContagemPassagemViewModel @Inject constructor(
    private val passagemRepository: PassagemFirestoreRepository,
    private val contagemPassagensMapper: ContagemPassagensMapper
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContagemPassagemUiState())
    val uiState: StateFlow<ContagemPassagemUiState>
        get() = _uiState.asStateFlow()

    lateinit var helper: ContagemPassagemHelper

    init {
        viewModelScope.launch {
            inicializarHelper()
        }
    }

    private fun inicializarHelper() {
        helper = ContagemPassagemHelper(
            uiState = _uiState
        )
    }

    fun atualizarLista(data: String) {
        viewModelScope.launch {
            val snapshot = passagemRepository.obterTodasPorData(data).await()
            val listaPassagemNova = snapshot.documents.mapNotNull { document ->
                document.toObject<PassagemDocumento>()?.toPassagem(document.id)
            }
            helper.atualizarDadosContagem(contagemPassagensMapper.map(listaPassagemNova))
            helper.atualizarProcessamento()
        }
    }

}