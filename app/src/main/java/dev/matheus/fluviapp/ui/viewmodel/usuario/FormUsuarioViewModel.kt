package dev.matheus.fluviapp.ui.viewmodel.usuario

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.matheus.fluviapp.domain.operacoes.Convite
import dev.matheus.fluviapp.domain.operacoes.Funcionario
import dev.matheus.fluviapp.domain.operacoes.Usuario
import dev.matheus.fluviapp.services.repository.cadastro.viagem.EmpresaRepository
import dev.matheus.fluviapp.services.repository.operacoes.ConviteRepository
import dev.matheus.fluviapp.services.repository.operacoes.FuncionarioRepository
import dev.matheus.fluviapp.ui.states.EmpresaOpcao
import dev.matheus.fluviapp.ui.states.FormUsuarioUiState
import dev.matheus.fluviapp.ui.viewmodel.helpers.usuario.validarUsuario
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * **Novo usuário** — o convite (F6.6), no molde do ADR-0006.
 *
 * Um gesto, **duas escritas**, e a ordem entre elas importa:
 *
 * 1. o **funcionário** primeiro, quando o convite é de operador — é ele que o primeiro acesso procura
 *    por e-mail para montar o elo (`users/{uid}.funcionarioId`), e a regra do servidor só aceita o elo
 *    se o funcionário já existir com aquele e-mail;
 * 2. o **convite** depois, que é o que diz o papel.
 *
 * Invertida, a janela ruim é real: alguém convidado entraria antes de o funcionário existir, o elo
 * seria recusado, e a pessoa ficaria com perfil sem operação — o estado que o app trata como "não é da
 * casa". Na ordem escolhida o pior caso é um funcionário sem convite, que ninguém alcança e a gestão
 * reescreve.
 */
@HiltViewModel
class FormUsuarioViewModel @Inject constructor(
    private val conviteRepository: ConviteRepository,
    private val funcionarioRepository: FuncionarioRepository,
    private val empresaRepository: EmpresaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FormUsuarioUiState())
    val uiState: StateFlow<FormUsuarioUiState> = _uiState.asStateFlow()

    private val _sucesso = Channel<Unit>(Channel.BUFFERED)
    val sucesso = _sucesso.receiveAsFlow()

    init {
        viewModelScope.launch {
            val empresas = empresaRepository.obterTodas().map { EmpresaOpcao(it.id, it.nome) }
            _uiState.update { it.copy(empresas = empresas) }
        }
    }

    fun onNomeChange(v: String) = _uiState.update { it.copy(nome = v, isNomeError = false) }
    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v, isEmailError = false) }

    /**
     * Trocar o papel **apaga a segunda metade** quando ela deixa de existir: um convite de plataforma com
     * empresa guardada gravaria a promessa de um vínculo que ninguém vai criar. Zerar é o que mantém o
     * estado igual ao que a pessoa vê.
     */
    fun onPapelChange(v: String) = _uiState.update {
        val papel = Usuario.Papel.de(v)
        it.copy(
            papel = papel,
            isPapelError = false,
            empresa = if (papel == Usuario.Papel.OPERADOR) it.empresa else "",
            isEmpresaError = false,
        )
    }

    fun onEmpresaChange(v: String) = _uiState.update { it.copy(empresa = v, isEmpresaError = false) }
    fun onCargoChange(v: String) = _uiState.update { it.copy(cargo = v, isCargoError = false) }

    fun salvar() {
        val estado = _uiState.value
        val erros = validarUsuario(estado)
        // `papel == null` é redundante com `erros.papel` — está aqui para o smart-cast: é o compilador
        // que impede um convite sem papel de chegar ao domínio.
        val papel = estado.papel
        if (!erros.valido || papel == null) {
            _uiState.update {
                it.copy(
                    isNomeError = erros.nome,
                    isEmailError = erros.email,
                    isPapelError = erros.papel,
                    isEmpresaError = erros.empresa,
                    isCargoError = erros.cargo,
                )
            }
            return
        }

        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            try {
                val email = estado.email.trim().lowercase()
                val empresaId = estado.empresas.firstOrNull { it.nome == estado.empresa }?.id.orEmpty()
                val convite = Convite(
                    email = email,
                    nome = estado.nome.trim(),
                    papel = papel,
                    empresaId = if (papel == Usuario.Papel.OPERADOR) empresaId else "",
                    cargo = if (papel == Usuario.Papel.OPERADOR) Funcionario.Cargo.de(estado.cargo) else null,
                )

                convite.vinculo?.let { vinculo ->
                    funcionarioRepository.salvar(
                        Funcionario(
                            id = "",
                            descricaoNome = convite.nome,
                            email = convite.email,
                            cargo = vinculo.cargo.name,
                            vinculos = listOf(vinculo),
                        )
                    )
                }
                conviteRepository.salvar(convite)
                _sucesso.send(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "salvar: ${e.message}", e)
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    private companion object {
        const val TAG = "formUsuarioViewModel"
    }
}