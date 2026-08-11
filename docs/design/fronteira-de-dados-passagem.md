# A fronteira de dados da Passagem — o que o contrato de hoje resolve, e onde ela é a primeira a não caber

> **Status:** **aberto** — mapeia a fronteira como ela existe em `f1cae5f` (2026-08-11) e expõe as decisões
> que a Passagem cobra dela. As perguntas estão no §6.
>
> Vem depois do [planejamento de domínio](../adr/0023-passagem-por-categoria-e-referencia.md) e antes da
> camada de dados — a ordem que o analista fixou: **domínio → fronteira → camada**. Decisões-fonte:
> [ADR-0019](../adr/0019-camada-de-dados-dinamica-e-dto-por-caso-de-uso.md) (`Map` na fronteira, DTO por caso
> de uso), [ADR-0017](../adr/0017-eixo-de-storage-firestore-only.md) (Firestore-only) e
> [ADR-0011](../adr/0011-regras-firestore-por-cargo.md) (a regra é parte da fatia).

## 1. O que "fronteira" é neste app

Não é uma metáfora: é um contrato de quatro peças, e ele já foi atravessado por **sete entidades** (Empresa,
Embarcação, Localidade, Porto, Funcionário, Rota, Viagem).

| Peça | Onde | O que faz |
|---|---|---|
| `DocumentoBruto` | `firebase/DocumentoBruto.kt` | `id` + `Map<String, Any?>`, **sem nenhum tipo do Firebase** — é o que torna o mapeamento testável em JVM |
| `CodecFirestore<T>` | `firebase/ColecaoFirestore.kt:20` | o que **cada** entidade declara: nome da coleção, `deDocumento` (que pode **recusar**), `paraMapa`, `id`, `comId` |
| `ColecaoFirestore<T>` | idem `:76` | o CRUD escrito **uma vez**: listener → `StateFlow`, espera o primeiro snapshot, escrita otimista, id gerado × existente |
| `[Entidade]Documento` | `firebase/documents/` | **documentação, não caminho** — a forma do documento escrita como `data class` para se ler, enquanto o código vai por `Map` |

Os acessores tipados do `DocumentoBruto` são o vocabulário disponível: `texto`, `inteiro`, `decimal`,
`booleano` (com padrão explícito), `listaDeMapas` e `mapaDeDoubles`. Não há acessor de data — **nenhuma
entidade tem `Timestamp`**; `criadoEm` é `String` em todas.

E há três padrões de resolução que valem como precedente, porque a Passagem vai usar os três:

1. **a recusa é do codec** (`deDocumento` devolve `T?`): documento sem `rotaId` não é viagem, e sai da lista
   com `mapNotNull` sem derrubar a coleção. A telemetria conta a diferença entre recebido e gravado;
2. **denormalização deliberada quando o Firestore não consulta** — `Funcionario.empresaIds` existe ao lado de
   `vinculos` porque *`array-contains` casa o elemento inteiro, não um campo dentro dele*. É o dado derivado
   mais barato que existe: mesma escrita, mesmo documento, sem sincronia entre coleções;
3. **enum grava `name`**, não ordinal nem número — "lê-se no console sem tabela de conversão".

## 2. A Passagem é a primeira a não caber, e em três pontos

### 2.1 Volume — a Passagem não pode ser uma `ColecaoFirestore` como as outras

`ColecaoFirestore` mantém **a coleção inteira** num `StateFlow` em memória (`:85`, `:92`) e `obterTodos()`
espera o primeiro snapshot antes de responder. Isso é excelente para Empresa, Porto ou Rota — dezenas de
documentos, todos úteis, todos o tempo todo.

**Passagens crescem sem limite.** Um listener na coleção inteira significa baixar todo bilhete já emitido por
qualquer agência, a cada sessão, para mostrar a venda de hoje. Nenhuma das sete entidades anteriores tem essa
propriedade, e é por isso que o padrão não previu o caso.

O código de hoje **já sabe disso**, aliás sem nunca ter dito: a Passagem é a única entidade que nunca foi
espelhada por listener — as leituras dela são **consultas** (`obterTodasPorDataStatus`,
`obterTodasPorData`, `contarGratuidadePorViagem`, `obterDoServidorPorId`). O ADR-0017 chama isso de "a fase
mais barata" por essa razão.

