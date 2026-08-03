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
val SECOES_DO_PAINEL: Set<SecaoMenu> = setOf(SecaoMenu.EMPRESA, SecaoMenu.NAVIO, SecaoMenu.VIAGEM)

/**
 * A seção que **atravessa** painel e atuações — a única, e por uma razão de domínio: o supervisor gere os
 * membros de onde atua, então Equipe existe dos dois lados (ADR-0015 §2.2, ADR-0016 §2).
 */
val SECOES_TRANSVERSAIS: Set<SecaoMenu> = setOf(SecaoMenu.EQUIPE)

/**
 * As seções da família de uma atuação. Atuação dormente devolve **conjunto vazio** — não é omissão, é o
 * fail-closed do ADR-0016 §8: valor que existe no modelo mas não tem operação não ganha painel sozinho.
 */
fun secoesDa(atuacao: Atuacao): Set<SecaoMenu> = when (atuacao) {
    // O agenciamento é o segmento operante: vende passagem.
    Atuacao.AGENCIAMENTO -> setOf(SecaoMenu.PASSAGEM) + SECOES_TRANSVERSAIS
    // O transporte é dono da frota; a seção dele nasce quando o cadastro de frota existir.
    Atuacao.TRANSPORTE -> SECOES_TRANSVERSAIS
    // Portuárias dormentes (ADR-0016 §5): é onde o check-in vai morar.
    Atuacao.PORTUARIA_OPERACAO, Atuacao.PORTUARIA_ARRENDAMENTO -> emptySet()
}

/** As seções do painel da plataforma, incluindo as transversais. */
fun secoesDoPainel(): Set<SecaoMenu> = SECOES_DO_PAINEL + SECOES_TRANSVERSAIS