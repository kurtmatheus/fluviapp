package dev.matheus.fluviapp.ui.viewmodel.funcionario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.matheus.fluviapp.domain.operacoes.Funcionario
import dev.matheus.fluviapp.domain.operacoes.PermissoesUsuario
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaRepository
import dev.matheus.fluviapp.services.repository.operacoes.FuncionarioRepository
import dev.matheus.fluviapp.services.repository.operacoes.SessaoUsuario
import dev.matheus.fluviapp.ui.states.EmpresaOpcao
import dev.matheus.fluviapp.ui.states.FuncionarioResultado
import dev.matheus.fluviapp.ui.states.PesquisaFuncionarioUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Busca de membros da equipe — mesma tela, **dois recortes** (ADR-0015 §2.2), agora por empresa.
 *
 * O recorte é aplicado ao **universo** (`todos`), e não ao filtro, para que nenhum caminho de UI o
 * contorne: a lista do supervisor contém apenas quem tem vínculo com a empresa dele. Isso não mudou de
 * intenção na F6.3, mudou de coordenada — era `agencia == "MATRIZ"`, virou `empresaIds.contains(id)`.
 *
 * O que **mudou de fato** é o que aparece em cada linha: quem serve a duas empresas mostra as duas, com o
 * cargo de cada uma. O cadastro antigo não tinha como dizer isso.
 */
@HiltViewModel
class PesquisaFuncionarioViewModel @Inject constructor(
    private val funcionarioRepository: FuncionarioRepository,
    private val empresaRepository: EmpresaRepository,
    private val sessaoUsuario: SessaoUsuario,
) : ViewModel() {

    private var todos: List<Funcionario> = emptyList()
    private var empresasPorId: Map<String, String> = emptyMap()

    /** Empresa do logado quando ele não vê todas; vazia para a plataforma. */
    private var empresaDoEscopo: String = ""

    private val _uiState = MutableStateFlow(PesquisaFuncionarioUiState())
    val uiState: StateFlow<PesquisaFuncionarioUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            aplicarRecorte()
            recarregar()
        }
    }

    private suspend fun aplicarRecorte() {
        val contexto = sessaoUsuario.atual()
        val veTodas = PermissoesUsuario.podeVerTodasAgencias(contexto?.papel)
        empresaDoEscopo = if (veTodas) "" else contexto?.vinculoAtivo?.empresaId.orEmpty()
        _uiState.update {
            it.copy(
                podeFiltrarPorEmpresa = veTodas,
                podeDeletar = PermissoesUsuario.podeDeletarFuncionario(contexto?.papel),
            )
        }
    }

    fun onNomeChange(nome: String) = _uiState.update {
        it.copy(nome = nome, resultados = filtrar(nome, it.empresa))
    }

    fun onEmpresaChange(empresa: String) = _uiState.update {
        if (!it.podeFiltrarPorEmpresa) it
        else it.copy(empresa = empresa, resultados = filtrar(it.nome, empresa))
    }

    fun onDeletar(id: String) {
        // Segunda barreira do mesmo recorte: a tela esconde o botão, mas o VM também recusa — o estado
        // pode ter sido montado antes do recorte, e deletar não é reversível.
        if (!_uiState.value.podeDeletar) return
        viewModelScope.launch {
            funcionarioRepository.deletar(id)
            recarregar()
        }
    }

    /** Recarrega o universo (já recortado) e reaplica os filtros correntes (init e pós-deleção). */
    private suspend fun recarregar() {
        val empresas = empresaRepository.obterTodas()
        empresasPorId = empresas.associate { it.id to it.nome }

        val universo = funcionarioRepository.obterTodosFuncionarios()
        todos = if (empresaDoEscopo.isBlank()) {
            universo
        } else {
            universo.filter { empresaDoEscopo in it.empresaIds }
        }

        _uiState.update {
            it.copy(
                empresas = if (it.podeFiltrarPorEmpresa) {
                    empresas.map { empresa -> EmpresaOpcao(empresa.id, empresa.nome) }
                } else {
                    emptyList()
                },
                resultados = filtrar(it.nome, it.empresa),
            )
        }
    }

    // Nome: prefixo. Empresa: seleção exata do dropdown (vazio = sem filtro).
    private fun filtrar(nome: String, empresa: String): List<FuncionarioResultado> {
        val idDaEmpresa = empresasPorId.entries.firstOrNull { it.value == empresa }?.key

        return todos
            .filter { it.descricaoNome.startsWith(nome, ignoreCase = true) }
            .filter { idDaEmpresa == null || idDaEmpresa in it.empresaIds }
            .map { funcionario ->
                FuncionarioResultado(
                    id = funcionario.id,
                    nome = funcionario.descricaoNome,
                    email = funcionario.email,
                    vinculos = funcionario.vinculos.map { vinculo ->
                        // Empresa que não resolve aparece só com o cargo: a linha não some, porque o
                        // vínculo existe — e sumir esconderia justamente o dado a corrigir.
                        listOfNotNull(empresasPorId[vinculo.empresaId], vinculo.cargo.name)
                            .joinToString(" · ")
                    },
                )
            }
    }
}