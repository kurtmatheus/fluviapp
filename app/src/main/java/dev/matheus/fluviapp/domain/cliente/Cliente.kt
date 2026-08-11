package dev.matheus.fluviapp.domain.cliente

import dev.matheus.fluviapp.domain.documento.TipoDocumento
import java.time.LocalDate

/**
 * Quem viaja — **entidade com cadastro próprio**, referenciada pela passagem por id ([ADR-0023] D5).
 *
 * É o **pool** que o [ADR-0018] D2/D3 desenhou: acumulativo, não *master data*. Ele garante que a informação
 * **exista** e seja atribuível, não que seja canônica — a mesma pessoa cadastrada com CPF numa agência e RG
 * noutra vira **duas entradas**, e isso é **aceito**: *"não é redundância, é questão de análise de dados"*.
 * Deduplicar é etapa analítica posterior, jamais no caminho da emissão.
 *
 * O mesmo tipo serve ao **passageiro** e ao **responsável pela retirada** de um veículo: é pessoa nos dois casos,
 * e o vínculo é da passagem — quem retira muda a cada travessia.
 *
 * ### Todo passageiro tem documento
 *
 * *"Não existe criança sem documento nesse negócio"* (ADR-0018 D4). Tipo **e** número são obrigatórios, e o
 * número tem de ser válido para o tipo ([ADR-0020] D2). É isso que faz a [chaveNatural] cobrir 100% dos
 * passageiros — e é a diferença em relação ao que o app fazia, que só cobrava o número **se** um tipo houvesse
 * sido escolhido, de modo que se emitia bilhete sem credencial nenhuma.
 *
 * ### O telefone é o único campo que não identifica
 *
 * Ele existe para **alcançar** a pessoa; todos os outros existem para **identificar quem viaja**. Duas
 * consequências: ele entra na conversa de LGPD junto do documento, e **não participa da chave natural** — dois
 * telefones diferentes não fazem duas pessoas, um documento diferente faz.
 *
 * A [dataNascimento] é [LocalDate], e não texto: é ela que **decide** a gratuidade de criança, e o app já
 * reparseava o texto para aplicar a regra. Tipo no domínio, formatação na fronteira — a régua da F8.1.
 */
data class Cliente(
    val id: String = "",
    val nome: String,
    val tipoDocumento: TipoDocumento,
    val numeroDocumento: String,
    val dataNascimento: LocalDate,
    /** Para contato. Opcional: não identifica ninguém. */
    val telefone: String? = null,
    /**
     * As agências que já atenderam esta pessoa (ADR-0018 D3): **existência é global, visibilidade é local**.
     * Conjunto porque assinar duas vezes é o mesmo que assinar uma — a escrita é `arrayUnion`.
     */
    val agenciaIds: Set<String> = emptySet(),
) {
    /**
     * A identidade do pool: **o documento apresentado**. Não é o [id] — aquele é surrogate, gerado pela coleção;
     * esta é a chave natural que decide se a entrada **já existe** antes de criar outra.
     */
    val chaveNatural: String get() = "${tipoDocumento.name}:${numeroDocumento.filter { it.isDigit() || it.isLetter() }}"

    /** Já atendido por esta agência? É o que decide entre *criar* e *assinar* (ADR-0018 D3). */
    fun assinadoPor(agenciaId: String): Boolean = agenciaId in agenciaIds
}