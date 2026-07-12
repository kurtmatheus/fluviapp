package dev.matheus.fluviapp.model.operacoes

import dev.matheus.fluviapp.model.operacoes.Usuario.Cargo.ADM
import dev.matheus.fluviapp.model.operacoes.Usuario.Cargo.DIRETOR
import dev.matheus.fluviapp.model.screendata.SecaoMenu

/**
 * Política única de autorização por cargo — substitui os `cargo == ADM.name || …` espalhados.
 * Fonte de verdade de quais seções do menu o usuário pode ver.
 */
object PermissoesUsuario {

    /** Gestor = ADM ou DIRETOR (acesso às seções operacionais). */
    fun ehGestor(cargo: String?): Boolean =
        cargo == ADM.name || cargo == DIRETOR.name

    fun podeAcessar(secao: SecaoMenu, cargo: String?): Boolean = when (secao) {
        SecaoMenu.PASSAGEM -> true
        SecaoMenu.VIAGEM, SecaoMenu.AGENTE, SecaoMenu.EMPRESA -> ehGestor(cargo)
    }

    fun secoesVisiveis(cargo: String?): List<SecaoMenu> =
        SecaoMenu.entries.filter { podeAcessar(it, cargo) }
}
