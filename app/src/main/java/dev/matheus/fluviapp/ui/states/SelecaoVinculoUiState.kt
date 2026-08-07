package dev.matheus.fluviapp.ui.states

/**
 * Estado da **seleção de contexto** (ADR-0016 §6, F6.4): em nome de qual empresa a pessoa vai operar.
 *
 * A tela existe só quando há mais de uma opção — quem tem uma nunca chega aqui —, e por isso ela não
 * tem estado vazio a desenhar: se [opcoes] chegasse vazia, o destino estaria errado antes da tela.
 */
data class SelecaoVinculoUiState(
    val nome: String = "",
    val opcoes: List<VinculoOpcao> = emptyList(),
    val carregando: Boolean = true,
)

/**
 * Uma opção de contexto, já formatada: a empresa e o que a pessoa é **nela** (ADR-0019 — DTO por caso de
 * uso). O cargo aparece porque é ele que muda o que a pessoa vai poder fazer depois de escolher — e
 * escolher sem ver isso seria escolher no escuro.
 */
data class VinculoOpcao(
    val empresaId: String,
    val empresa: String,
    val cargo: String,
)