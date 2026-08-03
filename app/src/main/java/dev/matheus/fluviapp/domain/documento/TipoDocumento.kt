package dev.matheus.fluviapp.domain.documento

/**
 * Tipo de documento como **tipo de domínio** ([ADR-0020 D2]), no lugar da linha de catálogo
 * `Constante.Categoria.DOCUMENTO`.
 *
 * O catálogo nunca foi a fonte: o código já decidia máscara e teclado com um `when` sobre
 * `Constante.Descricao` (`UtilExtensions.kt:10-20`) e a ocultação com outro (`extrairDocumentoFormatado`).
 * Item novo cadastrado pelo administrador caía no `else` — **sem máscara e sem erro** no primeiro, e
 * devolvendo **string vazia** no segundo, isto é, o documento sumia do bilhete em silêncio. Um valor que
 * governa três comportamentos não é rótulo.
 *
 * A razão de fundo não é organização de código: **máscara é tratamento de dado pessoal (LGPD)**. Formatar
 * e ocultar parcialmente é política de exibição de identificador, e política não mora numa linha do
 * Firestore que alguém edita, nem depende de a coleção ter sincronizado.
 *
 * **Este tipo é puro.** Ele não conhece Compose: expõe o formato e a política, e a camada de apresentação
 * traduz para `VisualTransformation`/`KeyboardType` (`util/visualtransformation/DocumentoVisual.kt`). É o
 * que mantém o domínio sem dependência de framework (ADR-0019 D2).
 *
 * Todas as funções são **totais** — nunca lançam. O código anterior fatiava por índice fixo e estourava
 * `StringIndexOutOfBounds` em documento incompleto, o que obrigou a um comentário de robustez em
 * `extrairDocumentoFormatado`; aqui a garantia é da assinatura.
 */
