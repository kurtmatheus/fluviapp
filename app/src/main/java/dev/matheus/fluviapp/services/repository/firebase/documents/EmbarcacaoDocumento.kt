package dev.matheus.fluviapp.services.repository.firebase.documents

import dev.matheus.fluviapp.domain.viagem.Embarcacao
import dev.matheus.fluviapp.domain.viagem.TipoEmbarcacao
import dev.matheus.fluviapp.services.repository.firebase.DocumentoBruto

/**
 * A forma do documento em `embarcacoes/{id}` — **documentação, não caminho** (ADR-0019 D2). A leitura e a
 * escrita passam direto por `Map`, abaixo; esta data class fica como registro de quais campos existem.
 */
data class EmbarcacaoDocumento(
    val nome: String = "",
    /**
     * `name` do [TipoEmbarcacao] — o **estável**, não o rótulo de tela: renomear "Ferry Boat" na interface
     * não pode obrigar a reescrever documento. Obrigatório; ausente ou desconhecido invalida o documento.
     */
    val tipo: String = "",
    val capacidadeVeiculo: Int = 0,
    val capacidadeSuite2: Int = 0,
    val capacidadeSuite3: Int = 0,
    val capacidadeCamarote: Int = 0,
    /** Vínculo N-1 com Empresa por id (ADR-0008, Fase 3). */
    val empresaId: String = "",
)

/**
 * `DocumentoBruto` → domínio, **sem passar pelo DTO** (ADR-0019 D2). Devolve `null` quando o documento
 * **não é uma embarcação**.
 *
 * ### Por que esta é a única leitura que pode devolver `null`
 *
 * Porque a Embarcação é a primeira entidade com um **invariante** na fronteira: não existe embarcação sem
 * tipo. Os outros campos têm fail-closed por valor — capacidade ausente vira `0`, e `0` é uma resposta
 * honesta (*não vende lugar*). O tipo não tem esse valor neutro: qualquer padrão que se escolhesse seria
 * uma **afirmação inventada** sobre o que a embarcação transporta, e é dela que dependem o modo veículo da
 * emissão e a classe admitida. Entre inventar e omitir, omite-se.
 *
 * Descartar o documento **não derruba a coleção**: `sincronizarColecao` usa `mapNotNull`, então uma linha
 * estragada tira a si mesma da lista e as outras seguem. E o descarte é **visível** sem telemetria nova —
 * o registro de sincronização já anota `snapshotRecebido(n)` e `gravado(m)`, e `m < n` é exatamente o
 * número de documentos recusados.
 *
 * O que ele não faz é consertar: um documento assim some da Flotilha e não é editável pelo app. É o mesmo
 * princípio das regras do servidor sobre o `delete` — a limpeza de documento inválido vive **fora** do
 * app, no console (ADR-0021 D0).
 *
 * As capacidades usam `inteiro()`, que coage `Number` → `Int`: o Firestore devolve inteiro como `Long`, e
 * ler direto como `Int` estouraria.
 */
fun DocumentoBruto.toEmbarcacao(): Embarcacao? {
    val tipo = TipoEmbarcacao.de(texto("tipo")) ?: return null

    return Embarcacao(
        id = id,
        descricaoNome = texto("nome"),
        tipo = tipo,
        capacidadeVeiculo = inteiro("capacidadeVeiculo"),
        capacidadeSuite2 = inteiro("capacidadeSuite2"),
        capacidadeSuite3 = inteiro("capacidadeSuite3"),
        capacidadeCamarote = inteiro("capacidadeCamarote"),
        empresaId = texto("empresaId"),
    )
}

/**
 * Domínio → `Map`, que é o que o Firestore grava. O `id` **não entra no mapa**: ele é o nome do
 * documento, não um campo dele — duplicá-lo criaria duas fontes para a mesma identidade.
 *
 * O tipo grava o `name` do enum, e não o rótulo: o dado fica preso ao vocabulário do domínio, não ao texto
 * da tela.
 */
fun Embarcacao.paraMapa(): Map<String, Any?> = mapOf(
    "nome" to descricaoNome,
    "tipo" to tipo.name,
    "capacidadeVeiculo" to capacidadeVeiculo,
    "capacidadeSuite2" to capacidadeSuite2,
    "capacidadeSuite3" to capacidadeSuite3,
    "capacidadeCamarote" to capacidadeCamarote,
    "empresaId" to empresaId,
)