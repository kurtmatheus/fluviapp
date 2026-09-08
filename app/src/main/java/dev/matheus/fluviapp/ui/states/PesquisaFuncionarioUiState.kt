package dev.matheus.fluviapp.ui.states

/**
 * Estado da busca de membros — separado do formulário (molde: form e busca não compartilham VM/state).
 * `resultados` já vem filtrado (filtro no VM, não no composable).
 *
 * [podeFiltrarPorEmpresa] é o recorte do ADR-0015 §2.2 com a coordenada da F6.3: para o supervisor a
 * empresa é **implícita**, e por isso deixa de ser filtro. Não é UI escondendo opção — a lista dele só
 * contém quem tem vínculo com a empresa dele, então um filtro de empresa ali não teria o que filtrar.
 */
data class PesquisaFuncionarioUiState(
    val nome: String = "",
    val empresa: String = "",
    val empresas: List<EmpresaOpcao> = emptyList(),
    val resultados: List<FuncionarioResultado> = emptyList(),
    val podeFiltrarPorEmpresa: Boolean = true,
    val podeDeletar: Boolean = true,
)

/**
 * Projeção de um membro para a lista (ADR-0019 — DTO por caso de uso, e este caso é exibir).
 *
 * [vinculo] chega **pronto para ler** — "Empresa X · SUPERVISOR" —, e é aqui que o `empresaId` do
 * documento vira nome. `null` quando não há vínculo: é o pré-cadastro (§2.1), e a linha simplesmente não
 * aparece.
 */
data class FuncionarioResultado(
    val id: String,
    val nome: String,
    val email: String,
    val vinculo: String?,
)