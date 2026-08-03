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
enum class Uf(val nome: String) {
    AC("Acre"),
    AL("Alagoas"),
    AP("Amapá"),
    AM("Amazonas"),
    BA("Bahia"),
    CE("Ceará"),
    DF("Distrito Federal"),
    ES("Espírito Santo"),
    GO("Goiás"),
    MA("Maranhão"),
    MT("Mato Grosso"),
    MS("Mato Grosso do Sul"),
    MG("Minas Gerais"),
    PA("Pará"),
    PB("Paraíba"),
    PR("Paraná"),
    PE("Pernambuco"),
    PI("Piauí"),
    RJ("Rio de Janeiro"),
    RN("Rio Grande do Norte"),
    RS("Rio Grande do Sul"),
    RO("Rondônia"),
    RR("Roraima"),
    SC("Santa Catarina"),
    SP("São Paulo"),
    SE("Sergipe"),
    TO("Tocantins");

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
    }
}