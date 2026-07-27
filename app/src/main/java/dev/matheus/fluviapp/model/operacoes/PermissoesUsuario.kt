package dev.matheus.fluviapp.model.operacoes

import dev.matheus.fluviapp.model.operacoes.Funcionario.Cargo
import dev.matheus.fluviapp.model.operacoes.Usuario.Papel
import dev.matheus.fluviapp.model.operacoes.Usuario.Papel.ADM
import dev.matheus.fluviapp.model.operacoes.Usuario.Papel.GESTOR
import dev.matheus.fluviapp.model.screendata.SecaoMenu

/**
 * Política **única** de autorização — fonte de verdade das permissões (ADR-0010, ADR-0015 §8.2).
 *
 * Ela não se divide em duas depois da revisão estrutural: continua uma só, agora com **duas entradas**,
 * porque a pergunta "quem pode" mistura os dois contextos enquanto o app cobre um processo só (emissão
 * de passagem digital). Quando o sistema abarcar novos processos, a política expande junto.
 *
 *  - **papel** ([Usuario.papel], contexto SISTEMA): o que compete no aplicativo.
 *  - **cargo** ([Funcionario.cargo], contexto NEGÓCIO): o que a pessoa faz na operação.
 *
 * Eixos de decisão: seção (menu) e ação sobre a Passagem (com posse). Ambos chegam como String
 * (persistidos) e são convertidos para enum na fronteira desta política — nunca se compara `.name`
 * solto. Desconhecido → sem permissão (fail-closed). **Cargo ausente é caso normal**, não erro: o
 * `ADM`/`GESTOR` não tem registro de funcionário, e ali quem decide é o papel. A UI apenas *pergunta*.
 */
object PermissoesUsuario {

    /**
     * Papel de **plataforma** (FluviApp) = [ADM] ou [GESTOR]: acesso total às seções operacionais e, quando
     * o escopo por agência entrar (ADR-0015 §4.1), o que atravessa todas as agências. Nomeado pelo *escopo*
     * — e por *papel*, não "cargo", desde que os dois eixos se separaram (§8.1).
     */
    fun ehPapelPlataforma(papel: String?): Boolean = ehPapelPlataforma(Papel.de(papel))

    private fun ehPapelPlataforma(papel: Papel?): Boolean = papel == ADM || papel == GESTOR

    // --- Eixo seção (menu): puramente de SISTEMA ---

    fun podeAcessar(secao: SecaoMenu, papel: String?): Boolean = when (secao) {
        SecaoMenu.PASSAGEM -> true
        SecaoMenu.VIAGEM, SecaoMenu.EQUIPE, SecaoMenu.EMPRESA, SecaoMenu.NAVIO -> ehPapelPlataforma(papel)
    }

    fun secoesVisiveis(papel: String?): List<SecaoMenu> =
        SecaoMenu.entries.filter { podeAcessar(it, papel) }

    // --- Eixo ação sobre a Passagem (com posse) ---

    /** Os três papéis conhecidos podem criar passagem; papel desconhecido, não. */
    fun podeCriarPassagem(papel: String?): Boolean = Papel.de(papel) != null

    /**
     * Editar/deletar passagem de QUALQUER usuário. Aqui os dois eixos se encontram: vale para o papel de
     * plataforma **ou** para quem é [Cargo.SUPERVISOR] na operação — o master da agência.
     */
    fun podeEditarQualquerPassagem(papel: String?, cargo: String?): Boolean =
        ehPapelPlataforma(papel) || Cargo.de(cargo) == Cargo.SUPERVISOR

    /** Editar uma passagem específica: se for o dono, ou se puder editar qualquer uma. */
    fun podeEditarPassagem(papel: String?, cargo: String?, ehDono: Boolean): Boolean =
        ehDono || podeEditarQualquerPassagem(papel, cargo)

    /** Deletar segue as mesmas regras de editar (decisão do ADR-0010). */
    fun podeDeletarPassagem(papel: String?, cargo: String?, ehDono: Boolean): Boolean =
        podeEditarPassagem(papel, cargo, ehDono)

    /** Ver todas as passagens na pesquisa (não só as próprias) acompanha o editar-qualquer. */
    fun podeVerTodasPassagens(papel: String?, cargo: String?): Boolean =
        podeEditarQualquerPassagem(papel, cargo)

    /**
     * Confirmar embarque (validar o QR na doca) — eixo NOVO (ADR-0012). Validar embarque **≠** editar
     * o conteúdo do bilhete: qualquer papel conhecido pode validar (é ação de doca, mesmo sem ter
     * vendido); papel desconhecido, não (fail-closed).
     */
    fun podeConfirmarEmbarque(papel: String?): Boolean = Papel.de(papel) != null
}