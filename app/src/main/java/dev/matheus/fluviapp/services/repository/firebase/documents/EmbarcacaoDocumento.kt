package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.domain.viagem.Embarcacao
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto

/**
 * A forma do documento em `embarcacoes/{id}` — **documentação, não caminho** (ADR-0019 D2). A leitura e a
 * escrita passam direto por `Map`, abaixo; esta data class fica como registro de quais campos existem.
 */
data class EmbarcacaoDocumento(
    val nome: String = "",
    val capacidadeVeiculo: Int = 0,
    val capacidadeSuite2: Int = 0,
    val capacidadeSuite3: Int = 0,
    val capacidadeCamarote: Int = 0,
    /** Vínculo N-1 com Empresa por id (ADR-0008, Fase 3). */
    val empresaId: String = "",
)

/**
 * `DocumentoBruto` → domínio, **sem passar pelo DTO** (ADR-0019 D2).
 *
 * As capacidades usam `inteiro()`, que coage `Number` → `Int`: o Firestore devolve inteiro como `Long`, e
 * ler direto como `Int` estouraria. Ausente vira `0` — capacidade desconhecida é capacidade nenhuma, que
 * é o fail-closed certo aqui: embarcação sem capacidade declarada não vende lugar.
 */
fun DocumentoBruto.toEmbarcacao() = Embarcacao(
    id = id,
    descricaoNome = texto("nome"),
    capacidadeVeiculo = inteiro("capacidadeVeiculo"),
    capacidadeSuite2 = inteiro("capacidadeSuite2"),
    capacidadeSuite3 = inteiro("capacidadeSuite3"),
    capacidadeCamarote = inteiro("capacidadeCamarote"),
    empresaId = texto("empresaId"),
)

/**
 * Domínio → `Map`, que é o que o Firestore grava. O `id` **não entra no mapa**: ele é o nome do
 * documento, não um campo dele — duplicá-lo criaria duas fontes para a mesma identidade.
 */
fun Embarcacao.paraMapa(): Map<String, Any?> = mapOf(
    "nome" to descricaoNome,
    "capacidadeVeiculo" to capacidadeVeiculo,
    "capacidadeSuite2" to capacidadeSuite2,
    "capacidadeSuite3" to capacidadeSuite3,
    "capacidadeCamarote" to capacidadeCamarote,
    "empresaId" to empresaId,
)