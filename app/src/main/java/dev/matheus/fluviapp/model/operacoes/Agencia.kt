package dev.matheus.fluviapp.model.operacoes

/**
 * Agência a que um [Usuario] pertence — **capacidade organizacional**, não permissão (ADR-0015 §2):
 * diz *onde* o membro atua; *o que* ele pode fazer continua saindo do cargo ([Usuario.Cargo]).
 *
 * Vivia dentro de `Agente.Agencia`. Saiu de lá porque a entidade `Agente` é aposentada (ADR-0015 §7) e
 * o `Usuario` não pode depender de quem vai morrer.
 *
 * **Conjunto fixo por ora** (ADR-0015): vira coleção cadastrável quando houver cadastro de agência — daí
 * a relação passa a ser por id (ADR-0008). Enquanto é enum, o valor persistido é o `name`, com String só
 * na fronteira (mesmo padrão de `Cargo`/`StatusPassagem`).
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

        /**
         * Leitura de fronteira: valor ausente/vazio/desconhecido cai em [AUTONOMO]. Cobre documento
         * antigo (Firestore é schemaless — perfis anteriores a este campo simplesmente não o têm) sem
         * espalhar `null` pelo modelo.
         */
        fun deOuPadrao(valor: String?): Agencia = de(valor) ?: AUTONOMO
    }
}