enum class TipoDocumento(
    val rotulo: String,
    /** `true` = só dígitos (teclado numérico); `false` admite letras (passaporte). */
    val apenasDigitos: Boolean,
    /** Quantidade de caracteres significativos que o documento tem quando completo. */
    val comprimento: IntRange,
) {
    CPF("CPF", apenasDigitos = true, comprimento = 11..11),
    CNPJ("CNPJ", apenasDigitos = true, comprimento = 14..14),
    RG("RG", apenasDigitos = true, comprimento = 5..14),
    CNH("CNH", apenasDigitos = true, comprimento = 11..11),
    PASSAPORTE("Passaporte", apenasDigitos = false, comprimento = 8..8);

    /**
     * Reduz o valor ao que é significativo: descarta separadores e, no passaporte, normaliza a caixa.
     * É a forma **canônica** — é ela que se persiste e sobre a qual todas as outras funções operam.
     */
    fun normalizar(bruto: String?): String {
        val limpo = bruto.orEmpty().filter { if (apenasDigitos) it.isDigit() else it.isLetterOrDigit() }
        return if (apenasDigitos) limpo else limpo.uppercase()
    }

    /** `true` quando o valor normalizado tem o comprimento que este tipo exige. */
    fun estaCompleto(valor: String?): Boolean = normalizar(valor).length in comprimento

    /**
     * Formatação **progressiva**, para o campo enquanto se digita: formata o que já existe sem exigir o
     * documento completo. É o que a `VisualTransformation` consome.
     */
    fun formatarProgressivo(valor: String?): String {
        val d = normalizar(valor)
        return when (this) {
            CPF -> agrupar(d, tamanhos = listOf(3, 3, 3, 2), separadores = listOf('.', '.', '-'))
            CNPJ -> agrupar(d, tamanhos = listOf(2, 3, 3, 4, 2), separadores = listOf('.', '.', '/', '-'))
            PASSAPORTE -> agrupar(d, tamanhos = listOf(2, 6), separadores = listOf('-'))
            RG, CNH -> d
        }
    }

    /**
     * Formatação de exibição. Documento incompleto sai **como está** (normalizado) em vez de sumir — o
     * `else -> ""` do código anterior apagava o documento do bilhete sem avisar ninguém.
     */
    fun formatar(valor: String?): String {
        val d = normalizar(valor)
        return if (d.length in comprimento) formatarProgressivo(d) else d
    }

    /**
     * Exibição com **ocultação parcial** (LGPD): mantém o documento reconhecível para conferência de
     * balcão e esconde o suficiente para que a tela/o bilhete não sejam uma cópia do identificador.
     *
     * **O CPF esconde os 6 primeiros dígitos e mostra os 5 últimos** (`###.###.247-25`). A forma anterior
     * — herdada de `mascararCPF`, que escondia as pontas — expunha os 6 do meio; esta expõe 5 e é a que o
     * balcão precisa para conferir contra o documento físico. Foi a primeira vez que essa política pôde
     * ser decidida numa linha: antes ela estava espalhada por quatro funções de extensão.
     *
     * As demais formas replicam o que o app já imprimia, para que a F2 não mude nada visível nelas.
     *
     * O **CNPJ não é ocultado**: identifica pessoa jurídica, é público por natureza e não é dado pessoal.
     */
    fun mascarar(valor: String?): String {
        val d = normalizar(valor)
        if (d.length !in comprimento) return d
        return when (this) {
            CPF -> "###.###.${d.substring(6, 9)}-${d.substring(9, 11)}"
            CNPJ -> formatar(d)
            RG -> d.replaceRange(1, 4, "###")
            CNH -> d.replaceRange(2, 8, "######")
            PASSAPORTE -> formatar(d).replaceRange(4, 7, "###")
        }
    }

    /** Atalho de exibição: [mascarar] quando [ocultar], senão [formatar]. */
    fun exibir(valor: String?, ocultar: Boolean = false): String =
        if (ocultar) mascarar(valor) else formatar(valor)

    /**
     * Validação do documento. `CPF` e `CNPJ` têm **dígito verificador** e são checados de verdade; `RG`,
     * `CNH` e `PASSAPORTE` não têm regra nacional única, então valem pelo comprimento.
     *
     * Este é o lugar que não existia: até aqui o campo `documento` era texto livre — nenhum dos três
     * `when` do código antigo validava coisa alguma, só decoravam.
     */
    fun validar(valor: String?): Boolean {
        val d = normalizar(valor)
        if (d.length !in comprimento) return false
        return when (this) {
            CPF -> validarCpf(d)
            CNPJ -> validarCnpj(d)
            RG, CNH, PASSAPORTE -> true
        }
    }

    private fun agrupar(digitos: String, tamanhos: List<Int>, separadores: List<Char>): String {
        val saida = StringBuilder()
        var posicao = 0
        for ((indice, tamanho) in tamanhos.withIndex()) {
            if (posicao >= digitos.length) break
            if (indice > 0) saida.append(separadores[indice - 1])
            saida.append(digitos, posicao, minOf(posicao + tamanho, digitos.length))
            posicao += tamanho
        }
        return saida.toString()
    }

    companion object {
        /**
         * Converte o valor persistido (String) no enum canônico; `null` se desconhecido — **fail-closed**,
         * como [dev.matheus.fluviapp.domain.passagem.StatusPassagem] e
         * [dev.matheus.fluviapp.domain.passagem.TipoPassagem]. Tolerante à grafia legada.
         */
        fun de(valor: String?): TipoDocumento? {
            val normalizado = valor?.trim()?.uppercase()?.replace(" ", "_") ?: return null
            return entries.firstOrNull { it.name == normalizado }
        }

        private fun validarCpf(d: String): Boolean {
            if (d.all { it == d[0] }) return false
            val dv1 = digitoVerificador(d.take(9), pesoInicial = 10)
            val dv2 = digitoVerificador(d.take(10), pesoInicial = 11)
            return d[9] == dv1 && d[10] == dv2
        }

        private fun validarCnpj(d: String): Boolean {
            if (d.all { it == d[0] }) return false
            val pesos1 = listOf(5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
            val pesos2 = listOf(6) + pesos1
            val dv1 = digitoPorPesos(d.take(12), pesos1)
            val dv2 = digitoPorPesos(d.take(13), pesos2)
            return d[12] == dv1 && d[13] == dv2
        }

        /** DV do CPF: soma ponderada com pesos decrescentes a partir de [pesoInicial]. */
        private fun digitoVerificador(base: String, pesoInicial: Int): Char {
            val soma = base.foldIndexed(0) { i, acc, c -> acc + (c - '0') * (pesoInicial - i) }
            val resto = soma % 11
            return if (resto < 2) '0' else ('0' + (11 - resto))
        }

        private fun digitoPorPesos(base: String, pesos: List<Int>): Char {
            val soma = base.foldIndexed(0) { i, acc, c -> acc + (c - '0') * pesos[i] }
            val resto = soma % 11
            return if (resto < 2) '0' else ('0' + (11 - resto))
        }
    }
}