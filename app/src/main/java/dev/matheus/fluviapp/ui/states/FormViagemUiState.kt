package dev.matheus.fluviapp.ui.states

import dev.matheus.fluviapp.R
import dev.matheus.fluviapp.model.cadastro.constantes.Constante
import dev.matheus.fluviapp.model.viagem.Empresa
import dev.matheus.fluviapp.model.viagem.Navio

/**
 * Estado do formulário de viagem — puro (só dados + flags). Sem lambdas embutidas: os eventos são
 * métodos no FormViagemViewModel (molde cadastro-modulos §7.2).
 */
data class FormViagemUiState(
    val titulo: Int = R.string.subtitle_cadastrar_nova_viagem,

    val empresa: String = "",
    val isEmpresaError: Boolean = false,

    val navio: String = "",
    val isNavioError: Boolean = false,
    val navioDesabilitado: Boolean = true,

    val trechoOrigem: String = "",
    val isTrechoOrigemError: Boolean = false,

    val trechoDestino: String = "",
    val isTrechoDestinoError: Boolean = false,
    val trechoDestinoDesabilitado: Boolean = true,

    val listaEmpresas: List<Empresa> = emptyList(),
    val listaNavios: List<Navio> = emptyList(),
    val listaMunicipios: List<Constante> = emptyList(),

    // Tarifa da inteira por acomodação (ADR-0013) — um input por acomodação do catálogo. Branco = não
    // ofertada (não vira célula TarifaViagem). Preparado p/ as classes de veículo (Fase 3, mesma forma).
    val tarifas: List<TarifaInputUiState> = emptyList(),

    val isProcessando: Boolean = false,
)

/**
 * Entrada de tarifa da inteira de UMA célula no form de Viagem (ADR-0013). `chave` = chave tarifária
 * canônica — acomodação (REDE/SUITE/CAMAROTE) p/ passageiro, ou classe (CARRO/CARRETA/CAMINHAO) p/ veículo
 * (moto é por regra, não entra aqui). `valor` é texto livre (branco = não ofertada → sem célula; preenchido
 * tem de ser número > 0). `grupoTitulo` (@StringRes) rotula a seção onde a entrada aparece.
 */
data class TarifaInputUiState(
    val chave: String,
    val valor: String = "",
    val isError: Boolean = false,
    val grupoTitulo: Int = R.string.label_tarifas_titulo,
)
