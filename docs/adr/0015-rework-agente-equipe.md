# ADR-0015: Agente é o usuário — Equipe, agência/lotação como capacidades, agência transversal à emissão

**Status:** Proposta — decisões **fundamentais fechadas** na conversa com o analista (ver *Decisões
resolvidas*); pontos menores abertos. Claude rascunhou. Nenhum código ainda. É o **Pilar 2** do
[`mvp-roadmap.md`](../design/mvp-roadmap.md) e responde o §6 do
[estudo do form de passagem](../design/form-passagem-validacao-exibicao.md).

> Conversa com o [ADR-0010](0010-autorizacao-por-cargo.md) (autorização por cargo; `funcionarioId = uid` já
> é o emissor), o [ADR-0008](0008-relacionamentos-por-identidade.md) (relacionar por id), o
> [ADR-0002](0002-capability-forma-pagamento.md)/[ADR-0003](0003-modelo-de-memoria-do-dado.md) (capability
> `podeSelecionarFormaPagamento`), o [ADR-0011](0011-regras-firestore-por-cargo.md) (regras Firestore), a
> nota [Viagem × Trecho](../design/viagem-vs-trecho.md) (a **ocupação das embarcações** é a dimensão que
> cruza agências) e a [identidade visual](../design/) (branding por agência; `logo1/logo2.png` já no repo).

## Contexto

Duas entidades sobrepostas modelam "quem vende":

- **`Agente`** — entidade separada (nome + `agencia` + capability `podeSelecionarFormaPagamento`), com
  `AgenteRepository` (`obterTodasAgencias`/`obterTodosAgentes`/`obterAgentesPorAgencia`). As **agências são
  um enum** (`Agente.Agencia`, ex. `MATRIZ`).
- **`Usuario`** — o logado (uid, email, nome, **cargo**). É quem de fato emite: `funcionarioId = uid`
  congelado na Passagem (ADR-0010).

Rachaduras:

- **O agente do bilhete é texto livre.** `Passagem.agencia`/`agente` são `String` snapshot, preenchidos por
  uma **área de agência do form que está comentada** (`ContentAgenciaAreaPassagemForm`); os eventos
  `onAgenciaChange`/`onAgenteChange` existem no VM mas não são plugados; `atualizarListaAgente` usa
  `runBlocking`.
- **Capability por casamento de nome (frágil).** `PassagemDadosPassagemMapper` deriva
  `podeSelecionarFormaPagamento` buscando `obterAgentesPorAgencia(agencia).firstOrNull { descricaoNome ==
  agente }` — best-effort, quebra com homônimo/rename.
- **Meta multi-agência.** A plataforma vai atender **várias agências** e exigir cadastro. Cada agência opera
  **isolada** nas suas passagens; o único ponto compartilhado é a **ocupação das embarcações**. E a emissão
  deve refletir a **identidade visual da agência** emissora.

**Diagnóstico:** o agente **é** o usuário logado. Manter `Agente` como entidade paralela + agência/agente
como texto livre no bilhete duplica identidade e fragiliza a capability. Falta **agência como capacidade do
usuário**, derivada na emissão.

## Decisão

**O agente é o usuário; o módulo "Agentes" vira "Equipe"; agência e lotação são capacidades do usuário; a
agência é transversal à emissão (sempre a do logado) e governa a identidade visual.**

### 1. Agente = Usuário; módulo "Equipe"

O membro da **Equipe** é o `Usuario`. O conceito `Agente` como entidade separada é **aposentado**; suas
capacidades migram para `Usuario`. O menu "Agentes" vira **"Equipe"** — gestão de membros (usuários com
cargo + agência + lotação), no molde de cadastro (ADR-0006).

### 2. Agência e lotação como capacidades do usuário

`Usuario` ganha:
- **`agencia`** — por ora o valor do **conjunto fixo** (`Agente.Agencia`; evolui p/ coleção cadastrável
  depois — §Decisões). Relação por valor estável agora, por id quando virar coleção (ADR-0008).
- **`lotacao`** — o **município** do membro. (Embarque/desembarque **não** são lotação: são competências do
  **Trecho** — origem/destino da rota — e serão integrados no rework Viagem→Trecho.)

Migração Room + espelho Firestore (ADR-0003) + form de cadastro do membro. A capability
`podeSelecionarFormaPagamento` migra para **atributo do usuário** (fim do casamento-por-nome).

### 3. Agência transversal à emissão — sempre a do logado

A emissão **deriva a agência do usuário logado** e a congela na Passagem (snapshot, como `funcionarioId`).
Consequências:
- **Aposenta o campo manual** agente/agência: `ContentAgenciaAreaPassagemForm` sai de vez; os eventos
  `onAgenciaChange`/`onAgenteChange` e o `runBlocking` de `atualizarListaAgente` morrem.
