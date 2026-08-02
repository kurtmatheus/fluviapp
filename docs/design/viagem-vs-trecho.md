# Nota de arquitetura — Viagem × Trecho (a data como questão de Arquitetura da Informação)

**Status:** **Colhida pelo [ADR-0016](../adr/0016-dominio-da-plataforma.md) §7** (2026-07-28) — o rework saiu de
"futuro" e **entrou no MVP**. Esta nota fica como registro do insight que o originou; **o vocabulário final é o do
ADR**, e ele difere do desta nota num ponto essencial (ver o aviso abaixo).

> ### ⚠️ Vocabulário — leia isto antes do resto *(atualizado em 2026-08-01)*
>
> Esta nota tem **dois** conceitos (Trecho com tarifas dentro, e Viagem = Trecho + data). O desenho vigente
> tem **três**, e **nenhum deles se chama Trecho** — a 7ª rodada do ADR-0016 **dissolveu o Trecho**, porque o
> par de cidades é **derivável** dos portos e não precisava de entidade própria.
>
> | Conceito vigente | O que é | Onde vive |
> |---|---|---|
> | **Rota** | o **onde**: dois portos (as cidades vêm deles), `distanciaMn`, `tempoMedioH`. Sem tarifa, sem navio, sem agenda. | `rotas/{id}` — capacidade da plataforma, **sem dono** |
> | **Viagem** | o **quando e em quê**, e é **atômica**: `(rota, navio, diaSemana, hora)` — uma saída, um documento | `viagens/{id}` — também sem dono |
> | **Ocorrência** | a travessia concreta: **`(viagemId, data)`** | não é coleção: é a viagem materializada numa data |
>
> Três correções ao texto abaixo, todas do ADR-0016 (7ª a 9ª rodadas) e do
> [ADR-0018](../adr/0018-agregado-passagem-participantes-modo-e-lancamentos.md):
>
> 1. **Onde a nota diz "Trecho" com tarifas dentro (§2, §3, §5), não leia "Rota" — leia "Rota + Viagem".** A
>    agenda **não** ficou na Rota: `diaSemana` e `hora` são da **Viagem**, e o navio também. A Viagem é o
>    vínculo temporal entre rota e embarcação.
> 2. **A tarifa saiu de cena.** Rota e Viagem são compartilhadas por várias empresas, e *uma entidade sem dono
>    não tem de quem ter tarifa* (§7.1). A tabela cadastrada adormeceu (§7.2) e o preço passou a ser
>    **inferido** dos valores praticados. Onde a nota fala em tarifa do trecho/rota, o conceito **não existe
>    mais**.
> 3. **A previsão do §4 se confirmou, e foi além:** não é só que "várias agências operam sobre os mesmos
>    trechos" — elas compartilham **a mesma viagem**, e é isso que faz a ocupação parar de se fragmentar:
>    `count(passagens where viagemId = X and data = D)`.
>
> **Do plano do §3:** entraram os itens 1 (renomear), 2 (agenda) e 4 (a data deixa de nascer no bilhete). O
> **item 3 continua fora** — as ocorrências são **calculadas**, não persistidas, e não há contador por
> acomodação; a ocupação sai dos bilhetes. Mas a régua mudou: com a capacidade agora declarada no **navio**
> ([ADR-0018 D8](../adr/0018-agregado-passagem-participantes-modo-e-lancamentos.md)), essa contagem deixa de
> ser só relatório e passa a **barrar a emissão** quando a ocorrência lota.

> Conversa com o [estudo do domínio da passagem](dominio-passagem.md), o
> [ADR-0008](../adr/0008-relacionamentos-por-identidade.md) (relacionar por id), o
> ~~[ADR-0013](../adr/0013-tabela-de-tarifa-e-tipo-tarifario.md) (tarifa tabelada na Viagem)~~ — **dormente**,
> ver o aviso acima — e o [estudo da contagem de passagem](balanco-passagens-mapper.md).

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

## 6. Escopo — ✅ cumprido

A nota existia para: (a) travar o vocabulário; (b) evitar decisões que aprofundassem a confusão; (c) **semear o
ADR do rework quando ele começasse**. O (c) aconteceu em 2026-07-28: o rework foi absorvido pelo
[ADR-0016](../adr/0016-dominio-da-plataforma.md) §7, que é agora a fonte.

Correção do (a), para o registro: o vocabulário que esta nota travou estava **um nível incompleto** — "Trecho =
rota" juntava a linha (comum) com o preço e o embarque (da agência). O ADR separou os dois, e o nome "Rota"
passou a designar a viagem concreta. Ver o aviso no topo.