package dev.matheus.fluviapp.exceptions

/**
 * Falhas da emissão de passagem, agrupadas por motivo (ADR-0004). Cada motivo carrega um
 * [evento] estável para virar dimensão navegável na telemetria.
 */
sealed class EmissaoException(
    mensagem: String,
    val evento: String,
    causa: Throwable? = null,
) : Exception(mensagem, causa) {

    /** Room falhou / mapper falhou ao montar o documento. */
    class FalhaAoPersistir(causa: Throwable) :
        EmissaoException("Falha ao persistir passagem: ${causa.message}", "passagem_persistencia", causa)

    /** Firestore rejeitou a escrita (não é offline — offline apenas enfileira). */
    class FalhaNaTransmissao(causa: Throwable) :
        EmissaoException("Falha na transmissão ao Firestore: ${causa.message}", "passagem_transmissao", causa)

    /** Impressão física/digital falhou. */
    class FalhaNaImpressao(causa: Throwable) :
        EmissaoException("Falha na impressão: ${causa.message}", "passagem_impressao", causa)

    /** Número de bilhete indisponível/inválido (ex.: contador não numérico). */
    class NumeroIndisponivel(motivo: String) :
        EmissaoException("Número de bilhete indisponível: $motivo", "passagem_numero")
}