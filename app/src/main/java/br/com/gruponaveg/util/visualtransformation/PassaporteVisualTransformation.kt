package br.com.gruponaveg.util.visualtransformation

import br.com.gruponaveg.extensions.formatarCampoPassaporte

class PassaporteVisualTransformation : GenericSeparatorVisualTransformation() {

    override fun transform(input: CharSequence): CharSequence = input.toString().formatarCampoPassaporte()

    override fun isSeparator(char: Char): Boolean = !char.isLetterOrDigit()
}