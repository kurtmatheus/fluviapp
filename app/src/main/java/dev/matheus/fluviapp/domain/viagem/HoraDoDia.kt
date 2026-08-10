package dev.matheus.fluviapp.domain.viagem

/**
 * **A hora como minutos desde a meia-noite** (decisão do analista, 2026-08-10) — e a exibição continua
 * sendo `HH:mm`.
 *
 * A escolha não é de gosto: a hora da [Viagem] é o **único horário do app sobre o qual se faz conta**. O
 * `criadoEm` é texto porque ninguém soma datas de criação; aqui alguém soma — a chegada estimada é
 * `hora + tempoMedioH` (ADR-0016 §7.1), e uma `String "18:30"` obrigaria cada leitor a ter o próprio
 * parser. Um `Int` faz a aritmética ser aritmética, e concentra a formatação em duas funções.
 *
 * O texto vive **só na fronteira**: [formatarHora] ao mostrar, [minutosDaHora] ao ler o que a tela
 * devolve. Nada entre as duas manipula `"HH:mm"`.
 */
const val MINUTOS_POR_DIA: Int = 24 * 60

/**
 * Minutos → `HH:mm`. Valores acima de um dia **dão a volta**, porque é o que a chegada estimada de uma
 * travessia longa produz: 26h depois da saída é uma hora do relógio, num dia adiante — e quantos dias
 * adiante é [Chegada.diasDepois] quem diz, não o relógio.
 */
fun formatarHora(minutos: Int): String {
    val doDia = Math.floorMod(minutos, MINUTOS_POR_DIA)
    return "%02d:%02d".format(doDia / 60, doDia % 60)
}

/**
 * **Máscara de digitação**: o que a pessoa digita (só dígitos) vira `HH:mm` enquanto ela digita.
 *
 * Ela existe por um motivo bem concreto, achado em homologação: **o teclado numérico do Android não tem
 * `:`**. O campo pedia `HH:mm` e oferecia um teclado onde os dois-pontos não existem — não era um atrito,
 * era um campo intransponível. Trocar o teclado para o completo resolveria o sintoma e pioraria o resto
 * (letras num campo de hora); a máscara resolve a causa: **o separador deixa de ser digitado e passa a
 * ser escrito pelo campo**.
 *
 * Quatro dígitos, no máximo — é o tamanho de uma hora. O que passa disso é descartado em vez de empurrar
 * a hora para a esquerda, porque digitar rápido demais não deve mudar o que já está certo.
 *
 * Apagar funciona pelo mesmo caminho, sem regra à parte: `"18:30"` menos um caractere é `"18:3"`, que
 * são os dígitos `183`, que a máscara reescreve como `"18:3"`. O `:` some sozinho quando sobram dois.
 */
fun mascararHora(texto: String): String {
    val digitos = texto.filter { it.isDigit() }.take(4)
    return if (digitos.length <= 2) digitos else "${digitos.take(2)}:${digitos.drop(2)}"
}

/**
 * `HH:mm` → minutos, ou **`null` quando não é uma hora**.
 *
 * Devolve `null` em vez de zero de propósito: `"00:00"` é meia-noite, um horário legítimo de saída de
 * balsa, e confundi-lo com "não digitou" faria o formulário aceitar campo vazio como madrugada. Recusa
 * 24h ou 60min — fora do relógio não é hora mal escrita, é outra coisa.
 *
 * **Exige dois dígitos de cada lado**, e essa exigência nasceu com a [mascararHora]. Antes ela aceitava
 * `"8:05"` "porque é o que se digita" — com a máscara, não é: quem digita `805` vê `"80:5"`. O que a
 * tolerância passaria a permitir é a armadilha oposta: parar em três dígitos e gravar `"18:3"` como
 * **18:03** quando se queria 18:30, silenciosamente. Hora pela metade é hora incompleta, não hora
 * abreviada.
 */
fun minutosDaHora(texto: String): Int? {
    val partes = texto.trim().split(":")
    if (partes.size != 2) return null
    if (partes[0].length != 2 || partes[1].length != 2) return null

    val hora = partes[0].toIntOrNull() ?: return null
    val minuto = partes[1].toIntOrNull() ?: return null
    if (hora !in 0..23 || minuto !in 0..59) return null

    return hora * 60 + minuto
}

/** A hora está dentro do relógio? É o que o cadastro pergunta antes de gravar. */
fun horaValida(minutos: Int): Boolean = minutos in 0 until MINUTOS_POR_DIA