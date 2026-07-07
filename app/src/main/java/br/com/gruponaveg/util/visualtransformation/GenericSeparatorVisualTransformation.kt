package br.com.gruponaveg.util.visualtransformation

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

abstract class GenericSeparatorVisualTransformation : VisualTransformation {

    abstract fun transform(input: CharSequence): CharSequence

    abstract fun isSeparator(char: Char): Boolean

    override fun filter(text: AnnotatedString): TransformedText {

        val formatted = transform(text)

        return TransformedText(
            text = AnnotatedString(text = formatted.toString()),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    val transformedOffsets = formatted
                        .mapIndexedNotNull { index, c ->
                            index
                                .takeIf { !isSeparator(c) }
                                // convert index to an offset
                                ?.plus(1)
                        }
                        // We want to support an offset of 0 and shift everything to the right,
                        // so we prepend that index by default
                        .let { offsetList ->
                            listOf(0) + offsetList
                        }

                    return transformedOffsets[offset]
                }

                override fun transformedToOriginal(offset: Int): Int =
                    formatted
                        // This creates a list of all separator offsets
                        .mapIndexedNotNull { index, c ->
                            index.takeIf { isSeparator(c) }
                        }
                        // We want to count how many separators precede the transformed offset
                        .count { separatorIndex ->
                            separatorIndex < offset
                        }
                        // We find the original offset by subtracting the number of separators
                        .let { separatorCount ->
                            offset - separatorCount
                        }
            }
        )
    }
}