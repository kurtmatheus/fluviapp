package dev.matheus.fluviapp.util.visualtransformation

import dev.matheus.fluviapp.extensions.formatarCampoCNPJ

class CnpjVisualTransformation : GenericSeparatorVisualTransformation() {
    override fun transform(input: CharSequence): CharSequence = input.toString().formatarCampoCNPJ()

    override fun isSeparator(char: Char): Boolean = !char.isLetterOrDigit()
}