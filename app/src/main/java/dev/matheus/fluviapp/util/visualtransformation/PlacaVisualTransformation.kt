package dev.matheus.fluviapp.util.visualtransformation

/**
 * A máscara da **placa** ([ADR-0018] D15) — `ABC-1D23`.
 *
 * Ela existe por uma razão que o pool de veículos torna concreta: a placa é a **chave natural**, e este pool,
 * ao contrário do de pessoas, **não deveria acumular duplicata** — a placa é única por construção. A única
 * fonte de duplicata aqui é **digitação errada**, e é contra ela que a máscara serve: separar as três letras
 * dos quatro caracteres seguintes faz a troca de posição saltar aos olhos antes de o bilhete existir.
 *
 * Cobre os dois padrões em circulação, e sem precisar distinguir um do outro: `ABC-1234` (antigo) e
 * `ABC-1D23` (Mercosul) têm o mesmo formato de agrupamento — três, hífen, quatro. O que muda entre eles é a
 * natureza do quinto caractere, e isso é assunto de validação, não de exibição.
 *
 * A canonização (sem hífen, caixa alta) é do codec: aqui se **mostra**, lá se **grava**.
 */
class PlacaVisualTransformation : GenericSeparatorVisualTransformation() {

    override fun transform(input: CharSequence): CharSequence {
        val limpo = input.filter { it.isLetterOrDigit() }.take(TAMANHO).toString().uppercase()
        return if (limpo.length <= LETRAS) limpo else "${limpo.take(LETRAS)}-${limpo.drop(LETRAS)}"
    }

    override fun isSeparator(char: Char): Boolean = !char.isLetterOrDigit()

    private companion object {
        const val LETRAS = 3
        const val TAMANHO = 7
    }
}