package dev.matheus.fluviapp.ui.states

import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.model.operacoes.Funcionario

/**
 * Estado do formulário de funcionário — puro (só dados + flags), sem lambdas. Eventos são métodos no
 * FormFuncionarioViewModel (molde cadastro-modulos §7.2). Só campos de formulário — a busca tem estado
 * próprio (PesquisaFuncionarioUiState).
 *
 * As flags [podeEscolherAgencia] e [podeDefinirCargo] são o **recorte por quem cadastra** (ADR-0015
 * §2.1/§8.5) já resolvido pelo VM: a tela não pergunta o papel do logado, só desenha o que o estado diz
 * — a política continua num lugar só (ADR-0010).
 */
data class FormFuncionarioUiState(
    val titulo: Int = R.string.subtitle_cadastrar_novo_agente,
    val agencia: String = "",
    val isAgenciaError: Boolean = false,
    val funcionario: String = "",
    val isFuncionarioError: Boolean = false,
    /** Chave que liga o pré-cadastro à conta do Auth no primeiro acesso (ADR-0015 §2.1). */
    val email: String = "",
    val isEmailError: Boolean = false,
    val lotacao: String = "",
    val isLotacaoError: Boolean = false,
    val cargo: String = Funcionario.Cargo.AGENTE.name,
    val listaAgencia: List<String> = emptyList(),
    val listaMunicipios: List<String> = emptyList(),
    val listaCargo: List<String> = emptyList(),
    /** `false` = agência **implícita** (a do supervisor), exibida sem seletor. */
    val podeEscolherAgencia: Boolean = true,
    /** `false` = o membro nasce/permanece `AGENTE`; promover é da plataforma. */
    val podeDefinirCargo: Boolean = true,
    val isProcessing: Boolean = false,
)