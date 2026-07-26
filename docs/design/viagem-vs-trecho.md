# Nota de arquitetura — Viagem × Trecho (a data como questão de Arquitetura da Informação)

**Status:** Rascunho — Claude capturou o insight do analista (2026-07). Semente para o **rework "Viagem vira
Trecho"** (futuro). Não implementa nada; registra a distinção de domínio e a direção, para não repetir a
confusão e para o rework nascer com o vocabulário certo.

> Conversa com o [estudo do domínio da passagem](dominio-passagem.md), o
> [ADR-0008](../adr/0008-relacionamentos-por-identidade.md) (relacionar por id), o
> [ADR-0013](../adr/0013-tabela-de-tarifa-e-tipo-tarifario.md) (tarifa tabelada na Viagem) e o
> [estudo da contagem de passagem](balanco-passagens-mapper.md).

---

## 1. A confusão da data era Arquitetura da Informação, não bug

Ao longo do trabalho apareceu uma "confusão da data": a **data/hora não moram numa entidade de viagem** —
elas são fixadas **no ato da emissão da passagem** (`Passagem.dataViagem`/`horaViagem`, snapshot por valor),
e o balanço/contagem consulta os bilhetes **por data** (`obterTodasPorData`), não uma agenda de viagens.

A causa não é um campo mal colocado; é que **a entidade que hoje se chama `Viagem` não é uma viagem** — é o
que o negócio chama de **Trecho**. Ela não tem data porque um trecho, por natureza, não tem data.

## 2. O vocabulário correto

| Conceito de negócio | O que é | Onde vive hoje |
|---|---|---|
| **Trecho** | A rota operada: **quem opera** (empresa/agência), **qual navio**, **de onde sai**, **para onde vai**, e a **tabela de tarifas**. Sem data. | É a entidade que hoje se chama `Viagem` (empresaId/navioId/origem/destino + `TarifaViagem`). |
| **Viagem** | Um Trecho **com data e hora marcadas**. É a ocorrência concreta. | **Não existe como entidade.** Hoje a data/hora são carimbadas no bilhete na emissão. |

Ou seja: o `ViagemDadosViagemMapper`, o `TarifaViagem`, o `codigoViagem` — tudo que hoje chamamos de
"Viagem" é, no domínio, o **Trecho**. O que falta modelar é a **Viagem** de verdade (Trecho + quando).

## 3. O rework proposto (futuro) — "Viagem vira Trecho"

Quando o rework acontecer:

1. **`Viagem` → `Trecho`.** A entidade atual é renomeada/reposicionada como Trecho (rota + tarifas, sem
   data). Toda a relação por id (ADR-0008) e a tabela de tarifas (ADR-0013) já estão prontas para virar
   atributos do Trecho.
2. **Trecho ganha agenda semanal.** **Dias da semana** em que opera + **hora correspondente por dia**
   (um trecho pode sair 08:00 na segunda e 14:00 no sábado).
3. **Viagens passam a ser geradas.** As **viagens da semana** são derivadas da agenda do Trecho — cada uma
   uma ocorrência concreta (Trecho + data + hora), com um **contador por acomodação** (ocupação vive na
   viagem, não recontada a cada consulta de bilhetes).
4. **A data deixa de nascer no bilhete.** A emissão passa a **selecionar uma Viagem** (ocorrência já datada)
   em vez de carimbar data/hora à mão — a confusão do §1 se dissolve na origem.

## 4. Por que isso importa para o multi-agência

Com Trecho + agenda + viagens-geradas, a **gestão da informação em alto nível fica mais eficiente e os dados
se cruzam entre agências**:

- O Trecho é o ponto de encontro: várias agências operam/vendem sobre os mesmos trechos; a agenda e as
  tarifas são do trecho, não copiadas por bilhete.
- O **contador por acomodação na viagem** dá ocupação em O(1) de leitura (sem reagrupar bilhetes por data a
  cada balanço — ver [contagem de passagem](balanco-passagens-mapper.md) §3).
- Cruzar dados entre agências (ocupação de um trecho somando o que cada agência vendeu) vira uma agregação
  sobre viagens, não um varredura de bilhetes espalhados. Casa com a rework do agente/Equipe (multi-tenant)
  e com o branding por agência na emissão ([[project_identidade_visual_fluviapp]]).

## 5. Impacto no que já existe (a vigiar no rework)

- **Snapshot da Passagem.** Hoje o bilhete congela empresa/navio/origem/destino/código + data/hora. Com a
  Viagem datada, o bilhete passa a apontar para a Viagem (id) e herdar a data dela — mantendo o snapshot por
  valor para histórico (ADR-0008).
- **`codigoViagem`.** Derivado do navio/trecho hoje; com Trecho+Viagem, revisar de onde o código nasce.
- **Balanço/contagem por `dataViagem`.** Passará a ser por Viagem (ocorrência), não por string de data.
- **Cota de gratuidade por viagem (ADR-0013).** Hoje "por viagem" = por `viagemId` do Trecho; com a Viagem
  datada de verdade, a cota fica mais precisa (por ocorrência).

## 6. Escopo

Este é um **rework grande e futuro** — não faz parte do incremento atual. A nota existe para: (a) travar o
vocabulário (Trecho = rota; Viagem = rota datada); (b) evitar decisões que aprofundem a confusão; (c) semear
o ADR do rework quando ele começar. Nada aqui é código a escrever agora.