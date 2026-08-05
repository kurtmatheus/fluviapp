package dev.matheus.fluviapp.domain.localidade

/**
 * **Onde** — o par UF + município como uma coisa só (ADR-0016 §5, 7ª rodada). É capacidade da
 * **plataforma**: mora na raiz, não pertence a empresa nenhuma, e é sobre ela que os portos se apoiam.
 *
 * ### O que mudou em relação ao ADR-0016 §5
 *
 * O §5 desenhava `uf` e `municipio` como dois `Catalogo { id, descricao }` **embutidos**. O `Catalogo` não
 * nasceu (ADR-0020 D1), e cada um seguiu o seu caminho:
 *
 * - [uf] é o tipo [Uf] — 27 valores fechados por constituição, e por isso a unicidade que o §5 precisava
 *   inventar (`(categoria, descricao)`) deixa de existir: escrever "Pará" de duas formas é impossível por
 *   construção;
 * - [municipio] é **campo de texto desta entidade**, e não rótulo de outra tabela. É *o nome dela*.
 *
 * ### Por que o código do IBGE é obrigatório
 *
 * Porque é a **chave natural** da dimensão (decisão do analista). Ele resolve a unicidade de graça — dois
 * cadastros do mesmo município colidem no código antes de colidirem na grafia — e ancora a análise: sem
 * ele, "passagens por município" agrupa por igualdade de texto, e um acento a menos vira duas linhas no
 * relatório. Os **dois primeiros dígitos são o código da UF**, o que dá uma verificação cruzada local, sem
 * rede: código de outro estado não casa com a UF escolhida.
 *
 * ### Delete lógico
 *
 * [ativo] existe porque localidade é **referenciada** (o porto aponta para ela, e a rota aponta para o
 * porto): apagar o documento deixaria histórico apontando para o nada. Inativar preserva a referência e
 * tira a linha das listas — o passado continua legível, o futuro não pode escolhê-la.
 *
 * A regra que decorre disso, e que o repositório documenta: **quem lista filtra por `ativo`; quem resolve
 * por id, não.**
 */
data class Localidade(
    val id: String,
    val municipio: String,
    val uf: Uf,
    /** 7 dígitos do IBGE; os dois primeiros são o código da [uf]. */
    val codigoIbge: String,
    val ativo: Boolean = true,
) {

    /** Exibição canônica: "Belém/PA". */
    val rotulo: String get() = "$municipio/${uf.sigla}"
}