**Consequência para a fronteira:** a Passagem compõe do contrato o **codec** (que é o valioso: `Map` →
domínio, com recusa) e **não** o `observarTodos`. Ela precisa de um segundo tipo de peça — a **consulta
recortada** —, e é a primeira a precisar.

### 2.2 Polimorfismo — é o primeiro agregado com sub-tipos

O ADR-0023 fez a categoria a raiz. Nenhum codec de hoje despacha por tipo: cada um mapeia uma forma só.
O codec da Passagem passa a ter de **ler o discriminador, escolher o construtor e recusar o que não
reconhece** — e a recusa tem precedente exato (`Embarcacao.tipo`, `Viagem.diaSemana`): *documento com
categoria ausente ou ilegível não vira passagem de categoria padrão, não vira nada*.

Isso é também o que torna a **carga** barata na fronteira: um valor novo no discriminador e um ramo no
`when`.

### 2.3 O contador mora dentro da coleção que ele conta

Hoje o número do bilhete vem de `passagens/contador` — **um documento que não é uma passagem, dentro de
`passagens/`** (`PassagemFirestoreRepository.kt:284`), com a regra do servidor abrindo exceção para ele por
`id == 'contador'` (`firestore.rules:388-389`).

Com um codec que recusa o que não reconhece, esse documento passa a ser **recusado** — o mecanismo funciona,
mas continuaríamos baixando-o e contando-o como recusa na telemetria. E ele muda de natureza de qualquer
forma: o ADR-0018 D10 quer numeração **por ocorrência**, então deixa de existir *um* contador.

## 3. A forma do documento que o ADR-0023 pede

Um esboço para reagir — `passagens/{id}`, com o discriminador em `categoria`:

```jsonc
// PASSAGEIRO
{
  "categoria": "PASSAGEIRO",          // discriminador (§2.2)
  "numero": "001234",
  "viagemId": "v_abc", "data": "2026-08-18",   // a ocorrência (§4.2)
  "acomodacao": "SUITE",
  "tipo": "INTEIRA",
  "clientes": ["cli_1", "cli_7"],     // ordenado: o titular é o primeiro
  "lancamento": { … },                 // §4.4 — forma ainda em aberto
  "observacao": null,
  "status": "EMITIDA",
  "funcionarioId": "uid_9", "agenciaId": "emp_3",
  "criadoEm": "2026-08-11T14:32:00", "alteradoEm": "2026-08-11T14:32:00",
  "embarcadaPorId": null, "embarcadaEm": null
}

// VEICULO — os cinco comuns idênticos, e o específico troca
{
  "categoria": "VEICULO",
  "veiculoId": "vei_44",
  "responsavelRetirada": "cli_7",      // ou ausente
  …
}
```

O que salta ao comparar com o documento de hoje (`PassagemDocumento.kt`): **o documento já tem estrutura** —
`passageiro1/2/3` e `veiculo` são sub-objetos, e a viagem congelada é um objeto embutido — enquanto **o
domínio é que é achatado** (49 campos). A fronteira de hoje faz o trabalho ao contrário do esperado: desmonta
estrutura na leitura. Com o ADR-0023 os dois se encontram no meio: o domínio ganha a estrutura, e o documento
perde as cópias.

## 4. As decisões que a fronteira cobra

### 4.1 Uma coleção com discriminador, ou uma coleção por categoria?

**Recomendo uma coleção só**, e a razão não é de gosto:

- **ocupação e numeração são por ocorrência e atravessam categoria.** Contar quem embarca numa saída é contar
  rede + suíte + camarote **e** veículos; com coleções separadas, cada contagem vira N consultas somadas no
  cliente, e a sequência numérica de uma ocorrência ficaria repartida;
- **a regra do servidor mora num lugar** em vez de três quase-iguais — e regra duplicada é onde as
  divergências nascem (a rc.3 do Porto já cobrou esse preço);
- **a carga entra sem coleção nova.**

O que se paga: documentos heterogêneos na mesma coleção, e um índice de consulta que quase sempre inclui
`categoria`. É barato, e é exatamente o que o discriminador existe para resolver.

### 4.2 A ocorrência: dois campos, e a data como texto ISO

`viagemId` (`String`) + `data`. Para a data, **`"yyyy-MM-dd"` como texto**, não `Timestamp`:

- **ordena e compara por intervalo** do jeito que se precisa (`>=`, `<=`, `orderBy`), porque ISO-8601 ordena
  lexicograficamente;
