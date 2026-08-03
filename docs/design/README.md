# Estudos de design — índice

Os documentos desta pasta são **estudos**: mapeiam o código como ele está, expõem opções e preparam
decisão. Quem decide é o analista; o que ele decide vira **ADR** em [`docs/adr/`](../adr/). Um estudo não
tem autoridade — quando ele e um ADR discordarem, **o ADR vence**.

> O estado de vigência das **decisões** fica no [índice dos ADRs](../adr/README.md) — o que vale, o que caiu
> e por quem. Este índice cuida dos **estudos**. Ambos foram revisados em **2026-08-01**.

Os estudos estão organizados em **cinco eixos**. O eixo diz de que o documento trata; o **estado** diz
quanto ele ainda vale:

| Estado | Significa |
|---|---|
| **base** | referência viva — consultar antes de mexer no assunto |
| **aberto** | mapeado, aguardando decisão do analista |
| **fechado** | virou ADR; fica como registro do caminho até a decisão |
| **superado** | envelheceu; ler só como história, com o substituto indicado |

---

## Eixo 1 — Domínio

O que o negócio é. **É a base de qualquer transformação:** mexer em dados, tela ou regra sem passar por
aqui é mexer no efeito, não na causa.

| Documento | Estado | Do que trata |
|---|---|---|
| [dominio-da-plataforma.md](dominio-da-plataforma.md) | **base** | **O catálogo completo**: os dois contextos, o mapa de coleções, todas as entidades com seus campos, todos os enums e as regras puras. Marca o que é `[hoje]`, `[alvo]` e `[morre]` |
| [e2-painel-e-fim-do-catalogo.md](e2-painel-e-fim-do-catalogo.md) | fechado → ADR-0020 (**F1 feita, F2 parcial**) | A E2 refeita: aplicando a régua do ADR-0016 §3 sem exceção, **nenhuma categoria do catálogo sobrevive** — máscara de documento é regra (LGPD), forma de pagamento é do lançamento, atuação é da empresa. O `Catalogo` não nasce, o domínio fecha em tipos e a primeira seção depois do Painel é **Empresa** |
| [e3-catalogo.md](e3-catalogo.md) | **superado** | O mapa da frente E3: o que do catálogo já virou domínio (quase tudo), o resíduo REST do `IObjetoSimplificado` e o contador de bilhete defasado. **É a prova do doc acima** — que levou a mesma conta até o fim |
| [dominio-passagem.md](dominio-passagem.md) | **base** · §11 fechado → ADR-0018 | O agregado Passagem em detalhe — participantes, snapshot × id, ciclo de vida. O §11 traz a rodada de 2026-08-01 (pools, modo, lançamentos) decisão a decisão |
| [viagem-vs-trecho.md](viagem-vs-trecho.md) | fechado → ADR-0016 §7.1 | O insight sobre a data. **Vocabulário vencido:** o Trecho foi dissolvido; hoje são Rota, Viagem e ocorrência — o aviso no topo da nota traduz |
| [dominio-relacionamentos-e-camadas.md](dominio-relacionamentos-e-camadas.md) | **superado** | Visão geral da era do ADR-0008 (ainda fala em `Agente` e relação por nome). Substituído por `dominio-da-plataforma.md` |

## Eixo 2 — Dados e persistência

Onde o dado vive, como chega e como se mantém.

| Documento | Estado | Do que trata |
|---|---|---|
| [eixo-de-storage-firestore-only.md](eixo-de-storage-firestore-only.md) | fechado → ADR-0017 | Aposentar o Room como datasource: inventário dos 11 DAOs, os dois caches em disco e o resíduo local |
| [dto-por-entidade-ou-caso-de-uso.md](dto-por-entidade-ou-caso-de-uso.md) | **aberto** | O ponto 10 do ADR-0016 virado estudo: o app já tem as duas formas, e `DadosPassagem` (58 campos para 10 usados numa lista) é a que cobra o preço — em leitura, não em memória |
| [sincronizacao-firestore-room.md](sincronizacao-firestore-room.md) | fechado → ADR-0009 · **destino vencido** | O pipeline reativo, o ciclo de vida do listener e a porta `FonteSnapshots` — tudo de pé, menos o destino: não é mais o DAO, é um `StateFlow` (ADR-0017 D1) |
| [balanco-passagens-mapper.md](balanco-passagens-mapper.md) | aberto | O mapper de ocupação: estrutura, threading e o que ele de fato conta. **A ocupação passa a ter teto** — a capacidade vem do navio e barra a emissão (ADR-0018 D8) |

## Eixo 3 — Apresentação

Como o app se mostra e por onde o usuário anda.

| Documento | Estado | Do que trata |
|---|---|---|
| [camada-de-apresentacao.md](camada-de-apresentacao.md) | **aberto** | A camada inteira: rotas por String, orquestração dentro da navegação, callback drilling, `@RequiresApi(S)` com `minSdk 26`, UiState com lambda. Ganha um consumidor: a **emissão por etapas** (ADR-0018 F7) é a primeira tela desenhada a partir de um eixo de domínio |
| [fluxo-login.md](fluxo-login.md) | base · **vocabulário vencido** | O fluxo de autenticação implementado. Cita cargos antigos (`DIRETOR`/`COLABORADOR_MASTER`) e o pacote `model/`; e o login ganha um passo novo — **a escolha do vínculo** (ADR-0016, 8ª rodada) |
| [fluxo-main-screen.md](fluxo-main-screen.md) | base · **vocabulário vencido** | A Main Screen: bottom bar reduzida e drawer com as seções. Cita `Agente` e cargos antigos; as seções passam a **derivar da atuação** (ADR-0016 §2) |
| [form-passagem-validacao-exibicao.md](form-passagem-validacao-exibicao.md) | **superado em boa parte** | Descreve o form **antes** do molde. Validação pura, UiState puro e eventos por parâmetro **já foram feitos**; sobrevivem os achados de regra, hoje listados no §9 do estudo do agregado |
| [cadastro-modulos.md](cadastro-modulos.md) | fechado → ADR-0006 | A análise que virou o molde de cadastro |

## Eixo 4 — Regra de negócio e relatórios

| Documento | Estado | Do que trata |
|---|---|---|
| [balanco-financeiro.md](balanco-financeiro.md) | aberto · **régua vencida** | Esperada × real × déficit. O modelo de preço mudou: a base **não vem mais de tarifa cadastrada**, e sim de inferência (ADR-0016 §7.2); o eixo é a **ocorrência** e canceladas ficam de fora (ADR-0018 D9/D18) |

## Eixo 5 — Produto e entrega

| Documento | Estado | Do que trata |
|---|---|---|
| [mvp-roadmap.md](mvp-roadmap.md) | **base** | Os pilares do MVP, o que está fechado e o que falta |

---

## Como um estudo vira ADR

1. **Mapear** o estado atual no código, com arquivo e linha — sem proposta ainda.
2. **Expor as opções**, inclusive as que serão rejeitadas, com o custo de cada uma.
3. **Perguntar ao analista** no fim do documento, uma pergunta por decisão.
4. **Registrar as respostas** no próprio estudo, à medida que chegam.
5. **Escrever o ADR** com as decisões — e marcar o estudo como *fechado*, apontando para ele.