package dev.matheus.fluviapp.model.passagem

/**
 * Resultado das **guardas de emissão** da passagem (ADR-0013 Fase 2b). Fail-closed: só [Ok] libera o
 * salvamento; os demais casos bloqueiam a emissão com um motivo tipado que a UI traduz em mensagem.
 */
sealed interface ResultadoEmissao {
    /** Pode emitir. */
    data object Ok : ResultadoEmissao

    /** Não há tarifa tabelada para a chave escolhida (acomodação/classe) — sem base para medir. */
    data object SemTarifa : ResultadoEmissao

    /** A cota de gratuidade da categoria já foi atingida nesta viagem (máx. 2 por categoria). */
    data class CotaGratuidadeAtingida(val categoria: String) : ResultadoEmissao
}