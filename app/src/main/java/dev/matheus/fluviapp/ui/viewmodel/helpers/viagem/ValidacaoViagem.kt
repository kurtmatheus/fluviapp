package dev.matheus.fluviapp.ui.viewmodel.helpers.viagem

import dev.matheus.fluviapp.domain.viagem.Viagem
import dev.matheus.fluviapp.domain.viagem.ativaComChave
import dev.matheus.fluviapp.domain.viagem.diaSemanaPorRotulo
import dev.matheus.fluviapp.domain.viagem.minutosDaHora
import dev.matheus.fluviapp.ui.states.ErroHoraViagem
import dev.matheus.fluviapp.ui.states.FormViagemUiState

/**
 * Validação do formulário de viagem — pura e JVM-testável.
 *
 * Os três primeiros campos são escolhas de dropdown, e por isso a queixa deles é binária: ou se escolheu,
 * ou não. A **hora** concentra as decisões, e a ordem entre elas importa: faltando → ilegível →
 * duplicada. Acusar duplicidade antes de saber ler a hora seria comparar com uma chave incompleta.
 *
 * A **duplicidade não é da hora** — é do par `(rota, embarcação, dia, hora)` —, mas é no campo da hora
 * que ela se resolve: os outros três costumam estar certos, e o que se muda é o horário. Marcar os
 * quatro mandaria corrigir o que já está certo.
 *
 * O que **não** está aqui é o recorte por concessão: os dropdowns já chegam recortados, então "rota que
 * a empresa não pode ofertar" não é um erro a acusar — é uma opção que não existe.
 */
data class ErrosViagem(
    val rota: Boolean = false,
    val embarcacao: Boolean = false,
    val diaSemana: Boolean = false,
    val hora: ErroHoraViagem = ErroHoraViagem.NENHUM,
) {
    val valido: Boolean get() = !rota && !embarcacao && !diaSemana && !hora.existe
}

fun validarViagem(state: FormViagemUiState): ErrosViagem = ErrosViagem(
    rota = idDaRota(state) == null,
    embarcacao = idDaEmbarcacao(state) == null,
    diaSemana = diaSemanaPorRotulo(state.diaSemana) == null,
    hora = erroDaHora(state),
)

/**
 * A chave que a viagem terá, ou `null` se o formulário ainda não a determina. É o que a validação usa
 * para a duplicidade e o que o ViewModel usa para gravar — **a mesma leitura**, e não duas parecidas que
 * podem divergir.
 */
fun chaveDaViagem(state: FormViagemUiState): Viagem.Chave? {
    val rotaId = idDaRota(state) ?: return null
    val embarcacaoId = idDaEmbarcacao(state) ?: return null
    val dia = diaSemanaPorRotulo(state.diaSemana) ?: return null
    val horaMin = minutosDaHora(state.hora) ?: return null

    return Viagem.Chave(rotaId, embarcacaoId, dia, horaMin)
}

private fun idDaRota(state: FormViagemUiState): String? =
    state.rotas.firstOrNull { it.rotulo == state.rota }?.id?.takeIf { it.isNotBlank() }

private fun idDaEmbarcacao(state: FormViagemUiState): String? =
    state.embarcacoes.firstOrNull { it.rotulo == state.embarcacao }?.id?.takeIf { it.isNotBlank() }

private fun erroDaHora(state: FormViagemUiState): ErroHoraViagem {
    if (state.hora.isBlank()) return ErroHoraViagem.OBRIGATORIA
    if (minutosDaHora(state.hora) == null) return ErroHoraViagem.INVALIDA

    // Duplicidade só faz sentido com a chave inteira: com um dropdown por escolher, ainda não se sabe
    // qual saída é esta, e "já existe" seria dito sobre uma partida que não está determinada.
    val chave = chaveDaViagem(state) ?: return ErroHoraViagem.NENHUM

    // Só entre as **ativas**: uma viagem inativada com a mesma chave é registro do passado, e recusar por
    // causa dela impediria de recriar exatamente o que se acabou de corrigir — o único jeito de corrigir,
    // já que não há edição.
    return if (state.viagensExistentes.ativaComChave(chave) != null) {
        ErroHoraViagem.DUPLICADA
    } else {
        ErroHoraViagem.NENHUM
    }
}