package dev.matheus.fluviapp.services.repository.firebase.documents

/**
 * **O snapshot da viagem congelado dentro da Passagem** — e, desde a F8.0, *só* isso.
 *
 * Ele sobreviveu à demolição do trecho disfarçado por não ser espelho de entidade nenhuma: é o valor por
 * cópia que o bilhete imprime (ADR-0008), e um bilhete emitido não muda de significado porque a entidade
 * de origem morreu. Por isso não há mais `toViagem(id)`: não existe entidade viva com esta forma para
 * onde voltar, e a `Viagem` da F8 é outra coisa (`rotaId`, `embarcacaoId`, dia e hora).
 *
 * Os campos ficam como estão, com os nomes que os documentos já gravados usam. Rescrevê-los agora
 * quebraria a leitura do histórico sem entregar nada — quem decide a forma do snapshot novo é a **F9**,
 * que é onde a emissão volta a montá-lo.
 *
 * [tarifas] é o resíduo mais visível disso: a tabela cadastrada morreu no ADR-0016 §7.2 (a base passa a
 * ser *inferida* por agregação), mas os bilhetes antigos a carregam, e ler o que está gravado é a razão
 * de este arquivo existir.
 */
data class ViagemDocumento(
    val codigo: String = "",
    val empresa: String = "",
    val embarcacao: String = "",
    val origem: String = "",
    val destino: String = "",
    val empresaId: String = "",
    val embarcacaoId: String = "",
    val tarifas: Map<String, Double> = emptyMap(),
)