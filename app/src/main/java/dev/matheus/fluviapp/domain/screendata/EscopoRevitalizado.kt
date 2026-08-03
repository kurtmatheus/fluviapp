package dev.matheus.fluviapp.domain.screendata

import dev.matheus.fluviapp.domain.operacoes.Atuacao
import dev.matheus.fluviapp.domain.operacoes.PermissoesUsuario

/**
 * **O andaime da revitalização**: quais seções já foram refeitas na arquitetura nova — domínio isolado,
 * camada de dados, camada lógica e apresentação, cada uma testável para a entidade em foco.
 *
 * Hoje é uma só: [SecaoMenu.EMPRESA] (ADR-0020 D10, ADR-0017 F3). O app deve se comportar, do painel para
 * dentro, **como se tivesse acabado de ser implementado** e só conhecesse ela — não como um app completo
 * com pedaços quebrados. Seção fora daqui não aparece no menu, e o que ela abriria não é alcançável.
 *
 * ### Por que aqui, e não dentro da política
 *
 * O recorte **não** entra em `PermissoesUsuario.secoesVisiveis`, e a distinção é a razão deste arquivo
 * existir: aquela função responde *"quem pode ver o quê"* — regra de negócio permanente, que continua
 * valendo para Navio, Viagem e Equipe mesmo hoje. Esta responde *"o que já existe"* — fato temporário
 * sobre o estado do refactor. Misturadas, a política passaria a mentir sobre a autorização, e os testes
 * dela (`PermissoesUsuarioTest`) teriam de ser reescritos para acomodar um andaime.
 *
 * Separadas, a composição é uma interseção: o menu **nunca amplia** o que a permissão concede, só reduz.
 *
 * ### Por que interseção, e não código comentado
 *
 * Comentar as seções espalharia a decisão por navegação, drawer e ViewModel, e comentário não é
 * verificável — ninguém garante que voltou inteiro. Aqui a lista viva é um `Set` de uma linha, provado
 * por teste. **Revitalizar a próxima seção é acrescentar um valor a este conjunto**, e o andaime inteiro
 * some quando ele igualar `SecaoMenu.entries`.
 */
val SECOES_REVITALIZADAS: Set<SecaoMenu> = setOf(SecaoMenu.EMPRESA)

/** Se a seção já foi refeita ponta a ponta. */
fun estaRevitalizada(secao: SecaoMenu): Boolean = secao in SECOES_REVITALIZADAS

/**
 * O menu que o painel realmente monta: o que a política concede, **limitado ao que já existe**.
 *
 * É esta função — e não `PermissoesUsuario.secoesVisiveis` — que a Main Screen consome. A ordem da
 * composição é deliberada: a permissão primeiro (para que revitalizar uma seção nunca conceda acesso a
 * quem não o teria), o escopo depois.
 */
fun secoesDoMenu(
    papel: String?,
    cargo: String? = null,
    atuacao: Atuacao? = null,
): List<SecaoMenu> = PermissoesUsuario.secoesVisiveis(papel, cargo, atuacao).filter(::estaRevitalizada)