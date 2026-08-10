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
 * `HH:mm` → minutos, ou **`null` quando não é uma hora**.
 *
 * Devolve `null` em vez de zero de propósito: `"00:00"` é meia-noite, um horário legítimo de saída de
 * balsa, e confundi-lo com "não digitou" faria o formulário aceitar campo vazio como madrugada. Aceita
 * `8:05` sem o zero à esquerda (é o que se digita), e recusa 24h ou 60min — fora do relógio não é hora
 * mal escrita, é outra coisa.
 */
fun minutosDaHora(texto: String): Int? {
    val partes = texto.trim().split(":")
    if (partes.size != 2) return null

    val hora = partes[0].toIntOrNull() ?: return null
    val minuto = partes[1].toIntOrNull() ?: return null
    if (hora !in 0..23 || minuto !in 0..59) return null

    return hora * 60 + minuto
}

/** A hora está dentro do relógio? É o que o cadastro pergunta antes de gravar. */
fun horaValida(minutos: Int): Boolean = minutos in 0 until MINUTOS_POR_DIA