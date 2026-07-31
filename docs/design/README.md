# Estudos de design — índice

Os documentos desta pasta são **estudos**: mapeiam o código como ele está, expõem opções e preparam
decisão. Quem decide é o analista; o que ele decide vira **ADR** em [`docs/adr/`](../adr/). Um estudo não
tem autoridade — quando ele e um ADR discordarem, **o ADR vence**.

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
| [dominio-passagem.md](dominio-passagem.md) | **base** | O agregado Passagem em detalhe — participantes, snapshot × id, ciclo de vida |
| [viagem-vs-trecho.md](viagem-vs-trecho.md) | fechado → ADR-0016 §7 | O insight que separou Trecho (par de cidades) de Rota (a viagem de verdade) |
| [dominio-relacionamentos-e-camadas.md](dominio-relacionamentos-e-camadas.md) | **superado** | Visão geral da era do ADR-0008 (ainda fala em `Agente` e relação por nome). Substituído por `dominio-da-plataforma.md` |

## Eixo 2 — Dados e persistência

Onde o dado vive, como chega e como se mantém.

| Documento | Estado | Do que trata |
|---|---|---|
| [eixo-de-storage-firestore-only.md](eixo-de-storage-firestore-only.md) | fechado → ADR-0017 | Aposentar o Room como datasource: inventário dos 11 DAOs, os dois caches em disco e o resíduo local |
| [sincronizacao-firestore-room.md](sincronizacao-firestore-room.md) | fechado → ADR-0009 | O pipeline reativo único, o ciclo de vida do listener e a porta `FonteSnapshots` |
| [balanco-passagens-mapper.md](balanco-passagens-mapper.md) | aberto | O mapper de ocupação: estrutura, threading e o que ele de fato conta |

## Eixo 3 — Apresentação

Como o app se mostra e por onde o usuário anda.

| Documento | Estado | Do que trata |
|---|---|---|
| [camada-de-apresentacao.md](camada-de-apresentacao.md) | **aberto** | A camada inteira: rotas por String, orquestração dentro da navegação, callback drilling, `@RequiresApi(S)` com `minSdk 26`, UiState com lambda |
| [fluxo-login.md](fluxo-login.md) | base | O fluxo de autenticação implementado |
| [fluxo-main-screen.md](fluxo-main-screen.md) | base | A Main Screen: bottom bar reduzida e drawer com as seções |
| [form-passagem-validacao-exibicao.md](form-passagem-validacao-exibicao.md) | aberto | O form de emissão: validação impura, desvios do molde e os bugs detectáveis |
| [cadastro-modulos.md](cadastro-modulos.md) | fechado → ADR-0006 | A análise que virou o molde de cadastro |

## Eixo 4 — Regra de negócio e relatórios

| Documento | Estado | Do que trata |
|---|---|---|
| [balanco-financeiro.md](balanco-financeiro.md) | aberto | Esperada × real × déficit, reusando o modelo de preço do ADR-0013 |

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