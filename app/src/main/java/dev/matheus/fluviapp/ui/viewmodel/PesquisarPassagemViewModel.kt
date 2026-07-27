package dev.matheus.fluviapp.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matheus.fluviapp.extensions.filtrarPor
import dev.matheus.fluviapp.model.mappers.PassagemDadosPassagemMapper
import dev.matheus.fluviapp.model.operacoes.ContextoUsuario
import dev.matheus.fluviapp.model.operacoes.PermissoesUsuario
import dev.matheus.fluviapp.services.repository.cadastro.ConstanteRepository
import dev.matheus.fluviapp.services.repository.firebase.PassagemFirestoreRepository
import dev.matheus.fluviapp.services.repository.firebase.documents.PassagemDocumento
import dev.matheus.fluviapp.services.repository.firebase.documents.toPassagem
import dev.matheus.fluviapp.services.repository.operacoes.FuncionarioRepository
import dev.matheus.fluviapp.services.repository.operacoes.SessaoUsuario
import dev.matheus.fluviapp.ui.states.passagem.PesquisarPassagemUiState
import dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.FormPesquisarPassagemHelper
import dev.matheus.fluviapp.ui.viewmodel.helpers.passagem.validacao.ValidacaoFormPesquisarPassagemHelper
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await
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
    private val funcionarioRepository: FuncionarioRepository,
    private val sessaoUsuario: SessaoUsuario,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PesquisarPassagemUiState())

    val uiState: StateFlow<PesquisarPassagemUiState>
        get() = _uiState.asStateFlow()

    internal lateinit var formPesquisarPassagemHelper: FormPesquisarPassagemHelper
    lateinit var validacaoFormPesquisarPassagemHelper: ValidacaoFormPesquisarPassagemHelper

    lateinit var onNavegaParaResultadosPesquisa: () -> Unit

    /**
     * Os dois contextos do logado (ADR-0015 §8.1): de onde saem o cargo (2ª entrada da política) e o
     * nome que filtra as passagens do próprio. Sem funcionário (papel puro de plataforma) não há nome de
     * emissor — e quem vê todas as passagens não precisa dele.
     */
    private var contexto: ContextoUsuario? = null

    init {
        viewModelScope.launch {
            inicializarHelpers()
            inicializarPermissaoEspecial()
        }
    }

    private suspend fun inicializarHelpers() {
        contexto = sessaoUsuario.atual()
        formPesquisarPassagemHelper = FormPesquisarPassagemHelper(
            uiState = _uiState,
            constanteRepository = constanteRepository,
            funcionarioRepository = funcionarioRepository,
            agenciaDoEscopo = agenciaDoEscopoDoContexto(),
        )
        validacaoFormPesquisarPassagemHelper = ValidacaoFormPesquisarPassagemHelper(
            uiState = _uiState
        )
    }

    /** "" = sem recorte (plataforma). Sem vínculo, também "": lá a listagem já nem consulta (§4.1). */
    private fun agenciaDoEscopoDoContexto(): String {
        val contexto = contexto ?: return ""
        val escopo = PermissoesUsuario.escopoDeAgencia(contexto.papel, contexto.agencia)
        return (escopo as? PermissoesUsuario.EscopoAgencia.Apenas)?.agencia.orEmpty()
    }

    private fun inicializarPermissaoEspecial() {
        val contexto = contexto ?: return
        if (PermissoesUsuario.podeVerTodasPassagens(contexto.papel, contexto.cargo)) {
            formPesquisarPassagemHelper.atualizaPermissaoEspecial()
        }
    }

    suspend fun carregarDadosPesquisados() {
        contexto?.let { contexto ->
            val pesquisarPassagemUiState = _uiState.value

            // Quem vê todas escolhe o operador no filtro; os demais veem as próprias — e "as próprias"
            // se identifica pelo NOME DO FUNCIONÁRIO, que é o que a emissão congela (ADR-0015 §8.1).
            val usuarioValidado =
                if (PermissoesUsuario.podeVerTodasPassagens(contexto.papel, contexto.cargo)) {
                    pesquisarPassagemUiState.operador
                } else {
                    contexto.funcionario?.descricaoNome.orEmpty()
                }

            // Escopo por agência (ADR-0015 §4.1): "todas as passagens" do SUPERVISOR passa a significar
            // "todas da MINHA agência" — plataforma atravessa, e quem não é plataforma nem tem vínculo
            // não vê listagem nenhuma (fail-closed, sem ir à rede).
            val agenciaDoEscopo = when (
                val escopo = PermissoesUsuario.escopoDeAgencia(contexto.papel, contexto.agencia)
            ) {
                is PermissoesUsuario.EscopoAgencia.Todas -> ""
                is PermissoesUsuario.EscopoAgencia.Apenas -> escopo.agencia
                is PermissoesUsuario.EscopoAgencia.Nenhuma -> {
                    _uiState.update { it.copy(listaResultadoPassagens = emptyList()) }
                    formPesquisarPassagemHelper.atualizarProcessamento()
                    onNavegaParaResultadosPesquisa()
                    return
                }
            }

            // try só na chamada de rede (equivalente ao antigo addOnFailureListener). Erros de
            // mapeamento NÃO entram aqui — surgem com o próprio stack, não mascarados de "Falha no Processo".
            val snapshot = try {
                passagemRepository.obterTodasPorDataStatus(
                    data = pesquisarPassagemUiState.data,
                    status = pesquisarPassagemUiState.situacao,
                    nomeFuncionario = usuarioValidado,
                    agencia = agenciaDoEscopo,
                ).await()
            } catch (e: Exception) {
                Log.e(TAG, "obterTodasPorDataStatus: Exception: ${e.message}")
                throw RuntimeException("Falha no Processo: ${e.message}")
            }

            val passagens = snapshot.documents.mapNotNull { document ->
                document.toObject<PassagemDocumento>()?.toPassagem(document.id)
            }.sortedBy { it.numero.toIntOrNull() ?: 0 }

            var listaPassagemFiltered = filtrarPor(pesquisarPassagemUiState.filtrarTodos, passagens) { true }
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
        }
    }

    fun showSearchBar() {
        formPesquisarPassagemHelper.showBarraPesquisa()
    }

    companion object {
        private const val TAG = "pesquisarPassagemViewModel"
    }
}
