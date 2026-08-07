package dev.matheus.fluviapp.domain.operacoes

/**
 * **Onde a pessoa trabalha, e como** (ADR-0016 §6/§6.1, ADR-0022 D4): a ligação entre um [Funcionario] e
 * uma empresa. Um funcionário tem uma lista deles — é isso que o faz servir mais de uma empresa, e ter
 * papel diferente em cada uma.
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

/**
 * As empresas em que a pessoa atua — o `empresaIds` do §6, **calculado**.
 *
 * No documento ele é denormalização deliberada (o Firestore não consulta campo de dentro de elemento de
 * array, então "quem trabalha na empresa X" precisa do array chato ao lado). Em memória não é: aqui ele
 * se deriva na hora, e gravá-lo seria manter duas verdades para a mesma pergunta.
 */
val List<Vinculo>.empresaIds: List<String> get() = map { it.empresaId }.distinct()

/** O vínculo desta pessoa **naquela** empresa, se houver. */
fun List<Vinculo>.naEmpresa(empresaId: String): Vinculo? = firstOrNull { it.empresaId == empresaId }

/**
 * O vínculo **ativo por ausência de escolha**: quem tem um só não precisa escolher nada.
 *
 * Com mais de um, devolve `null` de propósito — a escolha é da pessoa (ADR-0016 §6, "seleção de
 * contexto"), e adivinhar por ela seria decidir em nome de quem opera. Sem nenhum, também `null`: é o
 * caso do papel puro de plataforma, que não atua em empresa nenhuma.
 */
fun List<Vinculo>.unicoOuNenhum(): Vinculo? = singleOrNull()

/**
 * **Qual vínculo está em vigor**, dada a escolha guardada (ADR-0016 §6 — F6.4). Função pura: é a regra
 * inteira da seleção de contexto, e é ela que os testes cobrem sem DataStore nem tela.
 *
 * | vínculos | escolha | resultado |
 * |---|---|---|
 * | nenhum | — | `null` — papel puro de plataforma, não atua em empresa nenhuma |
 * | um | qualquer | **o único** — quem não tem alternativa não escolhe, e escolha guardada não o contradiz |
 * | vários | válida | o escolhido |
 * | vários | ausente ou **vencida** | `null` — é a pergunta que falta fazer |
 *
 * A linha da escolha **vencida** é a que evita o pior defeito possível aqui: alguém perde o vínculo com
 * uma empresa e continua operando em nome dela porque o id ficou gravado no aparelho. A escolha é
 * revalidada contra os vínculos **a cada leitura** — ela é uma preferência, nunca uma credencial.
 */
fun resolverVinculoAtivo(vinculos: List<Vinculo>, empresaEscolhida: String?): Vinculo? = when {
    vinculos.isEmpty() -> null
    vinculos.size == 1 -> vinculos.single()
    else -> empresaEscolhida?.let { vinculos.naEmpresa(it) }
}

/**
 * Falta escolher? Só quando há **mais de uma** opção e nenhuma escolha válida em vigor.
 *
 * Note o que isto **não** é: "não tem vínculo ativo". Quem não tem vínculo nenhum também não tem vínculo
 * ativo, e mandá-lo escolher entre zero opções seria uma tela sem saída.
 */
fun precisaEscolherVinculo(vinculos: List<Vinculo>, empresaEscolhida: String?): Boolean =
    vinculos.size > 1 && resolverVinculoAtivo(vinculos, empresaEscolhida) == null