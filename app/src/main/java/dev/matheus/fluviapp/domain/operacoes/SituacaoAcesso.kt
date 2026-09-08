package dev.matheus.fluviapp.domain.operacoes

/**
 * **Em que estado está o acesso de alguém** ([ADR-0032] D6 e Q1).
 *
 * Até 2026-09-08 o acesso não tinha estado: existir `users/{uid}` era poder entrar, e a única saída para
 * tirar alguém era o console. A D6 deu ao `ADM` o par **desativar/reativar** e uma **data de expiração** —
 * e o par importa, porque desativar sem reativar transforma um engano em ida ao console.
 *
 * ### Três estados, e por que não dois
 *
 * `DESATIVADO` e `EXPIRADO` produzem o mesmo efeito (não se escreve nada) e têm causas diferentes: um é
 * gesto de alguém, o outro é o relógio. Fundi-los faria a lista da seção Usuários dizer *"desativado"*
 * para quem ninguém desativou — e a correção de cada um é outra: reativar num caso, estender o prazo no
 * outro.
 *
 * *"Convidado"* **não** está aqui: é estado do convite, não do acesso — a pessoa que só foi convidada
 * ainda não tem `users/{uid}`. Quem junta as duas dimensões é a tela.
 */
enum class SituacaoAcesso {
    ATIVO,
    DESATIVADO,
    EXPIRADO;

    /** Só o [ATIVO] escreve. É a pergunta que a regra do servidor faz, com as mesmas duas condições. */
    val vigente: Boolean get() = this == ATIVO
}

/**
 * A situação do acesso, **pura** — e é o espelho em Kotlin da regra do servidor (o mesmo par
 * `PermissoesUsuario` × `firestore.rules` do ADR-0011).
 *
 * | ativo | expiraEm | resultado |
 * |---|---|---|
 * | `false` | qualquer | `DESATIVADO` — o gesto de alguém vence o relógio na hora de explicar |
 * | `true` | ausente | `ATIVO` — sem prazo é o caso normal |
 * | `true` | no futuro | `ATIVO` |
 * | `true` | passado ou agora | `EXPIRADO` |
 *
 * **O desativado vem primeiro** de propósito: quem foi desativado *e* tem prazo vencido é desativado, que
 * é a informação acionável — reativar resolve, esperar não.
 *
 * O limite é **exclusivo** (`agora >= expiraEm` já expirou), e é a mesma comparação que a regra faz com
 * `request.time`. Igualdade no milissegundo é caso teórico; o que não é teórico é as duas fronteiras
 * discordarem, e é por isso que a comparação é uma só, escrita duas vezes de propósito.
 */
fun situacaoDoAcesso(ativo: Boolean, expiraEm: Long?, agora: Long): SituacaoAcesso = when {
    !ativo -> SituacaoAcesso.DESATIVADO
    expiraEm != null && agora >= expiraEm -> SituacaoAcesso.EXPIRADO
    else -> SituacaoAcesso.ATIVO
}
