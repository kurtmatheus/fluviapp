package dev.matheus.fluviapp.ui.viewmodel.helpers.inicio

import dev.matheus.fluviapp.domain.porto.Porto
import dev.matheus.fluviapp.domain.rota.Rota
import dev.matheus.fluviapp.domain.viagem.InicioDoPainel
import dev.matheus.fluviapp.domain.viagem.chegadaEstimada
import dev.matheus.fluviapp.domain.viagem.formatarHora
import dev.matheus.fluviapp.domain.viagem.rotulo
import dev.matheus.fluviapp.domain.viagem.rotuloCurto
import dev.matheus.fluviapp.ui.states.InicioDaTela
import dev.matheus.fluviapp.ui.states.ViagemDisponivelCard
import java.time.format.DateTimeFormatter

/**
 * `InicioDoPainel` (domínio) → `InicioDaTela` (apresentação): resolve ids em nomes e formata a partida.
 *
 * Mora fora do ViewModel de propósito. O `MainScreenViewModel` depende de `FirebaseAuth` e de `DataStore`
 * — não se constrói numa JVM —, e é por isso que ele nunca teve teste. Tirar daqui a parte que **decide
 * texto** faz a formatação ser exercitável sem Android, e deixa no ViewModel só o que é orquestração.
 *
 * A tradução é uma camada e não um atalho: o domínio decide *de quem é o painel* e *quais ocorrências
 * existem*; aqui só se escreve o que se lê. É por isso que `DaPlataforma` e `SemConcessao` atravessam sem
 * dado nenhum — não há o que formatar quando a resposta é "este painel não é este".
 */
fun InicioDoPainel.paraTela(
    rotasPorId: Map<String, Rota>,
    portosPorId: Map<String, String>,
    embarcacoes: Map<String, String>,
): InicioDaTela = when (this) {
    InicioDoPainel.DaPlataforma -> InicioDaTela.DaPlataforma
    InicioDoPainel.SemConcessao -> InicioDaTela.SemConcessao
    is InicioDoPainel.DaEmpresa -> InicioDaTela.DaEmpresa(
        disponiveis.map { ocorrencia ->
            val rota = rotasPorId[ocorrencia.viagem.rotaId]
            val chegada = rota?.let { ocorrencia.viagem.chegadaEstimada(it) }

            ViagemDisponivelCard(
                id = ocorrencia.id,
                viagemId = ocorrencia.viagem.id,
                // "Terça-feira, 11/08 · 18:00": o dia da semana responde *quando sai*, a data responde
                // *qual delas* — a viagem é semanal, então o dia sozinho é ambíguo entre as ocorrências.
                partida = "${ocorrencia.data.dayOfWeek.rotulo}, " +
                    "${ocorrencia.data.format(DIA_E_MES)} · ${formatarHora(ocorrencia.viagem.horaMin)}",
                rota = rota?.rotuloCom(portosPorId).orEmpty(),
                embarcacao = embarcacoes[ocorrencia.viagem.embarcacaoId].orEmpty(),
                // O dia só entra quando a travessia o atravessa: repeti-lo numa viagem que chega no
                // mesmo dia seria ruído, e omiti-lo numa que não chega seria engano.
                chegada = chegada?.let {
                    if (it.diasDepois > 0) "${it.diaSemana.rotuloCurto} ${formatarHora(it.horaMin)}"
                    else formatarHora(it.horaMin)
                }.orEmpty(),
            )
        }
    )
}

private val DIA_E_MES: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")

/** O rótulo do porto: nome e cidade, que é o que distingue homônimos. */
fun Porto.rotuloCom(localidades: Map<String, String>): String =
    listOfNotNull(nome, localidades[localidadeId]).filter { it.isNotBlank() }.joinToString(" · ")

/** "Porto A · Belém/PA → Porto B · Parintins/AM" — o par, na ordem, que é o que a rota é. */
fun Rota.rotuloCom(portosPorId: Map<String, String>): String {
    val origem = portosPorId[portoOrigemId] ?: portoOrigemId
    val destino = portosPorId[portoDestinoId] ?: portoDestinoId
    return "$origem → $destino"
}