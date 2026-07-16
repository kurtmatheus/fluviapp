package dev.matheus.fluviapp.ui.viewmodel.faturamento

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matheus.fluviapp.model.mappers.BalancoPassagensMapper
import dev.matheus.fluviapp.services.repository.firebase.PassagemFirestoreRepository
import dev.matheus.fluviapp.services.repository.firebase.documents.PassagemDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.toPassagem
import dev.matheus.fluviapp.ui.states.faturamento.BalancoState
import dev.matheus.fluviapp.ui.viewmodel.helpers.faturamento.BalancoHelper
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BalancoViewModel @Inject constructor(
    private val passagemRepository: PassagemFirestoreRepository,
    private val balancoPassagensMapper: BalancoPassagensMapper
) : ViewModel() {

    private val _uiState = MutableStateFlow(BalancoState())
    val uiState: StateFlow<BalancoState>
        get() = _uiState.asStateFlow()

    lateinit var helper: BalancoHelper

    init {
        viewModelScope.launch {
            inicializarHelper()
        }
    }

    private fun inicializarHelper() {
        helper = BalancoHelper(
            uiState = _uiState
        )
    }

    fun atualizarLista(data: String) {
        viewModelScope.launch {
            val snapshot = passagemRepository.obterTodasPorData(data).await()
            val listaPassagemNova = snapshot.documents.mapNotNull { document ->
                document.toObject<PassagemDocumento>()?.toPassagem(document.id)
            }
            helper.atualizarDadosBalanco(balancoPassagensMapper.map(listaPassagemNova))
            helper.atualizarProcessamento()
        }
    }

}