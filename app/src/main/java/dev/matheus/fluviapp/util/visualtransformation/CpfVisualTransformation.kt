package dev.matheus.fluviapp.util.visualtransformation

import dev.matheus.fluviapp.extensions.formatarCampoCPF

class CpfVisualTransformation : GenericSeparatorVisualTransformation() {
    override fun transform(input: CharSequence): CharSequence = input.toString().formatarCampoCPF()

    override fun isSeparator(char: Char): Boolean = !char.isLetterOrDigit()
}