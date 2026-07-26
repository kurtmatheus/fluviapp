package dev.matheus.fluviapp.model.operacoes

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index("id")])
data class Usuario(
    @PrimaryKey
    val id: String,
    val email: String,
    val nome: String,
    val cargo: String,
    /**
     * Capacidades **organizacionais** (ADR-0015 §2) — onde o membro atua, não o que ele pode fazer.
     * [agencia] é o `name` de [Agencia] (default `AUTONOMO`, a coringa de quem não foi alocado);
     * [lotacao] é o **município**, vindo do catálogo `Constante` (categoria `MUNICIPIO`).
     */
    val agencia: String = Agencia.AUTONOMO.name,
    val lotacao: String = "",
    val ultimoUsuarioLogado: Boolean = false
) {
    /**
     * Cargos em dois escopos (ADR-0015 §4.1/§4.2):
     *  - **Plataforma (FluviApp)**: [ADM] e [GESTOR] — gestores do sistema, atravessam todas as agências.
     *  - **Agência**: [SUPERVISOR] (o master *da agência*; cargos executivos de agência entram aqui) e
     *    [AGENTE] (quem vende). Gerem informação dentro da própria agência.
     *
     * `AGENTE` é **cargo**, não entidade: o `Agente` como entidade paralela é aposentado (ADR-0015 §7),
     * o que libera o nome para o papel de quem emite — "o agente é o usuário".
     */
    enum class Cargo {
        ADM,
        GESTOR,
        SUPERVISOR,
        AGENTE;

        companion object {
            /** Converte o cargo persistido (String) no enum canônico; null se desconhecido. */
            fun de(valor: String?): Cargo? = entries.firstOrNull { it.name == valor }
        }
    }

    companion object {
        const val GERAL = "Geral"
    }
}