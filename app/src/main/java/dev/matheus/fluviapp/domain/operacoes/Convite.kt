package dev.matheus.fluviapp.domain.operacoes

/**
 * **O convite** — o único lugar onde os dois contextos se encontram (estudo `usuario-e-funcionario.md`).
 *
 * A plataforma não cria contas: criar conta pelo SDK cliente do Firebase **troca a sessão** para a conta
 * nova, e é por isso que o bootstrap do primeiro `ADM` é manual (ADR-0021 D0). O que ela cria é este
 * documento: *"esta pessoa pode entrar, com este papel — e, se for da operação, nesta empresa e com este
 * cargo"*. Quem entra é a pessoa, no primeiro acesso, criando a própria senha (ADR-0015 §2.1).
 *
 * ### Por que ele não afrouxa o anti-escalonamento
 *
 * O cliente continua **não escolhendo o próprio papel**: ele vem de um documento que só o `ADM` escreve,
 * e a regra do servidor confere o `users/{uid}` contra o convite do mesmo e-mail. O que muda é de onde o
 * papel vem — do convite em vez de ser sempre `OPERADOR` —, não quem o decide.
 *
 * ### As duas metades
 *
 * - **papel** é do contexto de SISTEMA e existe sempre;
 * - **empresa + cargo** são do contexto de NEGÓCIO e existem só para o `OPERADOR`: `ADM`/`GESTOR` não têm
 *   registro na operação (§8.1) — e é justamente por isso que não emitem passagem.
 *
 * É essa segunda metade que resolve o galinha-e-ovo do painel da empresa: sem ela, ninguém criaria o
 * primeiro supervisor de uma empresa, porque a Equipe é gerida de dentro.
 */
data class Convite(
    /** E-mail convidado — **é o id do documento**, e é por ele que o primeiro acesso se encontra. */
    val email: String,
    /**
     * O nome da pessoa. Ele existe aqui porque o convite de operador **cria o `Funcionario`**, e
     * funcionário sem nome seria um registro que ninguém reconhece na lista da equipe. Para papel de
     * plataforma ele é só cortesia de exibição — o `Usuario` não tem nome, tem `username` (§8.1).
     */
    val nome: String,
    val papel: Usuario.Papel,
    /** Só para [Usuario.Papel.OPERADOR]: em que empresa ele entra. */
    val empresaId: String = "",
    /** Só para [Usuario.Papel.OPERADOR]: com que cargo. */
    val cargo: Funcionario.Cargo? = null,
    /** `true` depois que alguém usou o convite — o registro fica, e é o que a lista mostra como situação. */
    val usado: Boolean = false,
) {

    /** Papel de plataforma não leva vínculo: `ADM`/`GESTOR` não atuam em empresa nenhuma (§8.1). */
    val ehDePlataforma: Boolean get() = papel == Usuario.Papel.ADM || papel == Usuario.Papel.GESTOR

    /**
     * O vínculo que este convite cria na operação — `null` para papel de plataforma, e também quando
     * falta empresa ou cargo (aí não há vínculo a criar, e a validação já recusou o convite).
     */
    val vinculo: Vinculo? get() = if (ehDePlataforma) null else Vinculo.de(empresaId, cargo?.name)

    companion object {
        /**
         * Fronteira (String → domínio). **Papel desconhecido não vira convite** (fail-closed, ADR-0010):
         * um convite é o que concede papel, e um papel ilegível concederia acesso que ninguém sabe qual é.
         */
        fun de(
            email: String?,
            nome: String?,
            papel: String?,
            empresaId: String?,
            cargo: String?,
            usado: Boolean = false,
        ): Convite? {
            if (email.isNullOrBlank()) return null
            val papelConhecido = Usuario.Papel.de(papel) ?: return null

            return Convite(
                email = email.trim().lowercase(),
                nome = nome.orEmpty(),
                papel = papelConhecido,
                empresaId = empresaId.orEmpty(),
                cargo = Funcionario.Cargo.de(cargo),
                usado = usado,
            )
        }
    }
}