- **lê-se no console**, que é o mesmo argumento que fez o `diaSemana` ser `"TUESDAY"` e não `2`;
- **não se faz conta com ela** — e onde se faz conta, o app já usa número (`horaMin` em minutos do dia). Essa
  é a régua da casa, e ela decide este caso sem inventar critério novo.

Duas consequências boas: `(viagemId, data)` é uma chave composta legível, e o par serve de **id de ocorrência**
onde ele for preciso.

### 4.3 Os clientes: array de ids, com a ordem carregando o titular

`clientes: ["cli_1", "cli_7"]` — array **de strings**, não de objetos. E isso resolve de graça uma consulta
que o pool vai querer: *"as passagens deste cliente"* é `array-contains`, que funciona em array de valores
simples. Foi justamente por não funcionar dentro de objetos que o `Funcionario` precisou do `empresaIds`
denormalizado ao lado.

**O titular é a posição 0**, e o Firestore preserva a ordem do array. A alternativa seria `titularId` +
`acompanhantesIds[]`, que torna o titular explícito ao custo de dois campos e de duas consultas para responder
"esta pessoa viajou nesta saída". Minha leitura é que a ordem basta, porque o domínio já a definiu como
significado (ADR-0023 D3) — mas é uma escolha de fronteira, e fica registrada como pergunta.

### 4.4 O lançamento: mapa `forma → valor`, ou array de lançamentos?

Aqui a restrição do Firestore decide mais do que a estética, e ela **corta para os dois lados**:

| Forma | Consultável? | Quando é a certa |
|---|---|---|
| **mapa** `{"PIX": 50.0, "DINHEIRO": 10.0}` | **sim** — caminho pontilhado (`lancamento.PIX`) funciona em `map` | se o lançamento é só *forma → valor*, e o faturamento vai perguntar "quanto em PIX no dia" |
| **array de objetos** `[{forma, valor, …}]` | **não** por campo interno — `array-contains` casa o elemento inteiro | se o lançamento precisa de mais que forma e valor: quando, por quem, estorno |
| **subcoleção** `passagens/{id}/lancamentos` | sim, e sem limite de tamanho | se lançamento vira **evento** com vida própria (conciliação, estorno) |

O `mapaDeDoubles` do `DocumentoBruto` **já existe** (nasceu para a tabela de tarifas, que morreu) e serve o
primeiro caso sem uma linha nova de fronteira. Mas a escolha é de domínio, não de mecanismo: **ela depende de
o lançamento ser um par ou um evento** — e isso é o módulo de faturamento, que ainda não foi desenhado.
Enquanto ele não existir, o mapa é a forma que não bloqueia nada e não promete nada.

### 4.5 A numeração por ocorrência: onde o contador vive, e como não retrocede

Três pontos, e o terceiro é o que costuma ser esquecido:

- **onde**: fora de `passagens/` (§2.3). `contadores/{viagemId}_{data}` é a forma mais simples e casa com a
  chave composta do §4.2; a alternativa é a subcoleção `viagens/{id}/ocorrencias/{data}`, que agrupa melhor e
  cobra um nível a mais na regra;
- **como**: `FieldValue.increment(1)` é atômico no servidor e resolve a corrida entre dois caixas — o que
  **hoje não acontece**: o número é lido do cache local e gravado depois, com um `runBlocking` no meio
  (`PassagemFirestoreRepository.kt:116`);
- **a regra tem de mudar junto.** O endurecimento atual protege *um* contador por `id == 'contador'` e proíbe
  retrocesso. Com a chave nova, a regra passa a valer para uma coleção de contadores — e é **na mesma fatia**,
  pelo dever de paridade (ADR-0011).

### 4.6 As duas coleções novas, e o que elas cobram

`clientes/` e `veiculos/` nascem já no regime Firestore-only — sem espelho, com codec. O que elas trazem de
diferente das sete anteriores:

- **PII**: nome, documento, nascimento, telefone; e placa é dado pessoal indireto. Regra + suíte de emulador
  **no mesmo incremento**, não depois;
- **a escrita é de dois direitos** (ADR-0018 D3): *criar* o que não existe e *assinar* (`arrayUnion` em
  `agenciaIds`) o que existe. Corrigir conteúdo é curadoria da plataforma. É o mesmo endurecimento de
  `ehConfirmacaoEmbarque()`;
