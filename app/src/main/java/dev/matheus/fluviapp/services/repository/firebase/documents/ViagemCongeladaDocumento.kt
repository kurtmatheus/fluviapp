package dev.matheus.fluviapp.services.repository.firebase.documents

/**
 * **O snapshot da viagem congelado dentro da Passagem** (ADR-0008) — o valor por cópia que o bilhete
 * imprime, não a entidade viva.
 *
 * Ele se chamava `ViagemDocumento` até a F8.1, e o rename é o que **libera o nome** para o documento da
 * Viagem nova. A troca é invisível no Firestore: o `toObject` mapeia por **nome de campo**, e o campo
 * continua sendo `PassagemDocumento.viagem` — nenhum documento gravado muda de forma.
 *
 * Os campos ficam como estão, com os nomes que os documentos já gravados usam. Reescrevê-los agora
 * quebraria a leitura do histórico sem entregar nada — quem decide a forma do snapshot novo é a **F9**,
 * que é onde a emissão volta a montá-lo, e ele será outro: a partida física não tem `codigo`, e origem e
 * destino saem dos portos da rota.
 *
 * [tarifas] é o resíduo mais visível disso: a tabela cadastrada morreu no ADR-0016 §7.2 (a base passa a
 * ser *inferida* por agregação), mas os bilhetes antigos a carregam, e ler o que está gravado é a razão
 * de este arquivo existir.
 */
data class ViagemCongeladaDocumento(
    val codigo: String = "",
    val empresa: String = "",
    val embarcacao: String = "",
    val origem: String = "",
    val destino: String = "",
    val empresaId: String = "",
    val embarcacaoId: String = "",
    val tarifas: Map<String, Double> = emptyMap(),
)