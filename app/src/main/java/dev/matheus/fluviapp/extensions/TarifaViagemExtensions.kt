package dev.matheus.fluviapp.extensions

import dev.matheus.fluviapp.domain.viagem.TarifaViagem
import dev.matheus.fluviapp.services.repository.firebase.documents.ViagemDocumento

/**
 * Achatamento **mapa↔linhas** da tabela de tarifas da Viagem (ADR-0013). No Firestore a tabela vive como
 * **mapa aninhado** no [ViagemDocumento] (forma natural do DTO); no Room, como **linhas** [TarifaViagem]
 * (forma normalizada — o lado "SQL" do ADR-0003, p/ o balanço agregar por viagem). Estas duas funções são
 * a costura entre as formas, mantida pura e JVM-testável.
 */

/** Mapa do doc → linhas do Room, atando cada célula ao [viagemId]. */
fun ViagemDocumento.tarifasParaLinhas(viagemId: String): List<TarifaViagem> =
    tarifas.map { (chave, valor) -> TarifaViagem(viagemId, chave, valor) }

/** Linhas do Room → mapa do doc (chave→valor), para montar o [ViagemDocumento] na escrita. */
fun List<TarifaViagem>.paraMapaTarifas(): Map<String, Double> =
    associate { it.chave to it.valor }