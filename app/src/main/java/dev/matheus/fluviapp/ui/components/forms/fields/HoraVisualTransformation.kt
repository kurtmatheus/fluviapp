package dev.matheus.fluviapp.ui.components.forms.fields

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import dev.matheus.fluviapp.domain.viagem.mascararHora

/**
 * Desenha `HH:mm` sobre um campo que guarda **só dígitos**.
 *
 * O campo de hora não pode pedir o `:`: o teclado numérico do Android não o tem. E não pode guardá-lo:
 * inserir um caractere no meio do valor faz o Compose recalcular a seleção sobre o texto anterior, e o
 * cursor cai **atrás** do separador — o dígito seguinte entra no lugar errado (achado no teste manual,
 * 2026-08-10).
 *
 * Aqui o `:` existe só na pintura. O valor continua sendo `"1830"`, o cursor continua andando sobre
 * quatro posições, e o [OffsetMapping] traduz entre as duas réguas — que é exatamente o trabalho que a
 * máscara-no-valor fazia mal.
 */
object HoraVisualTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(AnnotatedString(mascararHora(text.text)), HoraOffsetMapping)
}

/**
 * As duas réguas: os dígitos (`0..4`) e o que se vê (`0..5`, com o `:` na posição 2).
 *
 * O deslocamento é de **um caractere, e só depois do segundo dígito** — antes dele as duas coincidem,
 * porque o `:` ainda não foi desenhado. Um mapeamento que devolvesse posição fora do texto derrubaria o
 * campo, então os dois lados param onde o texto para.
 */
private object HoraOffsetMapping : OffsetMapping {

    override fun originalToTransformed(offset: Int): Int = if (offset <= 2) offset else offset + 1

    override fun transformedToOriginal(offset: Int): Int = if (offset <= 2) offset else offset - 1
}