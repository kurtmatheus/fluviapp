package dev.matheus.fluviapp.ui.states

import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.operacoes.Funcionario
import dev.matheus.fluviapp.domain.operacoes.PermissoesUsuario
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
    /**
     * Os papéis que o convite oferece — **`GESTOR` e `OPERADOR`** ([ADR-0032] D6).
     *
     * Era `Usuario.Papel.entries`, os três sem filtro, e é assim que um `ADM` podia fabricar outro pelo
     * app: o papel do convite é o que a regra do servidor aceita em `users/{uid}` no primeiro acesso. A
     * lista vem da política ([PermissoesUsuario.papeisConvidaveis]) porque *quem pode ser convidado* é
     * pergunta de autorização, não de tela — e a mesma pergunta é feita na validação e no servidor.
     */
    val papeis: List<String> = PermissoesUsuario.papeisConvidaveis().map { it.name },
    val cargos: List<String> = Funcionario.Cargo.entries.map { it.name },

    val isProcessing: Boolean = false,
) {

    /**
     * A segunda metade da tela: só quem entra na operação tem empresa e cargo.
     *
     * **Pergunta à política** ([ADR-0032] D1) em vez de comparar o papel aqui — era uma de três cópias da
     * mesma pergunta, e a política é quem sabe o que "entrar na operação" significa.
     */
    val perguntaVinculo: Boolean get() = PermissoesUsuario.ehPapelDeOperacao(papel?.name)
}