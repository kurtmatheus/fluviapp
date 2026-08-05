package dev.matheus.fluviapp.domain.viagem

import dev.matheus.fluviapp.domain.IObjetoSimplificado

/**
 * O **ativo** que transporta: o que navega, com tipo, capacidade e dono (ADR-0016 §4).
 *
 * ### Por que deixou de se chamar `Navio`
 *
 * Porque nem tudo que navega é navio. O domínio já reconhecia isso em [TipoEmbarcacao] — ferry-boat,
 * navio e lancha —, e chamar a entidade de "navio" obrigava a dizer *"o navio é do tipo lancha"*, que é a
 * frase que denuncia um nome errado. **Embarcação é o gênero; navio é uma das espécies.** O rename estava
 * previsto desde o ADR-0020 e esperava a seção ser revitalizada.
 *
 * A seção do menu chama-se **Flotilha**: o conjunto das embarcações.
 *
 * ### Não existe embarcação sem tipo
 *
 * [tipo] **não é nulo** — é invariante, não campo opcional. Uma embarcação sem tipo não é uma embarcação
 * incompleta: é uma coisa da qual não se sabe dizer o que transporta, e o resto do domínio já pergunta
 * isso (o modo veículo da emissão, a classe admitida, a tarifa). Deixá-lo nulo empurraria a pergunta *"e
 * se não tiver?"* para **todo** chamador, e cada um responderia à sua maneira.
 *
 * O invariante custa nas duas pontas, e é onde ele se paga: o formulário **exige** o tipo antes de salvar,
 * e a leitura do Firestore **descarta** o documento que não declara um tipo conhecido, em vez de inventar
 * um padrão (`EmbarcacaoDocumento.toEmbarcacao`). Um documento assim não vira embarcação meia-boca — ele
 * simplesmente não é uma.
 *
 * O tipo mora aqui, e não na viagem, porque é atributo do **ativo**: uma lancha não deixa de ser lancha na
 * travessia seguinte. Guardá-lo na viagem obrigaria a repetir a mesma verdade em cada uma delas — e a
 * repetição é o que permite duas responderem diferente sobre a mesma embarcação.
 *
 * ### Livre de framework
 *
 * Como a [Empresa] (ADR-0017 D1, ADR-0019 D2): saiu o `@Entity` do Room e saiu o import do documento —
 * o domínio não conhece a forma que o Firestore grava. O vínculo com a Empresa é por **id estável**
 * ([empresaId], ADR-0008); o nome de exibição resolve-se na leitura contra a lista de empresas, e não se
 * congela aqui.
 */
data class Embarcacao(
    override val id: String,
    override val descricaoNome: String,
    val tipo: TipoEmbarcacao,
    val capacidadeVeiculo: Int,
    val capacidadeSuite2: Int,
    val capacidadeSuite3: Int,
    val capacidadeCamarote: Int,
    val empresaId: String,
) : IObjetoSimplificado