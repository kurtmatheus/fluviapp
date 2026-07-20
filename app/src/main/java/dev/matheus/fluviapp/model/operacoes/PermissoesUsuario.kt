package dev.matheus.fluviapp.model.operacoes

import dev.matheus.fluviapp.model.operacoes.Usuario.Cargo
import dev.matheus.fluviapp.model.operacoes.Usuario.Cargo.ADM
import dev.matheus.fluviapp.model.operacoes.Usuario.Cargo.COLABORADOR_MASTER
import dev.matheus.fluviapp.model.operacoes.Usuario.Cargo.DIRETOR
import dev.matheus.fluviapp.model.screendata.SecaoMenu

/**
 * Política única de autorização por cargo — fonte de verdade das permissões (ADR-0010).
 *
 * Dois eixos:
 *  - **Seção (menu)**: quais seções o usuário vê (`podeAcessar`/`secoesVisiveis`).
 *  - **Ação sobre a Passagem, com posse**: criar / editar / deletar / ver-todas.
 *
 * O `cargo` chega como String (persistido no Firestore/DataStore) e é convertido para o enum
 * `Cargo` na fronteira desta política (`Cargo.de`) — as regras casam por tipo, nunca por `.name`
 * solto. Cargo desconhecido → sem permissão (fail-closed). A UI apenas *pergunta*.
 */
object PermissoesUsuario {

    /** Gestor = ADM ou DIRETOR (acesso total às seções operacionais). */
    fun ehGestor(cargo: String?): Boolean = ehGestor(Cargo.de(cargo))

    private fun ehGestor(cargo: Cargo?): Boolean = cargo == ADM || cargo == DIRETOR

    // --- Eixo seção (menu) ---

    fun podeAcessar(secao: SecaoMenu, cargo: String?): Boolean = when (secao) {
        SecaoMenu.PASSAGEM -> true
        SecaoMenu.VIAGEM, SecaoMenu.AGENTE, SecaoMenu.EMPRESA, SecaoMenu.NAVIO -> ehGestor(cargo)
    }

    fun secoesVisiveis(cargo: String?): List<SecaoMenu> =
        SecaoMenu.entries.filter { podeAcessar(it, cargo) }

    // --- Eixo ação sobre a Passagem (com posse) ---

    /** Os quatro cargos conhecidos podem criar passagem; cargo desconhecido, não. */
    fun podeCriarPassagem(cargo: String?): Boolean = Cargo.de(cargo) != null

    /** Editar/deletar passagem de QUALQUER usuário: gestor ou Colaborador Master. */
    fun podeEditarQualquerPassagem(cargo: String?): Boolean {
        val c = Cargo.de(cargo)
        return ehGestor(c) || c == COLABORADOR_MASTER
    }

    /** Editar uma passagem específica: se for o dono, ou se puder editar qualquer uma. */
    fun podeEditarPassagem(cargo: String?, ehDono: Boolean): Boolean =
        ehDono || podeEditarQualquerPassagem(cargo)

    /** Deletar segue as mesmas regras de editar (decisão do ADR-0010). */
    fun podeDeletarPassagem(cargo: String?, ehDono: Boolean): Boolean =
        podeEditarPassagem(cargo, ehDono)

    /** Ver todas as passagens na pesquisa (não só as próprias) acompanha o editar-qualquer. */
    fun podeVerTodasPassagens(cargo: String?): Boolean = podeEditarQualquerPassagem(cargo)
}