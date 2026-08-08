package dev.matheus.fluviapp.domain.operacoes

import dev.matheus.fluviapp.domain.operacoes.Funcionario.Cargo
import dev.matheus.fluviapp.domain.operacoes.Usuario.Papel
import dev.matheus.fluviapp.domain.operacoes.Usuario.Papel.ADM
import dev.matheus.fluviapp.domain.operacoes.Usuario.Papel.GESTOR
import dev.matheus.fluviapp.domain.screendata.SecaoMenu
import dev.matheus.fluviapp.domain.screendata.secoesDa
import dev.matheus.fluviapp.domain.screendata.secoesDoPainel

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
     * Quase todo o menu é eixo de SISTEMA — cadastro de viagem, empresa e embarcação é da plataforma. A
     * exceção é a **Equipe**: ela existe para o `SUPERVISOR` gerir os membros da própria agência
     * (ADR-0015 §2.2), então a seção olha os dois eixos. É a mistura que o §8.2 assume enquanto o app
     * cobre um processo só.
     */
    fun podeAcessar(secao: SecaoMenu, papel: String?, cargo: String? = null): Boolean = when (secao) {
        SecaoMenu.PASSAGEM -> true

        // A **Equipe é da empresa** (F6.6): quem a abre é o cargo de gestão dela. A plataforma não entra
        // aqui — não porque perdeu autoridade (a regra do servidor continua admitindo-a, e é ela quem
        // conserta o que a empresa não consegue), mas porque **este não é o trabalho dela**: o que a
        // plataforma administra é quem acessa o app, e isso agora tem seção própria.
        SecaoMenu.EQUIPE -> Cargo.de(cargo) == Cargo.SUPERVISOR

        // **`ADM`-only** (ADR-0021 D1): papel concede tudo, e erro aqui é sistêmico. O `GESTOR` administra
        // o negócio da plataforma, não o acesso a ela.
        SecaoMenu.USUARIOS -> Papel.de(papel) == ADM

        // **Rotas é compartilhada** (ADR-0022 D2): o pool não tem dono, então plataforma e operação
        // enxergam o mesmo. Quem pode **criar** e quem pode **inativar** são outras perguntas, abaixo.
        SecaoMenu.ROTA -> ehPapelPlataforma(papel) || Cargo.de(cargo) != null

        SecaoMenu.VIAGEM, SecaoMenu.EMPRESA, SecaoMenu.EMBARCACAO,
        SecaoMenu.LOCALIDADE, SecaoMenu.PORTO,
        -> ehPapelPlataforma(papel)
    }

    /**
     * As seções que este usuário vê, na ordem de [SecaoMenu].
     *
     * Dois eixos se compõem aqui, e a ordem importa: a **família** decide *quais seções existem naquele
     * contexto* (painel da plataforma × a atuação em que a pessoa trabalha — ADR-0016 §2), e a
     * **permissão** decide *quais delas ela pode abrir* ([podeAcessar]). Família sem permissão não
     * aparece; permissão sem família também não.
     *
     * [atuacao] **nula é o caminho de compatibilidade**, e é por ele que o app anda hoje: o vínculo
     * `(empresa, atuação)` do logado só passa a existir na F4 do ADR-0020 (contexto e splash). Sem ele,
     * cai-se no comportamento anterior — todas as seções, filtradas pela permissão —, de modo que esta
     * fatia **não muda nada em tela**. Quando a F4 passar a atuação, a família entra em vigor sem que esta
     * função mude.
     */
    fun secoesVisiveis(
        papel: String?,
        cargo: String? = null,
        atuacao: Atuacao? = null,
    ): List<SecaoMenu> {
        val familia = when {
            atuacao == null -> SecaoMenu.entries.toSet()
            ehPapelPlataforma(papel) -> secoesDoPainel()
            else -> secoesDa(atuacao)
        }
        return SecaoMenu.entries.filter { it in familia && podeAcessar(it, papel, cargo) }
    }

    // --- Eixo ação sobre a Equipe (ADR-0015 §2.1/§2.2/§8.5) ---

    /** Cadastrar/editar membro: plataforma em qualquer agência, supervisor na dele (§2.1). */
    fun podeCadastrarFuncionario(papel: String?, cargo: String?): Boolean =
        ehPapelPlataforma(papel) || Cargo.de(cargo) == Cargo.SUPERVISOR

    /**
     * Escolher a agência do membro no form. Para o supervisor ela é **implícita** — a dele —, e não por
     * comodidade de UI: é o isolamento por agência (§3) aplicado ao cadastro.
     */
    fun podeEscolherAgencia(papel: String?): Boolean = ehPapelPlataforma(papel)

    // As duas perguntas sobre a Equipe passaram a ser feitas **pelo vínculo** na F6.7 — ver
    // `podeDefinirCargo(papel, vinculo)` e `podeRemoverMembro` mais abaixo. As versões por papel só
    // saíram do caminho vivo; nada aqui as substituiu por acaso.

    /**
     * Enxergar **todas as agências** (listagem da Equipe e das passagens — §4.1). O supervisor vê só a
     * própria, e por isso a agência deixa de ser filtro para ele: sobra a lotação (§2.2).
     */
    fun podeVerTodasAgencias(papel: String?): Boolean = ehPapelPlataforma(papel)

    // --- O eixo do vínculo (ADR-0016 §6/§6.1, ADR-0022 D4 — F6) ---

    /**
     * Cadastrar/editar membro, **perguntado pelo vínculo**: plataforma em qualquer empresa, supervisor na
     * dele. É a mesma regra do §2.1 com a coordenada certa — antes o "dele" era uma String de agência,
     * agora é o id da empresa em que ele é supervisor.
     *
     * **Nome diferente de `podeCadastrarFuncionario` de propósito**, e não por gosto: as duas assinaturas
     * terminariam em `(String?, null)` no ponto de chamada, e o compilador não teria como escolher. Uma
     * sobrecarga ambígua que aparece meses depois, num `null` literal, é pior do que duas palavras. Esta
     * fica quando a outra sair (F6.3).
     */
    fun podeCadastrarMembro(papel: String?, vinculo: Vinculo?): Boolean =
        ehPapelPlataforma(papel) || vinculo?.cargo == Cargo.SUPERVISOR

    /**
     * **Definir o cargo de um membro** — e aqui a regra mudou (F6.7).
     *
     * O ADR-0015 §8.5 reservava isto à plataforma, porque cargo concede editar-qualquer-passagem. O
     * argumento continua verdadeiro, mas o **alcance** dele encolheu: desde que o escopo passou a ser
     * por empresa (F6.1/F6.3), promover alguém concede poder **dentro de uma empresa só** — e isso é
     * decisão de negócio dela, não de segurança da plataforma.
     *
     * O que impediu a mudança até agora era o inverso: com a plataforma na seção, ela promovia. Ao sair
     * dela (F6.6), ninguém promovia — nem o supervisor, nem quem já não tinha a tela. Um poder sem dono
     * é pior do que um poder distribuído.
     *
     * O anti-escalonamento continua inteiro por outro caminho, e é o mesmo do servidor: **ninguém mexe
     * nos próprios vínculos**, e o supervisor só alcança quem é exclusivamente da empresa dele.
     */
    fun podeDefinirCargo(papel: String?, vinculo: Vinculo?): Boolean =
        ehPapelPlataforma(papel) || vinculo?.cargo == Cargo.SUPERVISOR

    /** Remover membro: mesma regra do cargo (F6.7) — quem gere a equipe, gere-a por inteiro. */
    fun podeRemoverMembro(papel: String?, vinculo: Vinculo?): Boolean =
        ehPapelPlataforma(papel) || vinculo?.cargo == Cargo.SUPERVISOR

    // --- O pool compartilhado (ADR-0016 §7.1, ADR-0022 D3 — F7) ---

    /**
     * **Criar rota**: plataforma e supervisor, em qualquer par de portos (decisão do analista).
     *
     * Não se recorta pela concessão de propósito: a rota é a *ligação*, e ela existe no mundo
     * independentemente de quem pode vendê-la. O recorte do que a empresa oferta é da **viagem** (F8),
     * onde a concessão entra — e é lá que ele significa alguma coisa.
     */
    fun podeCriarRota(papel: String?, vinculo: Vinculo?): Boolean =
        ehPapelPlataforma(papel) || vinculo?.cargo == Cargo.SUPERVISOR

    /**
     * **Inativar rota é só da plataforma** (ADR-0022 D3).
     *
     * É o único poder deste conjunto que **atinge terceiros**: tirar do pool uma rota que outra empresa
     * está vendendo. Quem quer apenas não vê-la usa a lista de negadas da própria atuação (F8), que é
     * conforto de tela e não muda o pool de ninguém.
     */
    fun podeInativarRota(papel: String?): Boolean = ehPapelPlataforma(papel)

    /**
     * O **escopo de empresa** de uma listagem — o que o [EscopoAgencia] vira quando a agência deixa de
     * ser String e passa a ser a empresa em que se atua (§6).
     *
     * Os três casos continuam sendo os mesmos, e o terceiro continua sendo o perigoso: *não filtrar nada*
     * e *não ter empresa nenhuma* pareceriam iguais como String vazia, e abririam a listagem inteira
     * para quem não deveria ver nada.
     */
    sealed interface EscopoEmpresa {
        /** Papel de plataforma: atravessa empresas. */
        data object Todas : EscopoEmpresa

        /** Vínculo ativo: só a empresa dele. */
        data class Apenas(val empresaId: String) : EscopoEmpresa

        /** Sem papel de plataforma e sem vínculo: não há o que mostrar (fail-closed). */
        data object Nenhuma : EscopoEmpresa
    }

    fun escopoDeEmpresa(papel: String?, vinculo: Vinculo?): EscopoEmpresa = when {
        podeVerTodasAgencias(papel) -> EscopoEmpresa.Todas
        vinculo != null -> EscopoEmpresa.Apenas(vinculo.empresaId)
        else -> EscopoEmpresa.Nenhuma
    }

    /**
     * A **atuação em vigor**: a do vínculo ativo, e `null` para quem administra a plataforma.
     *
     * É esta função que substitui a derivação de hoje (`Cargo.de(cargo)?.atuacao` no `ContextoUsuario`),
     * e a diferença aparece quando a pessoa tem dois vínculos: o cargo deixa de ser um só, então a
     * atuação deixa de sair dele — sai da **escolha**.
     */
    fun atuacaoEmVigor(vinculo: Vinculo?): Atuacao? = vinculo?.atuacao

    // --- O eixo antigo, por String de agência (morre na F6.3) ---

    /**
     * O **escopo de agência** de uma listagem (ADR-0015 §4.1/§6). Existe como tipo, e não como String
     * vazia significando "sem filtro", porque os três casos são diferentes e o terceiro é o perigoso:
     * "não filtra nada" e "não tem agência nenhuma" pareceriam iguais e abririam a listagem inteira
     * para quem não deveria ver nada.
     *
     * **Substituído por [EscopoEmpresa]**, e ainda de pé porque quem o consome — a consulta de passagem —
     * é código não revitalizado (F9). Sai quando a Equipe terminar de trocar agência por empresa.
     */
    sealed interface EscopoAgencia {
        /** Papel de plataforma: atravessa agências. */
        data object Todas : EscopoAgencia

        /** Cargo de agência: só a dele. */
        data class Apenas(val agencia: String) : EscopoAgencia

        /** Sem papel de plataforma e sem vínculo: não há agência a mostrar (fail-closed). */
        data object Nenhuma : EscopoAgencia
    }

    fun escopoDeAgencia(papel: String?, agencia: String?): EscopoAgencia = when {
        podeVerTodasAgencias(papel) -> EscopoAgencia.Todas
        !agencia.isNullOrBlank() -> EscopoAgencia.Apenas(agencia)
        else -> EscopoAgencia.Nenhuma
    }

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