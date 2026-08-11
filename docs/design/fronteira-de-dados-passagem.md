# A fronteira de dados da Passagem — o que o contrato de hoje resolve, e onde ela é a primeira a não caber

> **Status:** **aberto · quase fechado** — mapeia a fronteira como ela existe em `f1cae5f` (2026-08-11) e
> expõe as decisões que a Passagem cobra dela. **Quatro das cinco perguntas foram respondidas no mesmo dia**
> (§6) e a quinta virou o aprofundamento do §4.4; sobraram dois pontos pequenos. Cada decisão está anotada na
> seção a que pertence.
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

*(atualizado em 2026-08-11 com as decisões dos §4.1 a §4.5)*

```jsonc
// passagens/{id} — PASSAGEIRO
{
  "categoria": "PASSAGEIRO",                   // discriminador (§4.1)
  "numero": "001234",
  "viagemId": "v_abc", "data": "2026-08-18",   // a ocorrência (§4.2)
  "acomodacao": "SUITE",
  "tipo": "INTEIRA",
  "titularId": "cli_1",                        // quem responde pelo bilhete (§4.3)
  "acompanhantesIds": ["cli_7"],
  "clienteIds": ["cli_1", "cli_7"],            // DERIVADO, só para consulta
  "lancamentos": [                             // lista imutável (§4.4)
    { "id": "l_7f3a", "forma": "PIX", "valor": 50.0 }
  ],
  "valorTotal": 50.0,                          // DERIVADO (§4.4) — sugestão
  "observacao": null,
  "status": "EMITIDA",
  "funcionarioId": "uid_9", "agenciaId": "emp_3",
  "criadoEm": "2026-08-11T14:32:00", "alteradoEm": "2026-08-11T14:32:00",
  "embarcadaPorId": null, "embarcadaEm": null
}

// passagens/{id} — VEICULO: o comum é idêntico, e o específico troca
{
  "categoria": "VEICULO",
  "veiculoId": "vei_44",
  "responsavelRetirada": "cli_7",              // ou ausente (é opcional por negócio)
  …
}

// viagens/{viagemId}/ocorrencias/{data} — o contador, e só ele (§4.5)
{ "ultimoNumero": 1234 }
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

> **Decidido pelo analista (2026-08-11): uma coleção com discriminador.**

### 4.2 A ocorrência: dois campos, e a data como texto ISO

`viagemId` (`String`) + `data`. Para a data, **`"yyyy-MM-dd"` como texto**, não `Timestamp`:

- **ordena e compara por intervalo** do jeito que se precisa (`>=`, `<=`, `orderBy`), porque ISO-8601 ordena
  lexicograficamente;
- **lê-se no console**, que é o mesmo argumento que fez o `diaSemana` ser `"TUESDAY"` e não `2`;
- **não se faz conta com ela** — e onde se faz conta, o app já usa número (`horaMin` em minutos do dia). Essa
  é a régua da casa, e ela decide este caso sem inventar critério novo.

Duas consequências boas: `(viagemId, data)` é uma chave composta legível, e o par serve de **id de ocorrência**
onde ele for preciso.

### 4.3 Os clientes: o titular em campo próprio, e o array chato ao lado

> **Decidido pelo analista (2026-08-11): campo próprio — "melhor para destacar".**

O titular deixa de ser *a posição 0 de um array* e passa a ser **`titularId`**, com os acompanhantes em
`acompanhantesIds[]`. O ganho é o que a decisão diz: o titular é quem responde pelo bilhete, e um campo com
nome próprio **diz isso**, enquanto uma convenção de ordem só o insinua — e convenção de ordem é do tipo que
sobrevive no código e morre na primeira consulta que reordena.

O custo aparece numa pergunta só, e ela é do pool: *"em que passagens esta pessoa viajou?"*. Com um array
único, era um `array-contains`; com o par de campos, viram **duas consultas** (uma por `titularId`, outra por
`array-contains` em `acompanhantesIds`) — e o Firestore não faz `OR` entre campos diferentes numa consulta só.

E aqui a casa já tem a resposta pronta, no molde exato: **grava-se `clienteIds[]` derivado**, chato, na mesma
escrita — `titularId` + acompanhantes. É o `Funcionario.empresaIds` outra vez, e pela **mesma** razão que o
fez nascer: *o Firestore não consulta campo de dentro de elemento de array, então a consulta pede o array
chato ao lado*. Mesma escrita, mesmo documento, sem sincronia entre coleções; nunca recebido de fora, sempre
calculado.

Fica então: **`titularId` para destacar, `acompanhantesIds[]` para completar, `clienteIds[]` para consultar** —
os dois primeiros são o fato, o terceiro é derivado e existe para o índice.

### 4.4 O lançamento — aprofundado *(a pedido do analista, 2026-08-11)*

**A pergunta "par ou evento" já tinha resposta, e é do próprio analista.** A decisão de **2026-08-01**
(`dominio-passagem.md` §11.9b) é: **lista de lançamentos, embutida, com identidade própria**, item
`{id, forma, valor}` — e *nada* de NSU, txid, taxa ou recebedor, que seria over-engineering. O `id` existe
para que promover a lista a coleção seja **mover, não redesenhar**. Duas lacunas se resolveram fora do
lançamento: *quando se pagou* virou `criadoEm` (porque **a emissão é pós-pagamento**, §11.9b′) e *quem
recebeu* é o emissor, que já está no documento.

Então o que este estudo tem a aprofundar não é a forma do domínio — é **o que essa forma custa e ganha na
fronteira**. Cinco pontos, e o terceiro é o único que muda uma linha do que estava escrito.

**(a) A consulta por forma não é a consulta que existe.** Eu havia apresentado "consultável por caminho
pontilhado" como vantagem do mapa. Revendo o que os consumidores realmente pedem, ela **não paga nada**:
ninguém precisa de *"passagens em que houve PIX"*. O que o faturamento vai pedir é *"quanto entrou em cada
forma no período"* — e isso é **varredura por período + agregação**, não filtro por documento. É como o
balanço já funciona hoje: lê as passagens e agrega em Kotlin (ADR-0014 §1), e o ADR-0017 tirou o SQL de
qualquer forma. **O array não cobra nada onde eu supunha que cobrava.**

**(b) Onde o array de objetos cobra de verdade: na escrita concorrente, e ela não existe aqui.**
`arrayUnion`/`arrayRemove` casam o **elemento inteiro**, então *acrescentar* um lançamento é atômico; o que
exige reescrever o array todo é **editar** um item — e aí dois processos simultâneos se sobrescrevem. Com
emissão pós-pagamento e lançamento **imutável** (nasce com a venda), esse caminho não existe. Se estorno
existisse dentro do bilhete, existiria — e é exatamente ele que o §11.9b′ empurrou para o faturamento. A
decisão de domínio, sem falar de Firestore, **eliminou o único custo real do array**.

**(c) O que o servidor não consegue verificar — e isto é limite novo, não sabido.** As regras do Firestore
não iteram lista: dá para exigir que o campo exista, seja lista e tenha tamanho tal, mas **não** para exigir
que *cada item* tenha uma forma conhecida e valor positivo, nem que a soma feche com nada. Com as quatro
colunas de hoje, uma regra podia ao menos falar sobre `valorPix` ser número. **Com a lista, a consistência do
dinheiro passa a ser inteiramente do cliente.**

Não é bloqueio — é um limite a declarar, e a casa já tem o precedente de como se declara: a unicidade de rota
e viagem também não é imposta pelo servidor, e vive documentada em **casos de emulador que passam de
propósito** (ADR-0011). O mesmo tratamento serve aqui, e por uma razão que vale escrever: um lançamento
inválido é **dado ruim de quem já está autenticado e autorizado a vender**, não escalonamento de privilégio.
Regra de servidor protege contra o segundo; contra o primeiro, protege validação e revisão.

**(d) A recusa do codec se inverte em relação ao vínculo — porque é dinheiro.** O precedente do
`FuncionarioDocumento` é explícito: vínculo ilegível **some da lista** sem levar a pessoa junto, porque
*"perder o nome de quem existe seria pior do que perder um vínculo que ninguém consegue interpretar"*.

Com lançamento, a assimetria **se inverte**: descartar um item ilegível faz o bilhete valer menos do que
valeu — R$ 40 onde entraram R$ 50 —, silenciosamente, e esse número vai para o balanço. Aqui o certo é
**recusar a passagem inteira** (`deDocumento` → `null`), que é o mecanismo que a Embarcação sem tipo já usa:
some da lista, aparece na telemetria como recusa, e alguém conserta. Um bilhete que não aparece é um problema
visível; um bilhete com o valor errado é um problema invisível.

**(e) O tamanho não é questão, e a rota de fuga já está desenhada.** Documento do Firestore vai a 1 MiB e uma
venda tem no máximo um punhado de lançamentos — subcoleção aqui seria estrutura para um problema que não
existe. E se um dia existir (o faturamento com conciliação e estorno), o `id` do item é o que faz a migração
ser **mover**: cada item já nasce com identidade própria.

**Nenhuma peça nova de fronteira.** `listaDeMapas` já existe no `DocumentoBruto` — nasceu para os `vinculos`
do funcionário. A forma no documento:

```jsonc
"lancamentos": [
  { "id": "l_7f3a", "forma": "PIX",      "valor": 50.0 },
  { "id": "l_9b21", "forma": "DINHEIRO", "valor": 10.0 }
]
```

`forma` grava o `name` do enum (convenção da casa) e `valor` é `Double` na fronteira (ADR-0013). O `id` é
**gerado no cliente e opaco** — não o índice na lista, que muda se a lista for reescrita e faria a identidade
mentir justamente no caso em que ela precisaria valer.

**Uma sugestão, que é decisão sua:** gravar `valorTotal` derivado ao lado da lista, na mesma escrita. É o
molde do `Funcionario.empresaIds` — *"o caso mais barato de dado derivado que existe: mesma escrita, mesmo
documento, sem sincronia entre coleções"* — e ele compra duas coisas que a lista não dá: `orderBy`/faixa por
valor **no servidor** (*"vendas acima de X"*, que hoje é impossível) e listagem sem somar N arrays no cliente.
O risco do derivado — divergir da origem — é o mesmo que lá, e se elimina do mesmo jeito: **nunca recebido de
fora, sempre calculado na escrita**.

### 4.5 A numeração por ocorrência: onde o contador vive, e como não retrocede

Três pontos, e o terceiro é o que costuma ser esquecido:

- **onde**: fora de `passagens/` (§2.3). **Decidido pelo analista (2026-08-11): subcoleção** —
  `viagens/{viagemId}/ocorrencias/{data}`, e não a coleção chata `contadores/{viagemId}_{data}`. O que a
  escolha compra: a chave composta deixa de ser **texto concatenado** (que ninguém consegue consultar por
  metade) e volta a ser **caminho**, então *"as ocorrências desta viagem"* é a subcoleção inteira, e a regra
  do servidor pode falar da viagem-pai. Custa um nível a mais na regra e uma leitura em dois passos;
- **como**: `FieldValue.increment(1)` é atômico no servidor e resolve a corrida entre dois caixas — o que
  **hoje não acontece**: o número é lido do cache local e gravado depois, com um `runBlocking` no meio
  (`PassagemFirestoreRepository.kt:116`);
- **a regra tem de mudar junto.** O endurecimento atual protege *um* contador por `id == 'contador'` e proíbe
  retrocesso. Com a chave nova, a regra passa a valer para uma **subcoleção** — e é **na mesma fatia**, pelo
  dever de paridade (ADR-0011). Um detalhe que a subcoleção facilita e a coleção chata dificultaria: como o
  caminho carrega o `viagemId`, a regra consegue exigir que a **viagem-pai exista** antes de deixar nascer um
  contador para ela.

**Uma ressalva que a subcoleção obriga a escrever, senão ela contradiz a F8.4:** a `ViagemSemana` é
**calculada, não persistida** — não existe coleção de ocorrências, e é por isso que a viagem pode ser
imutável. O documento em `ocorrencias/{data}` **não a persiste**: ele não é a fonte de que a ocorrência
existe, é onde mora o **contador** dela. Duas consequências que mantêm as duas decisões coerentes: ele nasce
**na primeira venda** (não há criação prévia de ocorrências, senão alguém teria de materializar o calendário),
e **a ausência dele não significa que a saída não existe** — significa que ninguém vendeu ainda. Quem responde
*"esta saída existe?"* continua sendo o cálculo sobre `(diaSemana, hora)` da viagem.

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

> **Decidido pelo analista (2026-08-11): o DTO carrega tipo.** Isto **fecha a pergunta que o ADR-0019 deixou
> aberta**, e não só para a Passagem: é o regime da camada.

O que a decisão implica, e vale medir antes de implementar:

- **a formatação sobe para a apresentação.** Hoje o mapper devolve texto pronto — `DadosPassagem` tem ~58
  campos, quase todos `String` já formatada, para uma lista que usa dez (é o caso que o estudo
  [dto-por-entidade-ou-caso-de-uso.md](dto-por-entidade-ou-caso-de-uso.md) usa como exemplo do preço a pagar).
  Com tipo no DTO, quem formata é a camada que sabe **para quem** está formatando: `Locale`, moeda, `HH:mm`;
- **o dinheiro para de virar texto no meio do caminho** — e isso é o que a análise sobre o agregado precisa:
  somar `String` formatada é impossível, e refazer o parse do texto que o próprio app formatou é a espécie de
  ida-e-volta que produz erro de centavo;
- **datas e horas viram `LocalDate`/minutos** dentro do app, com `dd/MM/yyyy` e `HH:mm` só na borda. É a régua
  que a F8.1 já aplicou à Viagem e o ADR-0023 acabou de aplicar ao `Cliente.dataNascimento`;
- **o teste muda de natureza, para melhor**: comparar `1830` ou `BigDecimal`/`Double` é comparar valor;
  comparar `"18:30"` é comparar apresentação, e o teste passa a falhar quando alguém troca o formato — que é
  ruído, não defeito.

O custo é real e é de trabalho: cada tela que hoje recebe texto pronto passa a formatar. Não é risco, é
volume — e é volume que aparece na F9, porque é a fase em que esses mappers são reescritos de qualquer forma.

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

## 6. As perguntas, e o que o analista respondeu (2026-08-11)

| # | Pergunta | Resposta |
|---|---|---|
| 1 | uma coleção com discriminador × uma por categoria (§4.1) | **uma coleção com discriminador** |
| 2 | o lançamento é par ou evento? (§4.4) | *"aprofunde"* → o §4.4 foi reescrito. **A resposta já existia**: a decisão de 2026-08-01 é **lista embutida de `{id, forma, valor}`**. O aprofundamento mostrou que o custo que eu atribuía ao array **não existe** aqui, e trouxe **um limite novo** (o servidor não verifica lista) e **uma inversão** (item ilegível recusa a passagem, porque é dinheiro) |
| 3 | titular na posição 0 × campo próprio (§4.3) | **campo próprio** — *"melhor para destacar"*; e o array chato `clienteIds[]` entra derivado, para a consulta |
| 4 | contador em coleção chata × subcoleção (§4.5) | **subcoleção** `viagens/{viagemId}/ocorrencias/{data}` |
| 5 | DTO com tipo × texto formatado (§4.7) | **tipo** — e isto **fecha a pergunta aberta do ADR-0019** para a camada inteira |

### O que sobrou em aberto

**Duas coisas, e as duas são pequenas** — as decisões estruturais estão todas tomadas:

1. **o `valorTotal` derivado** entra ou não (§4.4, fim). É a única sugestão minha que não foi respondida,
   porque nasceu no aprofundamento;
2. **a data como texto ISO** (§4.2) não foi contestada, então segue como proposta aceita por omissão — mas é
   bom dizer em voz alta, porque ela decide a forma de toda consulta por período.

Com isso, a fronteira está pronta para virar **ADR** — falta só a confirmação desses dois pontos e a resposta
sobre o aprofundamento do §4.4.