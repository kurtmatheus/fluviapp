package dev.matheus.fluviapp.ui.viewmodel.usuario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.matheus.fluviapp.domain.operacoes.Convite
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaRepository
import dev.matheus.fluviapp.services.repository.operacoes.ConviteRepository
import dev.matheus.fluviapp.ui.states.PesquisaUsuarioUiState
import dev.matheus.fluviapp.ui.states.UsuarioResultado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Busca de usuários (F6.6) — **somente leitura**, como o ADR-0021 D2 já previa.
 *
 * O que ela lista são os **convites**: do ponto de vista da plataforma, é o convite que diz quem pode
 * entrar e com que papel. Quem já entrou aparece como *Ativo*; quem ainda não, como *Convidado* — e
 * nenhum dos dois some da lista, porque o registro é o que responde "por que esta pessoa tem este
 * papel?".
 *
 * O que ela **não** faz, e é decisão e não falta: não edita papel nem revoga. Mudar o papel de alguém que
 * já entrou não é reescrever o convite — o papel já está em `users/{uid}`, que a regra torna imutável
 * pelo cliente. Enquanto esse caminho não existir, mostrar um botão que não cumpre seria pior do que
 * não ter botão.
 */
@HiltViewModel
class PesquisaUsuarioViewModel @Inject constructor(
    private val conviteRepository: ConviteRepository,
    private val empresaRepository: EmpresaRepository,
) : ViewModel() {

    private var convites: List<Convite> = emptyList()
    private var empresasPorId: Map<String, String> = emptyMap()

    private val _uiState = MutableStateFlow(PesquisaUsuarioUiState())
    val uiState: StateFlow<PesquisaUsuarioUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            empresasPorId = empresaRepository.obterTodas().associate { it.id to it.nome }
            convites = conviteRepository.obterTodos()
            _uiState.update { it.copy(resultados = filtrar(it.email)) }
        }
    }

    fun onEmailChange(email: String) = _uiState.update {
        it.copy(email = email, resultados = filtrar(email))
    }

    private fun filtrar(email: String): List<UsuarioResultado> = convites
        .filter { email.isBlank() || it.email.startsWith(email.trim(), ignoreCase = true) }
        .map { convite ->
            UsuarioResultado(
                email = convite.email,
                nome = convite.nome,
                papel = convite.papel.name,
                vinculo = convite.vinculo?.let { vinculo ->
                    // Empresa que não resolve mostra o id: a linha existe, e esconder o vínculo
                    // esconderia justamente o dado a corrigir.
                    "${empresasPorId[vinculo.empresaId] ?: vinculo.empresaId} · ${vinculo.cargo.name}"
                }.orEmpty(),
                situacao = if (convite.usado) SITUACAO_ATIVO else SITUACAO_CONVIDADO,
            )
        }

    private companion object {
        const val SITUACAO_ATIVO = "Ativo"
        const val SITUACAO_CONVIDADO = "Convidado"
    }
}