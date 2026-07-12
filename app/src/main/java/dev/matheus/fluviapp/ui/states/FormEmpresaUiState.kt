package dev.matheus.fluviapp.ui.states

import dev.matheus.fluviapp.R

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
    val isProcessing: Boolean = false,
)
