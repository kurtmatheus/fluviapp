package dev.matheus.fluviapp.util.visualtransformation

import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import dev.matheus.fluviapp.domain.documento.TipoDocumento

/**
 * A ponte entre o [TipoDocumento] (domínio puro) e o Compose. É aqui que mora a dependência de framework
 * — e é só aqui, para que o domínio não conheça `VisualTransformation` nem `KeyboardType` (ADR-0019 D2).
 *
 * Substitui as funções `visualTransformation(tipoDocumento: String)` e `keyboardType(...)` de
 * `extensions/UtilExtensions.kt`, que faziam um `when` sobre `Constante.Descricao` e caíam num `else`
 * silencioso — campo sem máscara e teclado errado — para qualquer valor que o administrador cadastrasse.
 * Com o tipo no lugar da String, **o `else` deixa de existir**: o `when` é exaustivo sobre o enum, e o
 * compilador é quem cobra o caso novo.
 *
 * O par das três classes `CpfVisualTransformation`/`CnpjVisualTransformation`/`PassaporteVisualTransformation`
 * é uma só: a diferença entre elas era o formatador, que agora é do tipo.
 */
class DocumentoVisualTransformation(
    private val tipo: TipoDocumento,
) : GenericSeparatorVisualTransformation() {

    override fun transform(input: CharSequence): CharSequence = tipo.formatarProgressivo(input.toString())

    override fun isSeparator(char: Char): Boolean = !char.isLetterOrDigit()
}

/**
 * `VisualTransformation` do campo deste tipo. Tipo ausente (nada selecionado ainda) não formata — que é
 * diferente do `else` antigo: ali um tipo **conhecido pelo gestor e desconhecido pelo código** também não
 * formatava, e ninguém ficava sabendo.
 */
fun TipoDocumento?.visualTransformation(): VisualTransformation =
    this?.let { DocumentoVisualTransformation(it) } ?: VisualTransformation.None

/** Teclado do campo deste tipo: numérico, salvo quando o documento admite letras (passaporte). */
fun TipoDocumento?.keyboardType(): KeyboardType =
    if (this == null || apenasDigitos) KeyboardType.Number else KeyboardType.Text