package dev.matheus.fluviapp.domain.porto

/**
 * **O lugar físico** onde se embarca e se desembarca (ADR-0016 §5). Como a [dev.matheus.fluviapp.domain.localidade.Localidade],
 * é capacidade da **plataforma**: mora na raiz e não pertence a empresa nenhuma — o cais de Manaus é o
 * mesmo cais para todas as empresas que atracam nele, e modelá-lo dentro da empresa produziria um
 * documento por empresa para o mesmo lugar.
 *
 * ### A localidade entra por id, e não por cópia
 *
 * [localidadeId] é **referência**: a `Localidade` tem coleção, identidade e ciclo de vida próprios, e
 * embutir uma cópia viva dela em cada porto criaria N cópias do mesmo município para manter (§5, e é o
 * ADR-0008 aplicado). O preço é que exibir "Porto de Val-de-Cães — Belém/PA" exige resolver o id — e o
 * troco é que corrigir a grafia de um município conserta todos os portos dele de uma vez.
 *
 * Note o que **não** está aqui: nem `uf`, nem `municipio`, nem rótulo pronto. O porto não sabe onde fica;
 * sabe **de quem** perguntar. Guardar o rótulo junto seria a cópia viva por outro nome.
 *
 * ### Por que não há chave natural
 *
 * A `Localidade` tem o código do IBGE, e por isso a unicidade dela é dada. Porto não tem cadastro
 * nacional que sirva de âncora, então o que o distingue é o par **(nome, localidade)** — dois "Porto
 * Central" em Belém são o mesmo problema um nível abaixo (§5). É invariante de cadastro, e é lá que ele
 * é verificado; a paridade no servidor fica para a F8.
 *
 * ### Delete lógico
 *
 * [ativo] pela mesma razão da Localidade, um elo adiante: o porto é referenciado pela **rota** e pela
 * **concessão** da empresa. Remover invalidaria as duas, e verificar "nenhuma rota usa" exigiria
 * collection group. Inativado, o porto some dos seletores e continua resolvendo o que já aponta para
 * ele — que é o comportamento correto para dado referenciado por fato histórico.
 */
data class Porto(
    val id: String,
    val nome: String,
    val localidadeId: String,
    val ativo: Boolean = true,
)