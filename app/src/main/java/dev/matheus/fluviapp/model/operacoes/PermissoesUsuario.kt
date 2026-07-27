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

    // --- Eixo seção (menu) ---

    /**
     * Quase todo o menu é eixo de SISTEMA — cadastro de viagem, empresa e navio é da plataforma. A
     * exceção é a **Equipe**: ela existe para o `SUPERVISOR` gerir os membros da própria agência
     * (ADR-0015 §2.2), então a seção olha os dois eixos. É a mistura que o §8.2 assume enquanto o app
     * cobre um processo só.
     */
    fun podeAcessar(secao: SecaoMenu, papel: String?, cargo: String? = null): Boolean = when (secao) {
        SecaoMenu.PASSAGEM -> true
        SecaoMenu.EQUIPE -> podeCadastrarFuncionario(papel, cargo)
        SecaoMenu.VIAGEM, SecaoMenu.EMPRESA, SecaoMenu.NAVIO -> ehPapelPlataforma(papel)
    }

    fun secoesVisiveis(papel: String?, cargo: String? = null): List<SecaoMenu> =
        SecaoMenu.entries.filter { podeAcessar(it, papel, cargo) }

    // --- Eixo ação sobre a Equipe (ADR-0015 §2.1/§2.2/§8.5) ---

    /** Cadastrar/editar membro: plataforma em qualquer agência, supervisor na dele (§2.1). */
    fun podeCadastrarFuncionario(papel: String?, cargo: String?): Boolean =
        ehPapelPlataforma(papel) || Cargo.de(cargo) == Cargo.SUPERVISOR

    /**
     * Escolher a agência do membro no form. Para o supervisor ela é **implícita** — a dele —, e não por
     * comodidade de UI: é o isolamento por agência (§3) aplicado ao cadastro.
     */
    fun podeEscolherAgencia(papel: String?): Boolean = ehPapelPlataforma(papel)

    /**
     * Definir o **cargo** de um membro (§8.5, decisão do analista): só a plataforma. O supervisor gere a
     * própria agência, mas não fabrica outro supervisor — cargo concede editar-qualquer-passagem (§8.2).
     */
    fun podeDefinirCargo(papel: String?): Boolean = ehPapelPlataforma(papel)

    /** Remover membro é operação de plataforma; o supervisor edita, não apaga (§2.1). */
    fun podeDeletarFuncionario(papel: String?): Boolean = ehPapelPlataforma(papel)

    /**
     * Enxergar **todas as agências** (listagem da Equipe hoje; passagens em P2.6 — §4.1). O supervisor
     * vê só a própria, e por isso a agência deixa de ser filtro para ele: sobra a lotação (§2.2).
     */
    fun podeVerTodasAgencias(papel: String?): Boolean = ehPapelPlataforma(papel)

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