package dev.matheus.fluviapp.ui.states

import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.operacoes.Funcionario
import dev.matheus.fluviapp.domain.operacoes.Vinculo

/**
 * Estado do formulário de membro da equipe — puro (só dados + flags), no molde do ADR-0006.
 *
 * ### O que mudou de forma (F6.3)
 *
 * Saíram `agencia` e `lotacao`; entrou [vinculos]. O cadastro deixa de perguntar *"em que agência esta
 * pessoa está?"* e passa a perguntar *"em que empresas ela atua, e como em cada uma?"* — que é a pergunta
 * do ADR-0016 §6, e a única que sabe responder por quem trabalha em duas.
 *
 * Os vínculos ficam como **lista em edição**: acrescentar e remover são gestos do formulário, e só o
 * `salvar` grava. É o mesmo princípio das outras telas do molde — o estado é o rascunho, o repositório é
 * o fato.
 *
 * As flags [podeEscolherEmpresa] e [podeDefinirCargo] são o **recorte por quem cadastra** (ADR-0015
 * §2.1/§8.5) já resolvido pelo VM: a tela não pergunta o papel do logado, só desenha o que o estado diz.
 */
data class FormFuncionarioUiState(
    val titulo: Int = R.string.subtitle_cadastrar_novo_agente,

    val nome: String = "",
    val isNomeError: Boolean = false,

    /** Chave que liga o pré-cadastro à conta do Auth no primeiro acesso (ADR-0015 §2.1). */
    val email: String = "",
    val isEmailError: Boolean = false,

    /** Os vínculos já atribuídos — o que será gravado. */
    val vinculos: List<Vinculo> = emptyList(),
    val isVinculosError: Boolean = false,

    /** O vínculo **em montagem**: a empresa escolhida no seletor, por rótulo. */
    val empresaEmEdicao: String = "",
    val cargoEmEdicao: String = Funcionario.Cargo.AGENTE.name,

    val empresas: List<EmpresaOpcao> = emptyList(),
    val listaCargo: List<String> = emptyList(),

    /** `false` = a empresa é **implícita** (a de quem cadastra), sem seletor. */
    val podeEscolherEmpresa: Boolean = true,

    /** `false` = o membro nasce/permanece `AGENTE`; promover é da plataforma (§8.5). */
    val podeDefinirCargo: Boolean = true,

    val isProcessing: Boolean = false,
) {

    /**
     * Os vínculos **prontos para exibir**: o id vira nome de empresa aqui, e não no domínio (ADR-0019 —
     * DTO por caso de uso). Empresa que não está na lista aparece sem nome em vez de sumir: um vínculo
     * que existe e não se consegue nomear é informação, e escondê-lo faria a tela mentir sobre o que
     * será gravado.
     */
    val vinculosNaTela: List<VinculoNaTela>
        get() = vinculos.map { vinculo ->
            VinculoNaTela(
                empresaId = vinculo.empresaId,
                empresa = empresas.firstOrNull { it.id == vinculo.empresaId }?.nome.orEmpty(),
                cargo = vinculo.cargo.name,
            )
        }

    /** Só dá para acrescentar quando há empresa escolhida — e o botão diz isso ficando desabilitado. */
    val podeAdicionarVinculo: Boolean get() = empresaEmEdicao.isNotBlank()
}

/** Uma empresa **como opção de escolha**: o id que se grava e o nome que se lê. */
data class EmpresaOpcao(
    val id: String,
    val nome: String,
)

/** Um vínculo já formatado para a lista do formulário. */
data class VinculoNaTela(
    val empresaId: String,
    val empresa: String,
    val cargo: String,
)