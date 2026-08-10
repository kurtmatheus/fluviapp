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
 * **Os dígitos que a tela guarda** — no máximo quatro, que é o tamanho de uma hora.
 *
 * O excedente é descartado em vez de empurrar a hora para a esquerda: digitar rápido demais não deve
 * mudar o que já está certo.
 */
fun digitosDaHora(texto: String): String = texto.filter { it.isDigit() }.take(4)

/**
 * **A máscara `HH:mm` sobre os dígitos** — e ela é de **exibição**, não de digitação.
 *
 * A distinção custou uma rodada de homologação e vale escrita. O problema original era que **o teclado
 * numérico do Android não tem `:`**: o campo pedia `HH:mm` e oferecia um teclado onde o separador não
 * existe — não era atrito, era um campo intransponível.
 *
 * A primeira correção aplicou esta função **ao valor do campo**, e trocou um defeito por outro: com
 * `TextField(value: String)`, o Compose reposiciona o cursor calculando sobre o texto que ele acabou de
 * enviar. Inserir o `:` no meio faz o cursor cair **atrás** dele, e o dígito seguinte entra no lugar
 * errado. Foi o que o teste manual pegou.
 *
 * A correção certa é não guardar o separador: o estado fica com os dígitos ([digitosDaHora]) e o `:` é
 * **desenhado** por uma `VisualTransformation`. Sem caractere novo no valor, não há cursor a
 * reposicionar — o problema deixa de existir em vez de ser compensado.
 *
 * Apagar segue sem regra à parte: some um dígito, a máscara reescreve, e o `:` desaparece sozinho
 * quando sobram dois.
 */
fun mascararHora(texto: String): String {
    val digitos = digitosDaHora(texto)
    return if (digitos.length <= 2) digitos else "${digitos.take(2)}:${digitos.drop(2)}"
}

/**
 * Os dígitos que a tela guarda → minutos. É a ponte entre o que o campo tem (`"1830"`) e o que o domínio
 * lê (`"18:30"`), num lugar só — para que a validação e a gravação não a reconstruam cada uma à sua
 * maneira.
 *
 * Herda a exigência de [minutosDaHora]: três dígitos não viram hora.
 */
fun minutosDosDigitos(digitos: String): Int? = minutosDaHora(mascararHora(digitos))

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