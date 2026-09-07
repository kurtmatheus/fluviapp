package dev.matheus.fluviapp.sampledata

import dev.matheus.fluviapp.domain.screendata.DadosImpressora

/**
 * Dado de amostra para `@Preview` — **o que sobrou**.
 *
 * O arquivo tinha dezessete listas e ficou com uma. As dezesseis que saíram em 2026-09-07 não tinham
 * leitor: eram amostras de `Constante` (o catálogo genérico que o ADR-0020 D1 matou), de `Usuario` por
 * papel, de `Empresa` e de `Embarcacao` — desenhadas para telas que desde então passaram a receber estado
 * pronto do ViewModel, e a montar a própria amostra dentro do `@Preview`.
 *
 * A décima sétima, a de forma de pagamento, tinha leitor e mesmo assim saiu: os dois `@Preview` que a
 * usavam passaram a ler `FormaPagamento.entries`, que é o dono do vocabulário desde o ADR-0018 D11. Uma
 * amostra que repete um enum não é amostra — é uma segunda lista para manter sincronizada.
 *
 * O que fica é o que **não** tem dono no domínio: a impressora pareada vem do Bluetooth do aparelho, e
 * não há como um `@Preview` descobri-la.
 */
val listaDadosImpressoraSample = listOf(
    DadosImpressora(
        nome = "IMPRESSORA MODELO",
        endereco = "00:11:22:33:44:55"
    ),
    DadosImpressora(
        nome = "IMPRESSORA MODELO",
        endereco = "00:11:22:33:44:55"
    ),
    DadosImpressora(
        nome = "IMPRESSORA MODELO",
        endereco = "00:11:22:33:44:55"
    )
)
