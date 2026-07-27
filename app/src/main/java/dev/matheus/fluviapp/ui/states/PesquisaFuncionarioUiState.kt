package dev.matheus.fluviapp.ui.states

import dev.matheus.fluviapp.model.operacoes.Funcionario

/**
 * Estado da busca de funcionários — separado do formulário (molde: form e busca não compartilham
 * VM/state). `resultados` já vem filtrado (filtro no VM, não no composable).
 *
 * [podeFiltrarPorAgencia] é o recorte do ADR-0015 §2.2: para o supervisor a agência é **implícita**, e
 * por isso deixa de ser filtro — sobra a lotação. Não é UI escondendo opção: a lista dele só contém a
 * própria agência, então um filtro de agência ali não teria o que filtrar.
 */
data class PesquisaFuncionarioUiState(
    val agencia: String = "",
    val listaAgencia: List<String> = emptyList(),
    val lotacao: String = "",
    val listaLotacao: List<String> = emptyList(),
    val resultados: List<Funcionario> = emptyList(),
    val podeFiltrarPorAgencia: Boolean = true,
    val podeDeletar: Boolean = true,
)