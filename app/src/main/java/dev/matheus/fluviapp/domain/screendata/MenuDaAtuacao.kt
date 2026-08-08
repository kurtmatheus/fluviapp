package dev.matheus.fluviapp.domain.screendata

import dev.matheus.fluviapp.domain.operacoes.Atuacao

/**
 * De qual **atuação** cada família do menu deriva ([ADR-0016] §2, 8ª rodada; ADR-0020 F3).
 *
 * O `SecaoMenu` é um enum fixo de cinco seções, e o roadmap apontava isso como achado da E2: a plataforma
 * é multi-segmento, então o menu **não é uma lista** — é *uma família por atuação, mais o painel da
 * plataforma*. É o que permite um segmento novo acordar sem tocar no modelo de permissão.
 *
 * Com [Atuacao] sendo tipo (ADR-0020 D5), a derivação vira **função pura**: testável em JVM, sem
 * Firestore e sem esperar cadastro nenhum.
 *
 * > **Estado hoje.** A `Atuacao` do logado ainda não existe em lugar nenhum — ela chega com o vínculo, na
 * > F4 (contexto e splash). Até lá o app segue pelo caminho de compatibilidade documentado em
 * > `PermissoesUsuario.secoesVisiveis`, e **nada muda em tela**. O que este arquivo entrega é a estrutura,
 * > provada por teste, para a F4 apenas ligar.
 */

/**
 * O painel da plataforma: o que `ADM`/`GESTOR` administram — as partes e os ativos.
 *
 * `VIAGEM` está aqui porque é onde ela está hoje (`PermissoesUsuario.podeAcessar`). O ADR-0016 §7 a
 * dissolve em **Rota** + **Viagem** e a passa para o agenciamento; enquanto essas entidades não existirem,
 * mover a seção seria mudar quem pode o quê sem que o cadastro correspondente exista.
 */
val SECOES_DO_PAINEL: Set<SecaoMenu> = setOf(
    SecaoMenu.EMPRESA,
    SecaoMenu.EMBARCACAO,
    // A `LOCALIDADE` é o caso mais puro desta família: capacidade da plataforma, sem dono e sem atuação
    // (ADR-0016 §5). Nenhuma agência a administra — todas a usam.
    SecaoMenu.LOCALIDADE,
    // O `PORTO` está aqui pelo mesmo motivo, e apesar de a atuação portuária existir no modelo: o cais é
    // infraestrutura (da plataforma), e o que a empresa tem nele é a **atuação**, não o porto (§5).
    SecaoMenu.PORTO,
    // Quem acessa o app e com que papel (F6.6). É o par da `EQUIPE` visto do lado da plataforma — e é
    // por ele existir que a Equipe pôde deixar de ser transversal.
    SecaoMenu.USUARIOS,
    // **Compartilhada** (ADR-0022 D2): aparece nos dois painéis, porque o pool não tem dono. Não é
    // exceção ao critério — é o critério: entidade sem dono é de todos.
    SecaoMenu.ROTA,
    SecaoMenu.VIAGEM,
)

/**
 * **Nenhuma seção atravessa mais os dois lados** (F6.6).
 *
 * A `EQUIPE` era a única, e por uma razão que parecia de domínio: o supervisor gere os membros de onde
 * atua, e a plataforma também precisava cadastrar alguém. O efeito em tela era o `ADM` abrindo o quadro
 * de pessoal de uma empresa — a plataforma não tem equipe.
 *
 * O que desfez o nó foi o **convite** (`Convite`): a plataforma passa a criar *quem entra*, com papel e,
 * quando é da operação, com empresa e cargo; a partir daí a empresa se gere sozinha. Cada painel voltou
 * a ter o seu, e o conjunto ficou vazio — que é o estado correto quando os dois contextos estão
 * separados de verdade (ADR-0015 §8.1).
 */
val SECOES_TRANSVERSAIS: Set<SecaoMenu> = emptySet()

/**
 * As seções da família de uma atuação. Atuação dormente devolve **conjunto vazio** — não é omissão, é o
 * fail-closed do ADR-0016 §8: valor que existe no modelo mas não tem operação não ganha painel sozinho.
 */
fun secoesDa(atuacao: Atuacao): Set<SecaoMenu> = when (atuacao) {
    // O agenciamento é o segmento operante: vende passagem, monta as rotas que vai ofertar (F7) e
    // cuida da própria equipe (F6.6).
    Atuacao.AGENCIAMENTO -> setOf(SecaoMenu.ROTA, SecaoMenu.PASSAGEM, SecaoMenu.EQUIPE)
    // O transporte é dono da frota; a seção dele nasce quando o cadastro de frota existir. A equipe é
    // dele também: quem opera num segmento tem quadro próprio.
    Atuacao.TRANSPORTE -> setOf(SecaoMenu.EQUIPE)
    // Portuárias dormentes (ADR-0016 §5): é onde o check-in vai morar.
    Atuacao.PORTUARIA_OPERACAO, Atuacao.PORTUARIA_ARRENDAMENTO -> emptySet()
}

/** As seções do painel da plataforma, incluindo as transversais. */
fun secoesDoPainel(): Set<SecaoMenu> = SECOES_DO_PAINEL + SECOES_TRANSVERSAIS