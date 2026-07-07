package br.com.gruponaveg.util.visualtransformation

import br.com.gruponaveg.extensions.formatarCampoCNPJ

class CnpjVisualTransformation : GenericSeparatorVisualTransformation() {
    override fun transform(input: CharSequence): CharSequence = input.toString().formatarCampoCNPJ()

    override fun isSeparator(char: Char): Boolean = !char.isLetterOrDigit()
}