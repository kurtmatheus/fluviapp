package dev.matheus.fluviapp.ui.states

import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.operacoes.Funcionario
import dev.matheus.fluviapp.domain.operacoes.Usuario

/**
 * Estado do formulário de **usuário** — o convite (F6.6), no molde do ADR-0006.
 *
 * A tela tem duas metades, e a segunda **só existe para o operador**: papel de plataforma não atua em
 * empresa nenhuma (ADR-0015 §8.1), então perguntar empresa e cargo a um `ADM` seria oferecer um campo
 * que nunca teria resposta. É a mesma escolha do tipo de embarcação (ADR-0016 §8): a pergunta some, em
 * vez de aparecer desabilitada.
 */
data class FormUsuarioUiState(
    val titulo: Int = R.string.subtitle_novo_usuario,

    val nome: String = "",
    val isNomeError: Boolean = false,

    /** É o **id do convite**: é por ele que o primeiro acesso encontra o papel de quem entrou. */
    val email: String = "",
    val isEmailError: Boolean = false,

    val papel: Usuario.Papel? = null,
    val isPapelError: Boolean = false,

    val empresa: String = "",
    val isEmpresaError: Boolean = false,

    val cargo: String = Funcionario.Cargo.AGENTE.name,
    val isCargoError: Boolean = false,

    val empresas: List<EmpresaOpcao> = emptyList(),
    val papeis: List<String> = Usuario.Papel.entries.map { it.name },
    val cargos: List<String> = Funcionario.Cargo.entries.map { it.name },

    val isProcessing: Boolean = false,
) {

    /** A segunda metade da tela: só quem entra na operação tem empresa e cargo. */
    val perguntaVinculo: Boolean get() = papel == Usuario.Papel.OPERADOR
}