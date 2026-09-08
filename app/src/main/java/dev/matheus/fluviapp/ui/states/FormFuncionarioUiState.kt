package dev.matheus.fluviapp.ui.states

import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.operacoes.Funcionario
import dev.matheus.fluviapp.domain.operacoes.Vinculo

/**
 * Estado do formulário de membro da equipe — puro (só dados + flags), no molde do ADR-0006.
 *
 * ### O que mudou de forma (F6.3, depois [ADR-0032] Q2)
 *
 * Saíram `agencia` e `lotacao`; entrou o **vínculo**. O cadastro deixa de perguntar *"em que agência esta
 * pessoa está?"* e passa a perguntar *"em que empresa ela atua, e como"* — que é a pergunta do ADR-0016
 * §6.
 *
 * Por um tempo essa pergunta admitia várias respostas, e o formulário tinha **lista em edição**:
 * acrescentar, remover e um botão para cada gesto. A D5 descartou o caso de servir a duas empresas, e com
 * ele os gestos: **os dois seletores são o vínculo**. Não sobrou lista para editar, então não sobrou
 * rascunho a distinguir do gravado — [vinculo] deriva do que está na tela, e é o que o `salvar` grava.
 *
 * As flags [podeEscolherEmpresa] e [podeDefinirCargo] são o **recorte por quem cadastra** (ADR-0015
 * §2.1/§8.5) já resolvido pelo VM: a tela não pergunta o papel do logado, só desenha o que o estado diz.
 */
data class FormFuncionarioUiState(
    val titulo: Int = R.string.subtitle_cadastrar_novo_agente,

    val nome: String = "",
    /**
     * Se quem esta operando pode **gravar** este cadastro (ADR-0032 D1). Nasce `false`: fail-closed, como
     * o resto da politica — sem sessao resolvida, nao se grava.
     */
    val podeCadastrar: Boolean = false,
    val isNomeError: Boolean = false,

    /** Chave que liga o pré-cadastro à conta do Auth no primeiro acesso (ADR-0015 §2.1). */
    val email: String = "",
    val isEmailError: Boolean = false,

    /** A empresa do vínculo, **por rótulo** — é assim que o seletor a devolve. */
    val empresa: String = "",
    val isEmpresaError: Boolean = false,
    val cargo: String = Funcionario.Cargo.AGENTE.name,

    val empresas: List<EmpresaOpcao> = emptyList(),
    val listaCargo: List<String> = emptyList(),

    /** `false` = a empresa é **implícita** (a de quem cadastra), sem seletor. */
    val podeEscolherEmpresa: Boolean = true,

    /** `false` = o membro nasce/permanece `AGENTE`; promover é da plataforma (§8.5). */
    val podeDefinirCargo: Boolean = true,

    val isProcessing: Boolean = false,
) {

    /**
     * **O vínculo que a tela está descrevendo**, ou `null` enquanto ela não descreve nenhum.
     *
     * O rótulo vira id aqui, e não no domínio (ADR-0019 — DTO por caso de uso): é a tradução inversa da
     * que o dropdown fez. Empresa que não está na lista não vira vínculo — e é o mesmo `null` de empresa
     * não escolhida, porque as duas situações têm o mesmo efeito: não há o que gravar.
     *
     * Quem recusa cargo ilegível é [Vinculo.de], a fronteira do domínio; o formulário não repete a regra.
     */
    val vinculo: Vinculo?
        get() = Vinculo.de(
            empresaId = empresas.firstOrNull { it.nome == empresa }?.id,
            cargo = if (podeDefinirCargo) cargo else Funcionario.Cargo.AGENTE.name,
        )
}

/** Uma empresa **como opção de escolha**: o id que se grava e o nome que se lê. */
data class EmpresaOpcao(
    val id: String,
    val nome: String,
)
