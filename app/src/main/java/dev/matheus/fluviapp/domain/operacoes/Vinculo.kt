package dev.matheus.fluviapp.domain.operacoes

/**
 * **Onde a pessoa trabalha, e como** (ADR-0016 §6/§6.1, ADR-0022 D4): a ligação entre um [Funcionario] e
 * uma empresa. Um funcionário tem **no máximo um** — é o que a [ADR-0032] D5 decidiu, e o que a Q2
 * levou até a estrutura.
 *
 * ### Por que só dois campos, e não três
 *
 * O ADR-0016 §6 desenha o vínculo como `{empresaId, atuacao, cargo}`. Aqui ele tem **dois**, e a atuação
 * é [atuacao] — derivada do cargo, não gravada ao lado dele.
 *
 * A razão está no próprio §6.1: *"cada valor declara a que atuação pertence"*, e o conjunto de cargos de
 * cada atuação é **disjunto por construção**. Guardar a atuação junto seria guardar duas vezes o que já
 * se sabe uma vez — e criar um estado impossível de validar depois: um vínculo com `atuacao = TRANSPORTE`
 * e `cargo = AGENTE` (que é do agenciamento) não significa nada, e nada impediria alguém de escrevê-lo.
 *
 * É o mesmo critério que dissolveu o Trecho na 7ª rodada: **o que é derivável não vira campo.** A
 * diferença aqui é que o campo derivado seria *contraditório*, não só redundante.
 *
 * ### O que ele NÃO carrega
 *
 * Nome de empresa, rótulo, agência. A empresa entra por **id** (ADR-0008), e quem exibe resolve — como o
 * porto faz com a localidade. O vínculo é a relação, não um resumo dela.
 *
 * ### O que ele deixou de carregar (ADR-0032 Q2)
 *
 * Um conjunto de extensões sobre `List<Vinculo>` — `empresaIds`, `naEmpresa`, `unicoOuNenhum`,
 * `resolverVinculoAtivo` e `precisaEscolherVinculo`. Todas respondiam à mesma pergunta: *qual dos
 * vínculos vale agora?* Com um vínculo no máximo, a pergunta não existe — o vínculo em vigor é **o
 * vínculo**, e a escolha entre empresas era o único chamador da preferência guardada no aparelho.
 */
data class Vinculo(
    val empresaId: String,
    val cargo: Funcionario.Cargo,
) {

    /** Em que segmento esta pessoa atua **nesta** empresa. Vem do cargo, que já a declara (§6.1). */
    val atuacao: Atuacao get() = cargo.atuacao

    companion object {
        /**
         * Fronteira (String → domínio): **cargo desconhecido não vira vínculo**, devolve `null`.
         *
         * É o fail-closed do ADR-0010 aplicado onde ele importa mais — vínculo é o que concede permissão,
         * e um vínculo com cargo ilegível concederia permissão sem que ninguém saiba qual. Empresa em
         * branco também recusa: vínculo sem empresa não liga a lugar nenhum.
         */
        fun de(empresaId: String?, cargo: String?): Vinculo? {
            if (empresaId.isNullOrBlank()) return null
            val papelNaOperacao = Funcionario.Cargo.de(cargo) ?: return null
            return Vinculo(empresaId = empresaId, cargo = papelNaOperacao)
        }
    }
}
