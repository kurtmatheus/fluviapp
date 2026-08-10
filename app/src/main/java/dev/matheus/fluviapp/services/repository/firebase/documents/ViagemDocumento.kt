package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.domain.viagem.Viagem
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto
import java.time.DayOfWeek

/**
 * A forma do documento em `viagens/{id}` — **documentação, não caminho** (ADR-0019 D2).
 *
 * O nome se libertou na F8.1: quem o ocupava era o snapshot congelado da Passagem, hoje
 * [ViagemCongeladaDocumento]. Os dois nunca foram a mesma coisa, e agora nem parecem.
 */
data class ViagemDocumento(
    val rotaId: String = "",
    val embarcacaoId: String = "",
    /** O `name` do `DayOfWeek` — `"TUESDAY"`. Ver a nota de [toViagem] sobre por que não é o número. */
    val diaSemana: String = "",
    /** Minutos desde a meia-noite. Número porque é sobre ele que se faz conta (ADR-0016 §7.1). */
    val horaMin: Int = 0,
    /** Num pool sem dono, a assinatura é o que resta de responsabilidade (§7.1). */
    val criadoPor: String = "",
    val criadoEm: String = "",
    /** Delete lógico: viagem fora de uso some dos seletores e continua resolvendo o que aponta para ela. */
    val ativo: Boolean = true,
)

/**
 * `DocumentoBruto` → domínio. Devolve `null` quando **não é uma viagem**, e são três causas:
 *
 * - **sem rota**: uma partida que não percorre nada;
 * - **sem embarcação**: uma partida sem em quê;
 * - **sem dia conhecido**: `diaSemana` é invariante, não campo opcional — o precedente é
 *   `Embarcacao.tipo` (decisão do analista, 2026-08-05). Documento com dia ausente ou ilegível some da
 *   lista em vez de derrubar a coleção inteira.
 *
 * A **hora não é causa de recusa**, e a assimetria é a mesma da Rota com origem=destino: hora fora do
 * relógio é dado ruim, não "não é uma viagem", e escondê-la esconderia justamente o que alguém precisa
 * inativar. Quem cobra a faixa é `Viagem.temSentido()`, no cadastro.
 *
 * O dia é gravado como **`name`** (`"TUESDAY"`), e não como o número ISO, por duas razões: é o que se lê
 * no console do Firestore sem tabela de conversão, e é a convenção que `Papel`, `Cargo`, `Atuacao` e
 * `TipoEmbarcacao` já seguem. `DayOfWeek.valueOf` lança em texto desconhecido — daí o `runCatching`, que
 * transforma "dia estranho" na mesma recusa dos outros dois casos.
 */
fun DocumentoBruto.toViagem(): Viagem? {
    val rotaId = texto("rotaId")
    val embarcacaoId = texto("embarcacaoId")
    if (rotaId.isBlank() || embarcacaoId.isBlank()) return null

    val dia = runCatching { DayOfWeek.valueOf(texto("diaSemana")) }.getOrNull() ?: return null

    return Viagem(
        id = id,
        rotaId = rotaId,
        embarcacaoId = embarcacaoId,
        diaSemana = dia,
        horaMin = inteiro("horaMin"),
        criadoPor = texto("criadoPor"),
        criadoEm = texto("criadoEm"),
        ativo = booleano("ativo", padrao = true),
    )
}

/** Domínio → `Map`. O `id` não entra: ele é o nome do documento. */
fun Viagem.paraMapa(): Map<String, Any?> = mapOf(
    "rotaId" to rotaId,
    "embarcacaoId" to embarcacaoId,
    "diaSemana" to diaSemana.name,
    "horaMin" to horaMin,
    "criadoPor" to criadoPor,
    "criadoEm" to criadoEm,
    "ativo" to ativo,
)