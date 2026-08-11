package dev.matheus.fluviapp.domain.passagem

import dev.matheus.fluviapp.database.PassagemEntity

/**
 * Resultado da confirmação de embarque (ADR-0012). A UI reage a cada caso: sucesso, reuso barrado
 * (idempotência), bilhete não emitido (guarda da FSM) ou id inexistente no servidor.
 */
sealed interface ResultadoEmbarque {
    /** Transição EMITIDA→EMBARCADA aplicada; carrega a passagem já carimbada. */
    data class Confirmada(val passagem: PassagemEntity) : ResultadoEmbarque

    /** Bilhete já estava EMBARCADA — reuso barrado (antifraude). Mostra quem/quando embarcou. */
    data class JaEmbarcada(val por: String, val em: String) : ResultadoEmbarque

    /** Ainda A_EMITIR (ou status desconhecido): não pode embarcar. */
    data object NaoEmitida : ResultadoEmbarque

    /** Nenhum documento com esse id no Firestore (QR inválido/estranho ao sistema). */
    data object NaoEncontrada : ResultadoEmbarque
}
