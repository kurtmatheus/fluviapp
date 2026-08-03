package dev.matheus.fluviapp.domain.operacoes

/**
 * Agência a que um [Funcionario] pertence — **capacidade organizacional**, não permissão (ADR-0015
 * §8.1): diz *onde* a pessoa atua; *o que* ela pode fazer sai do papel e do cargo.
 *
 * Vivia dentro de `Agente.Agencia`, passou por `Usuario` (P2.2a) e voltou para o contexto de negócio com
 * a revisão estrutural — é o funcionário que tem agência, não a conta de acesso.
 *
 * **Conjunto fixo por ora** (ADR-0015): vira coleção cadastrável quando houver cadastro de agência — daí
 * a relação passa a ser por id (ADR-0008). Enquanto isso, `Funcionario.agencia` ainda é String **livre**
 * (o seed tem "AGENCIA HORIZONTE" e outras que não estão aqui), então este enum nomeia o que o app
 * conhece — a coringa e a matriz — e não valida o campo. Fechar essa folga é trabalho do P2.2b, quando a
 * agência virar seletor.
 */
enum class Agencia {
    /**
     * Agência **coringa**: quem não está vinculado a nenhuma. É o **padrão**, não um estado inválido —
     * um membro recém-provisionado nasce aqui até a gestão alocá-lo (ADR-0015 §2.1).
     */
    AUTONOMO,
    MATRIZ;

    companion object {
        /** Converte o valor persistido no enum canônico; `null` se desconhecido (deriva de dado). */
        fun de(valor: String?): Agencia? = entries.firstOrNull { it.name == valor }
    }
}