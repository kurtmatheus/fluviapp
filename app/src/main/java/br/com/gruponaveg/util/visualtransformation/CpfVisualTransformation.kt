package br.com.gruponaveg.util.visualtransformation

import br.com.gruponaveg.extensions.formatarCampoCPF

class CpfVisualTransformation : GenericSeparatorVisualTransformation() {
    override fun transform(input: CharSequence): CharSequence = input.toString().formatarCampoCPF()

    override fun isSeparator(char: Char): Boolean = !char.isLetterOrDigit()
}