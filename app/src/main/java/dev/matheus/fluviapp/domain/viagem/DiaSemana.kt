package dev.matheus.fluviapp.domain.viagem

import java.time.DayOfWeek

/**
 * O dia da semana **como a pessoa lê** — e as duas fronteiras separadas, como em `TipoEmbarcacao`.
 *
 * O tipo é o `java.time.DayOfWeek` (nativo no minSdk 26), então não há enum próprio a manter: o que falta
 * é só o texto. Ele mora aqui, e não em `getDisplayName(Locale)`, por uma razão prática — a formatação da
 * JVM depende do locale do aparelho e devolve grafias diferentes conforme a versão, o que faria a mesma
 * escolha virar textos distintos em telas distintas e quebraria [diaSemanaPorRotulo].
 *
 * **Duas leituras, duas fronteiras**, e a distinção é a mesma do `TipoEmbarcacao`: o Firestore grava o
 * `name` (`"TUESDAY"`, estável), a tela devolve o [rotulo] (que pode ser reescrito sem migrar dado).
 * Confundir as duas é atar a persistência ao texto da interface.
 *
 * A ordem de [DIAS_DA_SEMANA] é a do `DayOfWeek` — segunda a domingo, ISO-8601. É a que o Brasil usa em
 * horário de embarcação ("sai toda terça"), e não a de calendário começando no domingo.
 */
val DayOfWeek.rotulo: String
    get() = when (this) {
        DayOfWeek.MONDAY -> "Segunda-feira"
        DayOfWeek.TUESDAY -> "Terça-feira"
        DayOfWeek.WEDNESDAY -> "Quarta-feira"
        DayOfWeek.THURSDAY -> "Quinta-feira"
        DayOfWeek.FRIDAY -> "Sexta-feira"
        DayOfWeek.SATURDAY -> "Sábado"
        DayOfWeek.SUNDAY -> "Domingo"
    }

/** Forma curta, para onde a linha é apertada — o card de resultado. */
val DayOfWeek.rotuloCurto: String
    get() = when (this) {
        DayOfWeek.MONDAY -> "Seg"
        DayOfWeek.TUESDAY -> "Ter"
        DayOfWeek.WEDNESDAY -> "Qua"
        DayOfWeek.THURSDAY -> "Qui"
        DayOfWeek.FRIDAY -> "Sex"
        DayOfWeek.SATURDAY -> "Sáb"
        DayOfWeek.SUNDAY -> "Dom"
    }

val DIAS_DA_SEMANA: List<DayOfWeek> = DayOfWeek.entries

/** Fronteira de **tela**: o dropdown mostra o [rotulo] e devolve o texto escolhido. */
fun diaSemanaPorRotulo(rotulo: String?): DayOfWeek? =
    DIAS_DA_SEMANA.firstOrNull { it.rotulo.equals(rotulo?.trim(), ignoreCase = true) }