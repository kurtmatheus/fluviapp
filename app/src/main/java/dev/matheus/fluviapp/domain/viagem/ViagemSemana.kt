package dev.matheus.fluviapp.domain.viagem

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * **A ocorrência concreta de uma [Viagem]** — `viagem_semana` no vocabulário do analista (2026-08-10).
 *
 * A `Viagem` é uma saída **semanal**: "terça às 18h". A `ViagemSemana` é uma dessas saídas **numa data**:
 * "terça, 12 de agosto, às 18h". É a diferença entre a regra e o acontecimento, e é sobre o acontecimento
 * que se vende bilhete e se conta ocupação — `(viagemId, data)` é a chave que o ADR-0016 §7.1 nomeou.
 *
 * ### Calculada, não persistida
 *
 * Não existe coleção para ela, e isso é decisão do §7: as ocorrências são **derivadas** do dia da semana
 * mais um intervalo. Persisti-las criaria uma segunda verdade a manter sincronizada com a viagem — e a
 * viagem é imutável justamente para que a derivação nunca precise ser refeita. O preço, já registrado no
 * ADR, é a ocupação continuar O(n) sobre bilhetes em vez de um contador O(1).
 *
 * ### O que "disponível" quer dizer
 *
 * Uma ocorrência é disponível se a **partida ainda não passou**. A conta é sobre o instante, não sobre o
 * dia: a saída das 06:00 de hoje não está disponível às 18:00 de hoje, e tratá-la como disponível porque
 * "é hoje" ofereceria um barco que já partiu.
 */
data class ViagemSemana(
    val viagem: Viagem,
    val data: LocalDate,
) {
    /** O instante da partida — data e hora juntas, que é o que ordena e o que compara. */
    val partida: LocalDateTime get() = data.atStartOfDay().plusMinutes(viagem.horaMin.toLong())

    val id: String get() = "${viagem.id}@$data"
}

/**
 * As ocorrências **disponíveis** a partir de um instante, dentro de uma janela de dias.
 *
 * A janela é deslizante — os próximos N dias a partir de hoje —, e não a semana do calendário. Uma semana
 * fixa de segunda a domingo esvaziaria a tela ao longo dela: no sábado, "Viagens Disponíveis" mostraria
 * dois dias. O que a agência precisa ver é sempre a mesma quantidade de futuro.
 *
 * Só as **ativas**: viagem inativada é registro do passado, e ofertá-la seria vender o que foi encerrado.
 * Ordenadas pela partida, porque é assim que se lê uma tabela de saídas.
 */
fun List<Viagem>.disponiveisAPartirDe(agora: LocalDateTime, dias: Long = DIAS_DA_JANELA): List<ViagemSemana> {
    if (dias <= 0) return emptyList()

    val hoje = agora.toLocalDate()
    val datas = (0 until dias).map { hoje.plusDays(it) }

    return filter { it.ativo }
        .flatMap { viagem -> datas.map { ViagemSemana(viagem, it) } }
        .filter { it.viagem.diaSemana == it.data.dayOfWeek }
        // `isBefore(agora)` e não uma comparação por dia: o barco das 06:00 já partiu às 18:00.
        .filterNot { it.partida.isBefore(agora) }
        .sortedBy { it.partida }
}

/**
 * Sete dias — a janela é uma semana porque a viagem é semanal: menos esconderia saídas que existem, mais
 * mostraria a mesma saída duas vezes.
 */
const val DIAS_DA_JANELA: Long = 7