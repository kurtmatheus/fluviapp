package dev.matheus.fluviapp.ui.viewmodel.helpers.rota

import dev.matheus.fluviapp.domain.rota.ativaComPar
import dev.matheus.fluviapp.ui.states.ErroParRota
import dev.matheus.fluviapp.ui.states.FormRotaUiState

/**
 * Validação do formulário de rota — pura e JVM-testável.
 *
 * O par de portos concentra as três regras, e a **ordem entre elas importa**: faltando → repetido →
 * duplicado. Acusar duplicidade antes de saber que os dois portos existem seria comparar com metade da
 * informação; acusar antes de checar o sentido diria "já existe" para uma rota que nem é travessia.
 *
 * Distância e tempo são **obrigatórios e maiores que zero**, e isso não é preciosismo: são os dois
 * fatos que justificam a Rota existir em vez de ser derivada dos portos (§7.1). Uma rota de zero milhas
 * em zero horas não descreve travessia nenhuma — descreve um cadastro pela metade.
 */
data class ErrosRota(
    val par: ErroParRota = ErroParRota.NENHUM,
    val distancia: Boolean = false,
    val tempo: Boolean = false,
) {
    val valido: Boolean get() = !par.existe && !distancia && !tempo
}

fun validarRota(state: FormRotaUiState): ErrosRota = ErrosRota(
    par = erroDoPar(state),
    distancia = (state.distanciaMn.toDoubleOrNull() ?: 0.0) <= 0.0,
    tempo = (state.tempoMedioH.toDoubleOrNull() ?: 0.0) <= 0.0,
)

private fun erroDoPar(state: FormRotaUiState): ErroParRota {
    val origem = state.portos.firstOrNull { it.rotulo == state.portoOrigem }?.id
    val destino = state.portos.firstOrNull { it.rotulo == state.portoDestino }?.id

    if (origem.isNullOrBlank() || destino.isNullOrBlank()) return ErroParRota.OBRIGATORIO
    if (origem == destino) return ErroParRota.MESMO_PORTO

    // Duplicidade só entre as **ativas**: uma rota inativada com o mesmo par é registro do passado, e
    // recusar por causa dela impediria de recriar exatamente o que se acabou de corrigir.
    return if (state.rotasExistentes.ativaComPar(origem, destino) != null) {
        ErroParRota.DUPLICADA
    } else {
        ErroParRota.NENHUM
    }
}