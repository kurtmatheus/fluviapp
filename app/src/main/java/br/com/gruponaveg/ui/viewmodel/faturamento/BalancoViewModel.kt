package br.com.gruponaveg.ui.viewmodel.faturamento

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.gruponaveg.model.mappers.BalancoPassagensMapper
import br.com.gruponaveg.services.repository.firebase.PassagemFirestoreRepository
import br.com.gruponaveg.services.repository.firebase.documents.PassagemDocumento
import br.com.gruponaveg.services.repository.firebase.documents.toPassagem
import br.com.gruponaveg.ui.states.faturamento.BalancoState
import br.com.gruponaveg.ui.viewmodel.helpers.faturamento.BalancoHelper
import com.google.firebase.firestore.toObject
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
        passagemRepository.obterTodasPorData(data)
            .addOnSuccessListener { snapshot ->
                val listaPassagemNova = snapshot.documents.mapNotNull { document ->
                    document.toObject<PassagemDocumento>()?.toPassagem(document.id)
                }
                helper.atualizarDadosBalanco(balancoPassagensMapper.map(listaPassagemNova))
                helper.atualizarProcessamento()
            }
    }

}