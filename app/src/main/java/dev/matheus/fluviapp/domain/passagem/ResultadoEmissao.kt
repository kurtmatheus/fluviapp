package dev.matheus.fluviapp.domain.passagem

/**
 * Resultado das **guardas de emissão** da passagem (ADR-0013 Fase 2b). Fail-closed: só [Ok] libera o
 * salvamento; os demais casos bloqueiam a emissão com um motivo tipado que a UI traduz em mensagem.
 */
sealed interface ResultadoEmissao {
    /** Pode emitir. */
    data object Ok : ResultadoEmissao

    /**
     * Não há tarifa tabelada para a chave escolhida (acomodação/classe) — sem base para medir.
     *
     * **REVITALIZAÇÃO (F9.1): condenado.** Desde que **preço é I/O** (decisão de 2026-08-11), a emissão não
     * calcula valor — o operador informa o praticado —, então **não há preço a faltar**. O ADR-0016 §7.2 já o
     * condenava por outra via: a tabela de tarifa que ele media morreu ali.
     *
     * **Por que ainda existe.** Quem o produz é o `FormPassagemHelper.validarEmissao` e quem o consome é o
     * `FormPassagemViewModel`; os dois são reescritos na **F9.4** (camada + orquestração), e é lá que este caso
     * é apagado. Removê-lo agora exigiria reescrever o fluxo de emissão fora da fatia dele.
     */
    data object SemTarifa : ResultadoEmissao

    /** A cota de gratuidade da categoria já foi atingida nesta viagem (máx. 2 por categoria). */
    data class CotaGratuidadeAtingida(val categoria: String) : ResultadoEmissao
}