package dev.matheus.fluviapp.ui.states

import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.operacoes.Atuacao

/**
 * Estado do formulário de empresa — puro (só dados + flags). Sem lambdas embutidas: os eventos são
 * métodos no FormEmpresaViewModel (molde cadastro-modulos §7.2). `cnpj` guarda só dígitos; a máscara
 * é aplicada na exibição (CnpjVisualTransformation).
 */
data class FormEmpresaUiState(
    val titulo: Int = R.string.subtitle_cadastrar_nova_empresa,
    val nome: String = "",
    val isNomeError: Boolean = false,
    val razaoSocial: String = "",
    val isRazaoSocialError: Boolean = false,
    val cnpj: String = "",
    val isCnpjError: Boolean = false,
    val endereco: String = "",
    val telefone1: String = "",
    val telefone2: String = "",
    /**
     * O que esta parte FAZ (ADR-0016 §4). Conjunto porque uma empresa exerce várias atuações ao mesmo
     * tempo — não é escolha única, e não é subtipo.
     */
    val atuacoes: Set<Atuacao> = emptySet(),
    val isProcessing: Boolean = false,
)
