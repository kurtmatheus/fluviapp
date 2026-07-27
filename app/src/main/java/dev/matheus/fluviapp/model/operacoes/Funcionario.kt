package dev.matheus.fluviapp.model.operacoes

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.matheus.fluviapp.model.IObjetoSimplificado
import dev.matheus.fluviapp.services.repository.firebase.documents.FuncionarioDocumento

/**
 * A pessoa na **operação** — contexto de NEGÓCIO (ADR-0015 §8.1). Era `Agente`: não morreu, mudou de
 * pergunta. O [Usuario] responde *quem acessa o app e o que pode nele*; o `Funcionario` responde *quem é a
 * pessoa na operação* — e por isso é aqui que moram o [cargo], a [agencia] e a [lotacao].
 *
 * O [descricaoNome] é o **nome da pessoa**: o `Usuario` não tem nome, tem `username` (credencial). Os dois
 * lados se ligam 1-1 por [Usuario.funcionarioId] (§8.3).
 */
@Entity(indices = [Index("id")])
data class Funcionario(
    @PrimaryKey
    override val id: String,
    override val descricaoNome: String,
    val agencia: String,
    var lotacao: String,
    /**
     * Cargo de **negócio** (§8.1) — nasce [Cargo.AGENTE], o menor privilégio. Promover é decisão da
     * gestão (ADR-0015 §8.5), nunca do próprio.
     */
    val cargo: String = Cargo.AGENTE.name,
) : IObjetoSimplificado {

    /**
     * O eixo **aberto** da autorização (ADR-0015, revisão estrutural): hoje supervisor e agente, amanhã
     * quem faz check-in, quem valida embarque, quem responde por um navio. Cresce sem tocar no
     * [Usuario.Papel], que é fechado por natureza.
     *
     * Sem default na fronteira, de propósito: cargo desconhecido → sem permissão (fail-closed, ADR-0010).
     */
    enum class Cargo {
        SUPERVISOR,
        AGENTE;

        companion object {
            /** Converte o cargo persistido (String) no enum canônico; null se desconhecido. */
            fun de(valor: String?): Cargo? = entries.firstOrNull { it.name == valor }
        }
    }

    enum class Lotacao {
        PORTO_NORTE,
        ILHA_CENTRAL,
        PORTO_SUL
    }
}

fun Funcionario.toDocumento(): FuncionarioDocumento {
    return FuncionarioDocumento(
        nome = descricaoNome,
        agencia = agencia,
        lotacao = lotacao,
        cargo = cargo
    )
}