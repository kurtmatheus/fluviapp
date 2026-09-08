package dev.matheus.fluviapp.ui.viewmodel.helpers.usuario

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * A tradução entre **o dia que se escolhe** e **o instante que a regra compara** ([ADR-0032] Q1) — pura, e
 * JVM-testável como as validações do molde do ADR-0006.
 *
 * O `expiraEm` do acesso é millis de época porque quem o lê é uma regra do Firestore, que compara
 * `request.time` e não sabe formatar data. A tela, por outro lado, pergunta um **dia**: ninguém define
 * prazo de acesso em milissegundos.
 *
 * ### A decisão que estas duas funções carregam: o prazo é o **fim** do dia
 *
 * *"Expira em 31/12"* quer dizer que o dia 31 ainda vale — e não que o acesso morre à meia-noite do dia 30
 * para o 31. Converter para o **início** do dia seria a leitura mais fácil de escrever e a mais errada de
 * explicar a quem opera: a pessoa perderia o acesso no dia que a tela diz ser o último.
 *
 * O fuso é o **do aparelho**, e é o certo para este caso: quem define o prazo e quem o cumpre estão na
 * mesma operação, e um prazo em UTC apareceria com um dia de diferença para quem o leu.
 */
private val FORMATO_BR: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/**
 * `dd/MM/yyyy` → o instante em que o acesso deixa de valer, ou `null` quando não há prazo.
 *
 * Texto em branco é **sem prazo**, e texto ilegível também: o campo é preenchido por um seletor de data,
 * então algo fora do formato só chega por caminho que não é a tela — e "não entendi a data" tem de virar
 * ausência de prazo, nunca um prazo inventado.
 */
fun prazoEmMillis(texto: String, zona: ZoneId = ZoneId.systemDefault()): Long? =
    runCatching { LocalDate.parse(texto.trim(), FORMATO_BR) }
        .getOrNull()
        ?.atTime(LocalTime.MAX)
        ?.atZone(zona)
        ?.toInstant()
        ?.toEpochMilli()

/** O caminho de volta, para o campo mostrar o dia que foi escolhido. Sem prazo → texto vazio. */
fun prazoFormatado(millis: Long?, zona: ZoneId = ZoneId.systemDefault()): String =
    millis?.let {
        Instant.ofEpochMilli(it).atZone(zona).toLocalDate().format(FORMATO_BR)
    }.orEmpty()
