package dev.matheus.fluviapp.domain.viagem

import dev.matheus.fluviapp.domain.IObjetoSimplificado

/**
 * O **ativo** que transporta: o que navega, com capacidade e dono (ADR-0016 §4).
 *
 * ### Por que deixou de se chamar `Embarcacao`
 *
 * Porque nem tudo que navega é embarcacao. O domínio já reconhecia isso em [TipoEmbarcacao] — ferry-boat,
 * embarcacao e lancha —, e chamar a entidade de "embarcacao" obrigava a dizer *"o embarcacao é do tipo lancha"*, que é a
 * frase que denuncia um nome errado. **Embarcação é o gênero; embarcacao é uma das espécies.** O rename estava
 * previsto desde o ADR-0020 e foi adiado até a seção ser revitalizada.
 *
 * A seção do menu chama-se **Flotilha**: o conjunto das embarcações.
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
    val capacidadeVeiculo: Int,
    val capacidadeSuite2: Int,
    val capacidadeSuite3: Int,
    val capacidadeCamarote: Int,
    val empresaId: String,
) : IObjetoSimplificado