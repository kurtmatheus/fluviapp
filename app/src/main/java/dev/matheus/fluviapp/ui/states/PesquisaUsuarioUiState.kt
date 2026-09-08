package dev.matheus.fluviapp.ui.states

/**
 * Estado da busca de usuários (F6.6, e **escrita** desde a [ADR-0032] D6). O filtro é o **e-mail**, porque
 * é o que identifica quem acessa — o `Usuario` não tem nome, tem `username` (ADR-0015 §8.1), e o nome que
 * aparece na linha é o do convite.
 */
data class PesquisaUsuarioUiState(
    val email: String = "",
    val resultados: List<UsuarioResultado> = emptyList(),
    /**
     * Se quem está olhando pode **gerir acesso** ([ADR-0032] D6) — `ADM`, e mais ninguém. Nasce `false`:
     * fail-closed, como o resto da política; sem sessão resolvida, a lista é só leitura.
     */
    val podeGerir: Boolean = false,
    val isProcessing: Boolean = false,
)

/**
 * Projeção de **uma pessoa** para a lista (ADR-0019 — DTO por caso de uso), juntando as duas metades: o
 * convite (quem pode entrar) e o perfil (quem entrou, e em que estado).
 *
 * Era só o convite, e a lista dizia *Ativo* para quem tivesse usado — o que virou mentira quando o acesso
 * passou a ter estado ([ADR-0032] D6): um convite usado por alguém depois desativado continuaria dizendo
 * *Ativo*. A situação agora vem de onde ela mora, `users/{uid}`, e o convite responde só pelo passado.
 */
data class UsuarioResultado(
    /**
     * O **uid** de quem entrou, e `""` para quem só foi convidado.
     *
     * É o que separa as duas metades na prática: sem uid não há perfil, e sem perfil não há acesso a
     * gerir — não se desativa quem ainda não entrou. Desconvidar é outro gesto, e ele não existe (o
     * convite é registro, ADR-0021).
     */
    val id: String,
    val email: String,
    val nome: String,
    val papel: String,
    /** "Empresa · CARGO" para operador; vazio para papel de plataforma, que não atua em empresa nenhuma. */
    val vinculo: String,
    /** Ativo · Desativado · Expirado · Convidado — já em português, como o resto desta lista. */
    val situacao: String,
    /** O dia em que o acesso deixa de valer, `dd/MM/yyyy`; vazio quando não há prazo. */
    val prazo: String = "",
    /**
     * Se o acesso está ligado **agora** — é o que decide se o gesto oferecido é *desativar* ou *reativar*.
     *
     * Distinto de `situacao == "Ativo"` de propósito: quem expirou continua ligado, e o que ele precisa é
     * de prazo novo, não de reativação.
     */
    val ativo: Boolean = false,
    /** `false` para quem só foi convidado: não há acesso a governar antes do primeiro acesso. */
    val temAcesso: Boolean = false,
)
