package dev.matheus.fluviapp.ui.states

import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.domain.viagem.Empresa

/**
 * Estado do formulário de navio — puro (só dados + flags). Sem lambdas embutidas: os eventos são
 * métodos no FormNavioViewModel (molde cadastro-modulos §7.2). As capacidades guardam só dígitos
 * (teclado numérico + filtro), então são sempre inteiros válidos (em branco = 0). `empresa` é o nome
 * selecionado no dropdown (estado de UI); o vínculo persistido é por id (o VM resolve — ADR-0008).
 */
data class FormNavioUiState(
    val titulo: Int = R.string.subtitle_cadastrar_novo_navio,

    val nome: String = "",
    val isNomeError: Boolean = false,

    val empresa: String = "",
    val isEmpresaError: Boolean = false,

    val capacidadeVeiculo: String = "",
    val capacidadeSuite2: String = "",
    val capacidadeSuite3: String = "",
    val capacidadeCamarote: String = "",

    val listaEmpresas: List<Empresa> = emptyList(),

    val isProcessing: Boolean = false,
)
