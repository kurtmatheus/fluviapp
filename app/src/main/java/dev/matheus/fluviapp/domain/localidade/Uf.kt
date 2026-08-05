package dev.matheus.fluviapp.domain.localidade

/**
 * Unidade federativa como tipo de domínio ([ADR-0020 D6]), no lugar de um dos dois `Catalogo` que o
 * [ADR-0016] §5 embutia dentro da `Localidade`.
 *
 * É o conjunto mais estável do domínio: **27 unidades, fechadas por constituição**. Tratá-lo como linha de
 * catálogo cadastrável dava ao administrador o poder de inventar uma UF e de escrever "Pará" de duas
 * formas — e foi para impedir a segunda que o §5 precisou criar a unicidade `(categoria, descricao)`, uma
 * regra que existia só para compensar a tabela genérica. Sem ela, a duplicidade é impossível por
 * construção.
 *
 * O par desta decisão: `municipio` deixa de ser `Catalogo` e passa a ser campo da própria `Localidade` —
 * não é rótulo de outra tabela, é *o nome daquela entidade*, e a autoridade sobre ele é o IBGE
 * (`codigoIbge`), não o gestor.
 *
 * O `name` é a **sigla** e é o valor canônico persistido.
 */
enum class Uf(val nome: String, val codigo: String) {
    AC("Acre", "12"),
    AL("Alagoas", "27"),
    AP("Amapá", "16"),
    AM("Amazonas", "13"),
    BA("Bahia", "29"),
    CE("Ceará", "23"),
    DF("Distrito Federal", "53"),
    ES("Espírito Santo", "32"),
    GO("Goiás", "52"),
    MA("Maranhão", "21"),
    MT("Mato Grosso", "51"),
    MS("Mato Grosso do Sul", "50"),
    MG("Minas Gerais", "31"),
    PA("Pará", "15"),
    PB("Paraíba", "25"),
    PR("Paraná", "41"),
    PE("Pernambuco", "26"),
    PI("Piauí", "22"),
    RJ("Rio de Janeiro", "33"),
    RN("Rio Grande do Norte", "24"),
    RS("Rio Grande do Sul", "43"),
    RO("Rondônia", "11"),
    RR("Roraima", "14"),
    SC("Santa Catarina", "42"),
    SP("São Paulo", "35"),
    SE("Sergipe", "28"),
    TO("Tocantins", "17");

    /** A sigla — valor canônico e o que aparece em tela. */
    val sigla: String get() = name

    /** Exibição por extenso com a sigla, para seletor: "Pará (PA)". */
    fun rotulo(): String = "$nome ($sigla)"

    companion object {
        /**
         * Fronteira String→enum; `null` se desconhecida (fail-closed). Aceita a **sigla** ("PA", "pa") e
         * também o **nome por extenso** ("Pará"), porque é assim que o dado nasceu no catálogo.
         */
        fun de(valor: String?): Uf? {
            val bruto = valor?.trim() ?: return null
            if (bruto.isEmpty()) return null
            return entries.firstOrNull { it.name == bruto.uppercase() }
                ?: entries.firstOrNull { it.nome.equals(bruto, ignoreCase = true) }
        }

        /**
         * A UF pelo **código numérico do IBGE** (os dois primeiros dígitos do código de município).
         *
         * É o que torna a verificação do `codigoIbge` possível **sem rede e sem tabela**: `1501402` começa
         * em `15`, e `15` é o Pará. Um código de outro estado deixa de ser um erro que só aparece no
         * relatório e passa a ser um erro que a tela recusa na hora.
         */
        fun porCodigo(codigo: String?): Uf? =
            entries.firstOrNull { it.codigo == codigo?.trim() }

        /**
         * Fronteira de **tela**: o dropdown mostra [rotulo] ("Pará (PA)") e devolve o texto escolhido.
         *
         * Separada de [de] pela mesma razão que em `TipoEmbarcacao`: aquela lê o que ficou **gravado**
         * (a sigla, estável), esta lê o que a **pessoa** escolheu. A forma de exibição pode mudar sem
         * migrar dado — e foi um teste de ViewModel que cobrou esta função, ao passar `"Pará (PA)"` para
         * um `de()` que só entendia `"PA"`.
         */
        fun porRotulo(rotulo: String?): Uf? {
            val bruto = rotulo?.trim() ?: return null
            return entries.firstOrNull { it.rotulo().equals(bruto, ignoreCase = true) }
        }
    }
}