package dev.matheus.fluviapp.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matheus.fluviapp.extensions.filtrarPor
import dev.matheus.fluviapp.model.mappers.PassagemDadosPassagemMapper
import dev.matheus.fluviapp.model.operacoes.Usuario
import dev.matheus.fluviapp.model.operacoes.temPermissaoEspecialPassagem
import dev.matheus.fluviapp.model.passagem.Passagem
import dev.matheus.fluviapp.services.repository.cadastro.ConstanteRepository
import dev.matheus.fluviapp.services.repository.firebase.PassagemFirestoreRepository
import dev.matheus.fluviapp.services.repository.firebase.documents.PassagemDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.toPassagem
import dev.matheus.fluviapp.services.repository.operacoes.UsuarioRepository
import dev.matheus.fluviapp.ui.states.passagem.PesquisarPassagemUiState
import dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.FormPesquisarPassagemHelper
import dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.validacao.ValidacaoFormPesquisarPassagemHelper
import com.google.firebase.firestore.toObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PesquisarPassagemViewModel @Inject constructor(
    private val constanteRepository: ConstanteRepository,
    private val passagemRepository: PassagemFirestoreRepository,
    private val dadosPassagemMapper: PassagemDadosPassagemMapper,
    private val usuarioRepository: UsuarioRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PesquisarPassagemUiState())

    val uiState: StateFlow<PesquisarPassagemUiState>
        get() = _uiState.asStateFlow()

    internal lateinit var formPesquisarPassagemHelper: FormPesquisarPassagemHelper
    lateinit var validacaoFormPesquisarPassagemHelper: ValidacaoFormPesquisarPassagemHelper

    lateinit var onNavegaParaResultadosPesquisa: () -> Unit

    lateinit var usuarioLogado: Usuario

    init {
        viewModelScope.launch {
            inicializarHelpers()
            inicializarPermissaoEspecial()
        }
    }

    private suspend fun inicializarHelpers() {
        formPesquisarPassagemHelper = FormPesquisarPassagemHelper(
            uiState = _uiState,
            constanteRepository = constanteRepository,
            usuarioRepository = usuarioRepository
        )
        validacaoFormPesquisarPassagemHelper = ValidacaoFormPesquisarPassagemHelper(
            uiState = _uiState
        )
        usuarioLogado = usuarioRepository.obterUltimoUsuarioLogado()!!
    }

    private fun inicializarPermissaoEspecial() {
        if (usuarioLogado.temPermissaoEspecialPassagem()) {
            formPesquisarPassagemHelper.atualizaPermissaoEspecial()
        }
    }

    suspend fun carregarDadosPesquisados() {
        usuarioRepository.obterUltimoUsuarioLogado()?.let { usuarioLogado ->
            val pesquisarPassagemUiState = _uiState.value

            var listaPassagemFiltered: List<Passagem>

            val usuarioValidado = if (usuarioLogado.temPermissaoEspecialPassagem()) pesquisarPassagemUiState.operador else usuarioLogado.nome

            passagemRepository.obterTodasPorDataStatus(
                data = pesquisarPassagemUiState.data,
                status = pesquisarPassagemUiState.situacao,
                nomeFuncionario = usuarioValidado
            ).addOnSuccessListener { snapshot ->
                val passagens = snapshot.documents.mapNotNull { document ->
                    document.toObject<PassagemDocumento>()?.toPassagem(document.id)
                }.sortedBy { it.numero.toInt() }

                listaPassagemFiltered = filtrarPor(pesquisarPassagemUiState.filtrarTodos, passagens) { true }
                listaPassagemFiltered = filtrarPor(pesquisarPassagemUiState.filtrarVeiculos, listaPassagemFiltered) { it.ehVeiculo }
                listaPassagemFiltered = filtrarPor(pesquisarPassagemUiState.filtrarPassageiros, listaPassagemFiltered) { !it.ehVeiculo }

                val listDadosPassagemCard = listaPassagemFiltered.map { passagem ->
                    dadosPassagemMapper.map(passagem)
                }

                _uiState.update {
                    it.copy(
                        listaResultadoPassagens = listDadosPassagemCard
                    )
                }

                formPesquisarPassagemHelper.atualizarProcessamento()

                onNavegaParaResultadosPesquisa()
            }.addOnFailureListener {
                Log.e(TAG, "obterTodasPorDataStatus: Exception: ${it.message}")
                throw RuntimeException("Falha no Processo: ${it.message}")
            }
        }
    }

    fun showSearchBar() {
        formPesquisarPassagemHelper.showBarraPesquisa()
    }

    companion object {
        private const val TAG = "pesquisarPassagemViewModel"
    }
}