- **a consulta recortada substitui o `getListaNome()` vazio**: `where("agenciaIds", array-contains, agenciaId)`
  — e como ela vai combinar com ordenação por nome, **precisa de índice composto**. Índice é infraestrutura, e
  entra no repositório com a regra.

### 4.7 Os DTOs por caso de uso — e a boa notícia sobre o custo do D8

O ADR-0023 D8 tirou as cópias, e eu registrei o custo: **ocupação e balanço deixam de agregar por campo
congelado e passam a exigir junção**. O Firestore não faz junção — mas aqui o app tem uma vantagem que já
está construída: **as entidades de referência inteiras já vivem em `StateFlow`** na sessão, servidas por
`ColecaoFirestore` (rotas, viagens, portos, localidades, embarcações). Resolver "que porto é este id" é
**lookup em memória**, não leitura extra.

Ou seja: o custo do D8 não é de rede, é de **montagem** — e é exatamente o trabalho que o ADR-0019 põe no
mapper de cada caso de uso. Os cinco consumidores, e o que cada um precisa:

| Caso de uso | Lê | Precisa juntar |
|---|---|---|
| **Emissão** | a ocorrência escolhida | viagem → rota → portos (+ localidades) e embarcação — para mostrar o trajeto |
| **Consulta/listagem** | passagens da agência por data e status | nome do cliente titular (por id) |
| **Bilhete** | uma passagem | o mesmo da emissão, mais a marca da agência |
| **Ocupação** | passagens de `(viagemId, data)` | capacidade da embarcação (via viagem) |
| **Balanço** | passagens por período | a ocorrência, para agrupar |

A pergunta de fronteira que sobra daqui é a que o ADR-0019 já deixou registrada: **o DTO carrega tipo ou
`String` formatada?** Hoje o mapper formata tudo — e a análise sobre o agregado (a decisão de preço de hoje)
é o primeiro consumidor que pede **número**.

### 4.8 O que morre nesta travessia

| Peça | Destino |
|---|---|
| `PassagemDocumento` + `toObject<PassagemDocumento>()` | vira **documentação** como as outras sete; o caminho passa a ser `Map` |
| `PassagemDao` + a tabela `Passagem` | somem (ADR-0017 F5) |
| `ContadorBilhete` (tabela) + `passagens/contador` | vira contador **por ocorrência** (§4.5) |
| `PassagemDigital` (tabela) | arquivo na galeria, nome derivado do `idPassagem` (ADR-0017 D5) |
| `RascunhoPassagemEntity` | DataStore (ADR-0017 D4) |
| `deletar` + `allow delete` de `/passagens` | saem: cancelar é estado (ADR-0018 D17) |

Com as quatro primeiras linhas, **o Room fica com `Usuario` e `Constante`** — e as duas são resíduo de coisas
já decididas.

## 5. Onde a fronteira precisa crescer

Resumo do que o contrato de hoje **não** tem e a Passagem exige:

1. **consulta recortada** como peça de primeira classe, ao lado do `observarTodos` (§2.1);
2. **codec que despacha por discriminador** (§2.2);
3. **escrita atômica de contador** (`increment`) — hoje não existe nenhuma (§4.5);
4. **acessor de data**, se a data virar tipo em vez de texto (§4.2 recomenda que não vire);
5. **assinatura por `arrayUnion`** na escrita, que nenhuma coleção faz hoje (§4.6).

Os cinco são aditivos: nenhum deles muda o que as sete entidades já usam.

## 6. Perguntas ao analista

1. **Uma coleção com discriminador** (§4.1) — confirma? É a decisão que mais amarra as outras: numeração,
   ocupação e regra dependem dela.
2. **O lançamento é par ou evento?** (§4.4) — mapa `forma → valor` agora, ou já array/subcoleção porque
   estorno e conciliação vêm? A resposta pode ser *"mapa agora, e o faturamento decide depois"*, e nesse caso
   o custo da mudança é uma migração de forma dentro do documento.
3. **O titular é a posição 0 do array, ou campo próprio?** (§4.3)
4. **O contador fica em `contadores/{viagemId}_{data}` ou em subcoleção da viagem?** (§4.5)
5. **O DTO carrega tipo ou texto formatado?** (§4.7) — a pergunta aberta do ADR-0019, que a análise sobre o
   agregado agora cobra.