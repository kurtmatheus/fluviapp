package dev.matheus.fluviapp.ui.states

/**
 * Estado da busca de usuários (F6.6). O filtro é o **e-mail**, porque é o que identifica quem acessa —
 * o `Usuario` não tem nome, tem `username` (ADR-0015 §8.1), e o nome que aparece na linha é o do convite.
 */
data class PesquisaUsuarioUiState(
    val email: String = "",
    val resultados: List<UsuarioResultado> = emptyList(),
)

/**
 * Projeção de um convite para a lista (ADR-0019 — DTO por caso de uso).
 *
 * A [situacao] é o que o `usado` do convite vira em tela: **Convidado** enquanto ninguém entrou,
 * **Ativo** depois do primeiro acesso. O convite não some quando é usado — vira registro, que é a
 * política do projeto para a fase analítica: o descartado também é dado.
 */
data class UsuarioResultado(
    val email: String,
    val nome: String,
    val papel: String,
    /** "Empresa · CARGO" para operador; vazio para papel de plataforma, que não atua em empresa nenhuma. */
    val vinculo: String,
    val situacao: String,
)