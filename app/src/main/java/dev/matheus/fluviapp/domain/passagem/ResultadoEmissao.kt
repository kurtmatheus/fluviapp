package dev.matheus.fluviapp.domain.passagem

/**
 * Resultado das **guardas de emissão** da passagem. Fail-closed: só [Ok] libera a emissão; os demais casos a
 * bloqueiam com um motivo tipado que a apresentação traduz em mensagem.
 *
 * ### O `SemTarifa` morreu aqui (F9.4), como a F9.1 previu
 *
 * Ele dizia *"não há tarifa tabelada para a acomodação/classe escolhida"*, e deixou de fazer sentido quando
 * **preço virou I/O** (2026-08-11): a emissão não calcula valor — o operador informa o praticado —, então não
 * há preço a faltar. Ele sobreviveu à F9.1 porque quem o produzia e quem o consumia eram o helper e o
 * ViewModel da emissão; os dois foram reescritos agora, e o caso saiu com eles.
 *
 * ### Por que as guardas não são pendências do agregado
 *
 * `PassagemDePassageiro.pendencias()` responde *"esta passagem é coerente consigo mesma?"* — pergunta que se
 * responde olhando só para ela. Estas guardas dependem do **mundo**: quantas gratuidades já saíram naquela
 * saída, o que exige consulta. Misturá-las faria o agregado precisar de I/O para se validar.
 */
sealed interface ResultadoEmissao {
    /** Pode emitir. */
    data object Ok : ResultadoEmissao

    /**
     * A cota de gratuidade daquela categoria já foi atingida **nesta saída** ([ADR-0013] §8: máx. 2).
     *
     * **Por ocorrência, e não por viagem semanal** — a atualização de vocabulário que o ADR-0016 §7.1 impõe
     * ao §8 do ADR-0013: a cota é assento livre de **uma travessia**, como a ocupação e a numeração. Contá-la
     * por viagem faria duas terças diferentes disputarem as mesmas duas vagas.
     */
    data class CotaGratuidadeAtingida(val gratuidade: TipoGratuidade) : ResultadoEmissao

    /** O agregado não fecha: falta titular, excede a ocupação, gratuidade sem subtipo… */
    data class Incoerente(val pendencias: Set<PassagemDePassageiro.Pendencia>) : ResultadoEmissao
}