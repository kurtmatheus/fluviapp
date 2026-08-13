package dev.matheus.fluviapp.domain.passagem

/**
 * As guardas que decidem se **esta** passagem pode ser emitida — puras, e é isso que as torna verificáveis.
 *
 * A separação em relação a `PassagemDePassageiro.pendencias()` é de **alcance**: as pendências respondem
 * *"este agregado é coerente consigo mesmo?"*, olhando só para ele; as guardas dependem do **mundo** — quantas
 * gratuidades já saíram naquela saída. O que este arquivo faz é receber esse número já apurado, para que a
 * decisão continue sendo pura e o I/O fique visível em quem a chama ([ADR-0025] D3).
 */

/**
 * A cota do [ADR-0013] §8: cada saída concede no máximo **duas** gratuidades **por categoria** (2 idosos,
 * 2 PcD, 2 crianças até 5, 2 passes federais).
 *
 * É "assento livre da travessia", então conta **por ocorrência** — não por viagem semanal, senão duas terças
 * diferentes disputariam as mesmas duas vagas (ADR-0016 §7.1 corrigiu esse vocabulário depois do ADR-0013).
 */
const val COTA_DE_GRATUIDADE_POR_CATEGORIA = 2

/**
 * Avalia a emissão de uma passagem de pessoa.
 *
 * @param jaEmitidasNaCategoria quantas gratuidades **daquela categoria** já existem na ocorrência, **sem
 *   contar as canceladas** — cancelada não ocupa (ADR-0018 D18), então também não consome cota.
 *
 * A ordem das guardas não é indiferente: a **coerência vem primeiro** porque uma passagem incoerente pode nem
 * ter subtipo de gratuidade para contar — perguntar a cota antes seria perguntar sobre um dado que ainda não
 * existe.
 */
fun avaliarEmissao(passagem: PassagemDePassageiro, jaEmitidasNaCategoria: Int): ResultadoEmissao {
    val pendencias = passagem.pendencias()
    if (pendencias.isNotEmpty()) return ResultadoEmissao.Incoerente(pendencias)

    val gratuidade = passagem.gratuidade ?: return ResultadoEmissao.Ok
    if (jaEmitidasNaCategoria >= COTA_DE_GRATUIDADE_POR_CATEGORIA) {
        return ResultadoEmissao.CotaGratuidadeAtingida(gratuidade)
    }
    return ResultadoEmissao.Ok
}