- **Isolamento multi-tenant:** cada agência só vê/mexe nas **próprias passagens**; nenhuma interfere nas da
  outra. O emissor não escolhe agência — é a dele.

### 4. Capability por usuário

`podeSelecionarFormaPagamento` passa a ser lida do `Usuario` (ou do cargo — ver Pontos abertos), não
derivada por nome no mapper. Remove a fragilidade e a ida ao `AgenteRepository` na leitura.

### 5. Identidade visual por agência

O bilhete/impressão resolve o **logo pela agência do emissor**. Por ora, **bundle fixo** (drawable —
`logo1/logo2.png` já no repo) mapeado por chave de agência; **Storage por agência** fica como futuro. Casa
com o `FluviWordmark` (marca do app) × logo da agência (marca do emissor).

### 6. O que cruza agências: só a ocupação das embarcações

Implicação forte das respostas: agências são **isoladas nas passagens**, mas **compartilham a ocupação das
embarcações** (a capacidade do navio é finita e comum). Logo:
- A **Contagem de Passagem** (ocupação do navio) é naturalmente **cross-agência** — todos precisam saber
  quão cheio está o barco (coerente com "operador vê a contagem geral", MVP P1.3).
- O **Faturamento** e a **gestão de passagens** são **isolados por agência**.

Isto amadurece com o rework **Viagem→Trecho** (a ocupação por embarcação/viagem é a dimensão que cruza
agências — `viagem-vs-trecho.md`).

## Plano de migração (faseado, aditivo)

- **P2.1 — Rótulo "Equipe".** Menu "Agentes" → "Equipe"; strings/títulos. Cosmético, sem migração.
- **P2.2 — `Usuario` ganha `agencia` + `lotacao`.** Migração Room + espelho Firestore + form de cadastro do
  membro (molde ADR-0006). Capability migra p/ o usuário.
- **P2.3 — Emissão deriva do logado.** Congela agência (do usuário) na Passagem; remove
  `ContentAgenciaAreaPassagemForm` + eventos/`runBlocking`; capability lida do usuário no mapper.
- **P2.4 — Identidade visual por agência.** Logo (bundle mapeado) no bilhete/impressão.

## Consequências

- **Uma identidade só** — o agente é o usuário; some a duplicação `Agente` × `Usuario`.
- **Capability robusta** — atributo do usuário, não casamento de nome.
- **Emissão mais simples** — sem campo de agência/agente; menos superfície, sem `runBlocking`.
- **Multi-tenant** — isolamento por agência nas passagens; ocupação de embarcação compartilhada.
- **Branding por agência** — o bilhete carrega a marca da agência emissora.
- **Débito de isolamento no servidor** — se o isolamento por agência precisar ser garantido (não só UI),
  entra regra Firestore (ADR-0011) — ver Pontos abertos.

## Alternativas consideradas

- **Manter `Agente` separado + texto livre (status quo)** — rejeitado: duplica identidade, capability frágil,
  não escala p/ multi-agência.
- **Agência como coleção cadastrável já no MVP** — adiado: conjunto fixo (enum) basta agora; promover a
  coleção quando exigir cadastro de agência (não só de usuário).
- **Logos no Firebase Storage por agência** — adiado: bundle fixo cobre o MVP; Storage quando houver muitas
  agências cadastrando o próprio logo.

## Decisões resolvidas na conversa (analista, 2026-07-26)

- **Lotação = município** — embarque/desembarque são competências do Trecho (origem/destino), integração
  futura (Viagem→Trecho).
- **Agência do bilhete = sempre a do usuário logado** — sem override; agências isoladas nas passagens,
  compartilham só a **ocupação das embarcações**.
- **Agência = conjunto fixo (enum) por ora** — só migra a relação p/ o usuário; evolui p/ coleção
  cadastrável depois.
- **Logos = bundle fixo mapeado por agência por ora** — Storage quando evoluir.

## Pontos abertos para a revisão (analista decide)

1. A `lotacao` (município) entra no **snapshot da Passagem** (município do emissor no bilhete) ou fica só no
   perfil do usuário?
2. Capability `podeSelecionarFormaPagamento` vira atributo **do usuário** ou função **do cargo**
   (`PermissoesUsuario`/ADR-0010)?
3. O isolamento por agência precisa de **regra Firestore** (ADR-0011) já no MVP, ou segurança-por-UI basta
   por ora?
4. `Agente`/`AgenteRepository` é **removido de vez** na migração, ou mantido **dormente** durante a transição?
5. A **Contagem de Passagem** cross-agência: soma a ocupação de **todas** as agências por navio já no MVP, ou
   isso espera o Viagem→Trecho?