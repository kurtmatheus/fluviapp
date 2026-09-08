package dev.matheus.fluviapp.domain.passagem

/**
 * Resultado da confirmação de embarque (ADR-0012). A UI reage a cada caso: sucesso, reuso barrado
 * (idempotência), bilhete não emitido (guarda da FSM) ou id inexistente no servidor.
 */
sealed interface ResultadoEmbarque {
    /** Transição EMITIDA→EMBARCADA aplicada; carrega a passagem já carimbada. */
    data class Confirmada(val passagem: Passagem) : ResultadoEmbarque

    /**
     * Bilhete já estava EMBARCADA — reuso barrado (antifraude).
     *
     * Carrega o [CarimboEmbarque] inteiro, e não um par de textos soltos: quem embarcou é um **uid**, e o nome
     * exibido se resolve por referência ([ADR-0023] D8). Antes eram duas `String`, uma delas já formatada em
     * `dd/MM/yyyy HH:mm` — o formato que não ordena e que o [ADR-0024] D2 corrigiu.
     */
    data class JaEmbarcada(val carimbo: CarimboEmbarque) : ResultadoEmbarque

    /** Ainda A_EMITIR, cancelada (ou status que não alcança o embarque): não pode embarcar. */
    data object NaoEmitida : ResultadoEmbarque

    /** Nenhum documento com esse id no Firestore (QR inválido/estranho ao sistema). */
    data object NaoEncontrada : ResultadoEmbarque

    /**
     * **Quem escaneou não pode validar** ([ADR-0032] D1) — sessão ausente ou papel que a política não
     * reconhece.
     *
     * Nasce em 2026-09-07 com o freio do cliente, e corrige uma resposta que **mentia sobre a causa**:
     * sem sessão, o embarque devolvia [NaoEncontrada], que é o desfecho de *QR estranho ao sistema*.
     * Quem estivesse na doca leria "bilhete inválido" para um bilhete perfeitamente válido.
     *
     * O servidor recusaria de qualquer forma (`papelConhecido()` na regra), e o que o cliente acrescenta
     * é o que a regra não faz: **falhar antes** e **falhar dizendo o motivo certo**.
     */
    data object SemPermissao : ResultadoEmbarque
}