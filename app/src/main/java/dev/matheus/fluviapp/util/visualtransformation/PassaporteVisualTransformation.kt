package dev.matheus.fluviapp.util.visualtransformation

import dev.matheus.fluviapp.extensions.formatarCampoPassaporte

class PassaporteVisualTransformation : GenericSeparatorVisualTransformation() {

    override fun transform(input: CharSequence): CharSequence = input.toString().formatarCampoPassaporte()

    override fun isSeparator(char: Char): Boolean = !char.isLetterOrDigit()
}