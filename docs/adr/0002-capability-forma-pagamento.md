# ADR-0002: Capability explícita no lugar de regra por identidade para forma de pagamento

**Status:** Aceita

**Contexto**
A habilitação da seção de forma de pagamento na passagem era decidida por um casamento de
identidade hardcoded em `DadosPassagem` (screendata):

```kotlin
val isFormaPagamentoEnabled = agencia == NAVEG.name && (agente == ODAIR.name || agente == ADRIELY.name)
```

O modelo afirmava, via `enum Agente.Nome { ODAIR, ADRIELY }`, que existem no máximo dois nomes
de agente no mundo — um atalho do deploy real (à época, só os agentes da agência da casa
podiam escolher a forma de pagamento; agências terceiras tinham faturamento fixo). A regra é
intencional, não um bug. Mas ela: (1) acopla uma *capacidade* de negócio a *identidades*
específicas; (2) mora numa derivação de screendata, não no domínio; (3) carregava dados reais
(nomes de pessoas), incompatível com a exposição pública do projeto em portfólio.

**Opções consideradas**
1. Manter a regra e apenas trocar os nomes reais por fictícios (`ODAIR`→`AGENTE_UM`, etc.).
   Descaracteriza, mas preserva o acoplamento identidade↔capacidade e a violação de OCP.
2. Mover a capacidade para uma coluna no agregado `Agente` (`@Entity` Room) e propagá-la pelo
   `PassagemDadosPassagemMapper`.
3. Expressar a capacidade como um campo booleano na própria screendata `DadosPassagem`
   (`podeSelecionarFormaPagamento`), decidido por quem materializa a `DadosPassagem`.

**Decisão**
Opção 3. `DadosPassagem` ganhou `val podeSelecionarFormaPagamento: Boolean = false` e a
derivação virou `val isFormaPagamentoEnabled = podeSelecionarFormaPagamento`. Os enums
`Agente.Nome` foram removidos; `Agencia` e `Lotacao` foram descaracterizados.

Comportamento preservado por construção: no fluxo real o `PassagemDadosPassagemMapper` **nunca
seta** `agencia`/`agente` (ficam `""`), então a regra antiga era sempre `false` fora do sample
— e o novo default `false` reproduz isso exatamente. Os `SampleData` que antes casavam a
identidade agora setam `podeSelecionarFormaPagamento = true`.

A Opção 2 foi **rejeitada por ora** deliberadamente: como o mapper real não lê `Agente` para
construir a `DadosPassagem`, uma coluna no `@Entity Agente` nasceria **morta** (nada a
consome no caminho real) e ainda custaria uma migração de schema Room (o builder é
`.build()` puro, `version = 1`, sem migração). Adicionar estrutura que o fluxo real não usa
seria over-engineering.

**Consequências**
- A capacidade é explícita e fechada para modificação: habilitar um novo agente não exige
  editar enum nem expressão booleana — basta a origem da `DadosPassagem` setar o flag.
- O domínio (`Agente`) ainda **não** é a fonte de verdade dessa capacidade; ela é decidida na
  fronteira do screendata. Fica um *seam* consciente: a "casa" (regra de quem pode) não está
  modelada no agregado, e sim em quem monta a passagem.
- Existe um segundo `isFormaPagamentoEnabled` em `FormPassagemUiState` hardcoded `= true`
  (caminho do formulário), independente deste. Não foi unificado — fora de escopo.

**Alternativas futuras**
Quando a "quem pode escolher pagamento" precisar ser dado persistido por agente (ex.: cadastro
marca o agente como habilitado), promover para a Opção 2: coluna `podeSelecionarFormaPagamento`
em `Agente`, migração Room, e wiring `PassagemDadosPassagemMapper` → `Agente` para propagar o
flag até a screendata. Esse passo se conecta ao arco maior de arquitetura de dados
(DTO-cêntrico / Room espelhando Firestore / modelo de memória) — ver ADR-0003.

**Realizado (Path B):** a `Passagem` passou a registrar `agencia`/`agente` (migração Room
v3→v4, persistidos também no Firestore) e o `PassagemDadosPassagemMapper` deriva a capability
via `AgenteRepository`. Nasce dormante: nenhum `Agente` é marcado capaz hoje (não há UI para
isso) — o robusto seria o agente virar seleção com `agenteId`, em vez de texto livre.