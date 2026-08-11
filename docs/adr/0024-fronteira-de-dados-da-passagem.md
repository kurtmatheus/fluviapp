# ADR-0024: A fronteira de dados da Passagem — uma coleção com discriminador, e as chaves que a consulta pede

**Status:** Aceita em direção (decisões do analista em 2026-08-11) · sem código

**Estudo que preparou:** [`docs/design/fronteira-de-dados-passagem.md`](../design/fronteira-de-dados-passagem.md)

---

## Contexto

O [ADR-0023](0023-passagem-por-categoria-e-referencia.md) reformou o **domínio** do agregado: a categoria virou
a raiz, os participantes viraram referência e nada é congelado. Este ADR responde ao passo seguinte da ordem
que o analista fixou — **domínio → fronteira → camada de dados** —, e trata só da fronteira: **como o agregado
atravessa para o Firestore e volta**.

A fronteira deste app não é uma metáfora: é um contrato de quatro peças (`DocumentoBruto`,
`CodecFirestore<T>`, `ColecaoFirestore<T>` e o `[Entidade]Documento` como *documentação, não caminho*), e ele
já foi atravessado por **sete entidades**. O estudo mediu onde a Passagem **não cabe** nele, e são três pontos:
o **volume** (a coleção inteira num `StateFlow` não serve para dado que cresce sem limite), o **polimorfismo**
(é o primeiro agregado com sub-tipos) e o **contador**, que hoje mora dentro da coleção que ele conta.

## Decisão

### D1 — Uma coleção com discriminador; o codec despacha e recusa

`passagens/{id}` continua sendo **uma** coleção, com o campo **`categoria`** discriminando o sub-domínio
(`PASSAGEIRO`, `VEICULO` e, quando vier, `CARGA`).

Não é preferência por documento homogêneo — é o que as consultas pedem: **ocupação e numeração são por
ocorrência e atravessam categoria**. Contar quem embarca numa saída é contar rede, suíte, camarote **e**
veículos; com uma coleção por categoria, cada contagem viraria N consultas somadas no cliente, a sequência
numérica de uma ocorrência ficaria repartida e a regra do servidor existiria em três versões quase iguais —
que é onde divergências nascem (a rc.3 do Porto cobrou esse preço).

O **codec passa a despachar**: lê `categoria`, escolhe o construtor, e **recusa o que não reconhece**
(`deDocumento` → `null`). É o mecanismo que a Embarcação sem tipo e a Viagem sem dia já usam — documento com
categoria ausente ou ilegível **não vira passagem de categoria padrão, não vira nada**: sai da lista pelo
`mapNotNull` e aparece na telemetria como recusa. É também o que faz a **carga** custar um valor no enum e um
ramo no `when`.

### D2 — A ocorrência são dois campos, e a data é **texto ISO-8601** `yyyy-MM-dd`

`viagemId` (`String`) + `data`. Para a data, o analista pediu *"o melhor e mais eficiente formato de
consulta"*, e a resposta é **texto ISO**, por cinco razões — a terceira sendo decisiva:

1. **igualdade exata sem normalização.** A consulta que mais roda é `data == "2026-08-18"`. Uma data de viagem
   é **data de calendário**, não instante; com `Timestamp` ela só se compara depois de normalizada à
   meia-noite de algum fuso, e qualquer deriva de um segundo — ou de fuso — quebra a igualdade em silêncio. É
   o defeito clássico de guardar calendário como instante;
2. **faixa e ordenação nativas.** ISO-8601 com zeros à esquerda ordena lexicograficamente na mesma ordem em
   que ordena cronologicamente, então `>=`, `<=` e `orderBy` funcionam sem truque — que é o que o balanço e a
   análise por período precisam;
3. **a data é o `id` do documento da ocorrência** (D6): `viagens/{viagemId}/ocorrencias/2026-08-18`. Id de
   documento é **string** — um `Timestamp` não pode ser id, e um inteiro viraria string de qualquer forma. A
   mesma chave serve nos dois lugares, e serve **igual**;
4. **lê-se no console sem tabela de conversão** — o mesmo argumento que fez `diaSemana` ser `"TUESDAY"` e não
   `2`;
5. **número só onde há conta** é a régua que a F8.1 fixou (`horaMin` em minutos do dia porque sobre ele se
   soma). Sobre `data` não se faz conta na consulta; e `20260818` como inteiro não permitiria nem somar sete
   dias, então compraria ilegibilidade sem comprar aritmética.

Sobre eficiência, o que vale dizer com precisão: no Firestore o **tipo do valor não muda o custo da consulta**
— o custo é por documento lido e por entrada de índice. Entre texto ISO e inteiro a diferença é de bytes; entre
texto ISO e `Timestamp`, a diferença é de **correção**. "Mais eficiente", aqui, é o formato que não exige
normalização, não precisa de índice extra e serve de id: é o texto ISO.

**Consequência que vai além da `data`, e corrige um defeito existente:** todo instante do agregado passa a ser
**ISO** — `criadoEm`, `alteradoEm`, `embarcadaEm`. Isto **já é** o que a Rota e a Viagem fazem
(`ISO_LOCAL_DATE_TIME`, `FormRotaViewModel.kt:140`, `FormViagemViewModel.kt:170`), e **não é** o que o carimbo
de embarque faz: ele grava `dd/MM/yyyy HH:mm` (`PassagemFirestoreRepository.kt:254`), formato que **não
ordena**. Ou seja, hoje *"quem embarcou entre as 8h e as 12h"* é uma pergunta sem resposta possível, e não por
falta de dado — por formato. Uniformizar em ISO é o que a torna respondível.

### D3 — Os clientes são um array de ids, e o titular é a posição 0

`clientes: ["cli_1", "cli_7"]`, **ordenado**, com o titular na primeira posição.

Isto **reverte** a rodada anterior desta mesma conversa, em que o titular tinha ganhado campo próprio
(`titularId` + `acompanhantesIds[]`), e a reversão tem razão de fronteira: o array único responde *"em que
passagens esta pessoa viajou?"* em **uma** consulta (`array-contains`), enquanto o par de campos exigia duas —
o Firestore não faz `OR` entre campos diferentes — ou um terceiro array derivado só para consultar. **Some o
`clienteIds[]` derivado**, que existia apenas para reparar essa divisão.

O destaque do titular não se perde porque **ele é do domínio**: o ADR-0023 D3 já diz que a lista é ordenada e
que o primeiro é o titular. A fronteira não precisa repetir a semântica em forma de campo — o Firestore
preserva a ordem do array, e a ordem *é* o significado.

### D4 — Os lançamentos são lista imutável; **não há `valorTotal`** — o total é inferido

`lancamentos: [{ id, forma, valor }]`, com `forma` gravando o `name` do enum e `valor` como `Double` na
fronteira. O `id` é **gerado no cliente e opaco** — nunca o índice na lista, que muda se a lista for reescrita
e faria a identidade mentir exatamente quando ela precisaria valer.

**Nenhum total denormalizado.** O total é **inferido** da lista, na leitura, e isso é coerente com as duas
decisões que o cercam: *preço é I/O* (a emissão não calcula, registra) e *a análise se faz sobre o agregado*.
O que se abre mão, e está aceito: **não há `orderBy` nem faixa por valor no servidor** — *"vendas acima de X"*
é varredura por período com filtro no cliente, que é como o balanço já funciona (ADR-0014 §1, e o ADR-0017
tirou o SQL de qualquer forma).

Três notas que o aprofundamento produziu e que valem como parte da decisão:

- **a consulta por forma nunca foi a consulta que existe.** Ninguém precisa de *"passagens em que houve PIX"*;
  o faturamento pede *"quanto entrou por forma no período"*, que é varredura + agregação. O custo que se
  atribuía ao array (não ser consultável por campo interno) **não incide aqui**;
- **o custo real do array também não incide**: `arrayUnion` acrescenta elemento inteiro atomicamente; o que
  exige reescrever a lista é **editar** um item — e com **emissão pós-pagamento** o lançamento é imutável, com
  o estorno morando no faturamento (ADR-0018 D12);
- **o servidor não consegue verificar a lista.** As regras do Firestore não iteram: dá para exigir que o campo
  exista, seja lista e tenha tamanho tal, mas **não** que cada item tenha forma conhecida e valor positivo.
  Com as quatro colunas de hoje uma regra podia ao menos falar de `valorPix`; com a lista, **a consistência do
  dinheiro passa a ser inteiramente do cliente**. Isso se **declara** como a casa já declara a unicidade de
  rota — em casos de emulador que passam de propósito (ADR-0011) — e se sustenta porque lançamento inválido é
  *dado ruim de quem já está autorizado a vender*, não escalonamento de privilégio.

### D5 — Lançamento ilegível recusa a passagem inteira

O `FuncionarioDocumento` estabeleceu que **item de lista ilegível some da lista** sem levar o dono junto:
*"perder o nome de quem existe seria pior do que perder um vínculo que ninguém consegue interpretar"*.

**Com lançamento, a assimetria se inverte, e a razão é o dinheiro:** descartar um item faz o bilhete valer
menos do que valeu — R$ 40 onde entraram R$ 50 —, em silêncio, e esse número vai para o balanço. Então aqui o
codec **recusa a passagem inteira**. Um bilhete que não aparece é um problema visível; um bilhete com o valor
errado é um problema invisível, e invisível é o que não se conserta.

### D6 — O contador vive em subcoleção da viagem, e **não persiste a ocorrência**

`viagens/{viagemId}/ocorrencias/{data}`, guardando o **último número** daquela saída — e nada mais.

- **por que subcoleção, e não `contadores/{viagemId}_{data}`**: a chave composta deixa de ser **texto
  concatenado** (que ninguém consulta por metade) e volta a ser **caminho**. *"As ocorrências desta viagem"* é
  a subcoleção inteira, e a regra do servidor pode exigir que a **viagem-pai exista** antes de deixar nascer um
  contador para ela;
- **como não retrocede**: `FieldValue.increment(1)`, atômico no servidor, resolve a corrida entre dois caixas —
  que **hoje não é resolvida**: o número é lido do cache local e gravado depois, com um `runBlocking` no meio
  (`PassagemFirestoreRepository.kt:116`);
- **a regra muda na mesma fatia** (dever de paridade, ADR-0011). O endurecimento atual protege *um* contador
  por `id == 'contador'` e proíbe retrocesso; passa a valer para a subcoleção;
- **o documento não persiste a ocorrência.** A `ViagemSemana` é **calculada, não persistida** (§7 do ADR-0016,
  executado na F8.4), e é por isso que a viagem pode ser imutável. O documento aqui é onde mora o **contador**:
  nasce **na primeira venda**, e a **ausência dele significa que ninguém vendeu** — não que a saída não exista.
  Quem responde *"esta saída existe?"* continua sendo o cálculo sobre `(diaSemana, hora)`.

Com isso, o documento `passagens/contador` — que nunca foi uma passagem e vivia dentro de `passagens/` com
exceção na regra por `id == 'contador'` — deixa de existir.

### D7 — `clientes/` e `veiculos/` nascem com regra e emulador na mesma fatia

As duas coleções novas entram já no regime Firestore-only, com codec e sem espelho. O que elas trazem de
diferente das sete anteriores:

- **PII**: nome, documento, nascimento e telefone; e placa é dado pessoal indireto. **Regra + suíte de
  emulador no mesmo incremento**, não depois — é a definição de pronto que a rc.3 do Porto ensinou;
- **escrita de dois direitos** (ADR-0018 D3): *criar* a entrada que não existe e *assinar* (`arrayUnion` em
  `agenciaIds`) a que existe; **corrigir conteúdo é curadoria da plataforma**. Mesmo endurecimento de
  `ehConfirmacaoEmbarque()`;
- **a consulta recortada substitui o `getListaNome()` vazio** (`PassagemFirestoreRepository.kt:168`):
  `where("agenciaIds", array-contains, agenciaId)`. Combinada com ordenação por nome, ela **exige índice
  composto** — e índice entra com a regra, não depois.

### D8 — O DTO carrega **tipo**, não texto formatado

Isto **fecha a pergunta que o [ADR-0019](0019-camada-de-dados-dinamica-e-dto-por-caso-de-uso.md) deixou
aberta**, e vale para a camada inteira, não só para a Passagem.

- **a formatação sobe para a apresentação**, que é a única camada que sabe *para quem* formata (`Locale`,
  moeda, `HH:mm`). O que morre é o padrão do `DadosPassagem`: ~58 campos, quase todos `String` já formatada,
  para uma lista que usa dez;
- **o dinheiro para de virar texto no meio do caminho** — e é isso que a análise sobre o agregado precisa:
  somar texto formatado é impossível, e refazer o parse do que o próprio app formatou é a ida-e-volta que
  produz erro de centavo;
- **datas e horas viram tipo** dentro do app (`LocalDate`, minutos do dia), com `dd/MM/yyyy` e `HH:mm` só na
  borda — a régua da F8.1, que o ADR-0023 já aplicou ao `Cliente.dataNascimento`;
- **o teste passa a comparar valor, não apresentação** — hoje um teste que compara `"18:30"` falha quando
  alguém troca o formato, o que é ruído e não defeito.

O custo é de **volume**: cada tela que hoje recebe texto pronto passa a formatar. Ele cai na F9, que é onde
esses mappers seriam reescritos de qualquer forma.

### D9 — A **consulta recortada** entra no contrato da fronteira

`ColecaoFirestore` mantém a coleção inteira num `StateFlow` e espera o primeiro snapshot antes de responder —
ótimo para dezenas de portos, ruinoso para bilhete, que cresce sem limite: seria baixar tudo o que já se
emitiu, a cada sessão, para mostrar a venda de hoje.

Então a Passagem **compõe do contrato o codec** — que é a parte valiosa: `Map` → domínio com direito de recusa
— e **não o `observarTodos`**. Em lugar dele, a fronteira ganha uma segunda peça de primeira classe: a
**consulta recortada** (por ocorrência, por agência, por período, por cliente).

O código de hoje já vivia assim sem que estivesse escrito: a Passagem é a única entidade nunca espelhada por
listener, e todas as leituras dela são consultas. Esta decisão só nomeia o que a prática já fazia — e a torna
reutilizável, em vez de reinventada por consumidor.

### D10 — O que morre nesta travessia

| Peça | Destino |
|---|---|
| `PassagemDocumento` + `toObject<PassagemDocumento>()` | vira **documentação** como as outras sete; o caminho passa a ser `Map` |
| `PassagemDao` + a tabela `Passagem` | somem (ADR-0017 F5) |
| `ContadorBilhete` (tabela) + `passagens/contador` | contador **por ocorrência**, em subcoleção (D6) |
| `PassagemDigital` (tabela) | arquivo na galeria, nome derivado do `idPassagem` (ADR-0017 D5) |
| `RascunhoPassagemEntity` | DataStore (ADR-0017 D4) |
| `deletar` + `allow delete` de `/passagens` | saem: cancelar é **estado** (ADR-0018 D17) |
| as quatro colunas de valor | lista de lançamentos (D4) |

Com as quatro primeiras linhas, **o Room fica com `Usuario` e `Constante`** — e as duas são resíduo de decisões
já tomadas. A F6 do ADR-0017 (remover o Room) passa a estar a duas remoções.

### D11 — Delete físico não existe na fronteira: só cancelamento

*(lembrete do analista, 2026-08-11 — e ele tem um fato medido por trás)*

A remoção física **não é uma operação da Passagem em nenhuma camada**: não existe no repositório, não existe na
porta que os ViewModels conhecem, e **é negada pelo servidor**. O que existe é o **cancelamento**, que é
**estado** da FSM (ADR-0018 D17/D18): cancelada não ocupa, não fatura, não renumera, e **fica com o número** —
sequência com buraco é o normal de uma numeração que registra fatos.

O estado do servidor hoje mostra o quanto isso está fora de linha: **seis coleções já declaram
`allow delete: if false`** — `users`, `rotas`, `convites`, `localidades`, `portos` e `viagens` — enquanto
`/passagens` ainda permite (`firestore.rules:414`). Entre os dados que **registram um fato**, a passagem é a
última exceção; as outras duas coleções que permitem delete são cadastros administrados pela plataforma
(`funcionarios`, `empresas/atuacoes`).

Duas consequências práticas, e a segunda é a que costuma passar batida:

- **a regra vira `allow delete: if false`**, entrando na lista das seis. Não é um `if` com condição de cargo:
  se ninguém deve apagar, a condição é `false`, e isso é auditável de relance;
- **um caso da suíte de emulador inverte de sinal.** Hoje existe, entre os 26 `skipped` de `passagens`, o caso
  *"dono deleta a própria passagem → OK"*. Ele não volta como está: passa a afirmar o oposto — **negado** —, e
  essa inversão é parte da fatia, não uma limpeza posterior. Um teste que afirma o contrário da decisão é pior
  do que teste nenhum, porque ele defende o comportamento errado.

O `ColecaoFirestore.deletar` continua existindo para quem legitimamente o usa (a atuação que a plataforma
retira). O que este ADR fixa é que **a Passagem não o compõe** — e, como ela também não compõe o
`observarTodos` (D9), o que ela toma do contrato compartilhado é exatamente o codec e a escrita.

## Consequências

**O que se ganha**

- **as chaves passam a servir a consulta**: `(viagemId, data)` responde ocupação e numeração; `clientes`
  responde o histórico de uma pessoa em uma consulta; `agenciaId` recorta por empresa **no servidor**;
- **uma pergunta que hoje não tem resposta passa a ter** — *"quem embarcou entre tal e tal hora"* —, e não por
  dado novo, por formato (D2);
- **a corrida do contador acaba** (D6), e com ela o último `runBlocking` do caminho de emissão;
- **o custo do fim do snapshot é menor do que parecia**: as entidades de referência inteiras já vivem em
  `StateFlow` na sessão, então resolver *"que porto é este id"* é **lookup em memória**, não leitura extra. O
  custo do ADR-0023 D8 é de **montagem** — o trabalho que o ADR-0019 põe no mapper de cada caso de uso.

  > **Correção do mesmo dia (2026-08-11), vinda do estudo da camada:** a frase acima vale para **empresa,
  > embarcação, localidade, porto, rota e viagem** — coleções pequenas, carregadas inteiras. **Não vale para os
  > pools `clientes` e `veiculos`**, que crescem sem limite e, pela razão do D9, não podem ser `observarTodos`.
  > A junção tem portanto **dois regimes**: *lookup em memória* para a referência e **leitura por ids em lote**
  > para os pools — com o `whereIn` do Firestore aceitando **30 valores por consulta**, o que faz `obterPorIds`
  > particionar. O que isso pesa em cada tela **não se dimensiona aqui**: os consumidores que fariam a conta
  > pesar (ocupação, balanço, análise) ainda não têm domínio planejado, e estimá-los seria somar o código de
  > hoje ao agregado de amanhã como se fosse requisito.

**O que se paga**

- **a consistência do dinheiro sai do servidor** (D4) e fica na validação e na revisão;
- **sem total denormalizado**, ordenação e faixa por valor não existem no servidor;
- **duas coleções com PII** a governar, cada uma com regra, emulador e índice;
- **volume de reescrita na apresentação** (D8);
- **documentos heterogêneos** numa coleção só, com `categoria` entrando em quase todo índice composto.

## O que este ADR não decide

- **A camada de dados** — os repositórios concretos, as portas, os mappers por caso de uso e como a consulta
  recortada se escreve. É o passo seguinte da ordem *domínio → fronteira → camada*.
- **O corte e a ordem da F9** — as perguntas 2 e 3 do estudo do terreno seguem abertas; este ADR e o 0023 são
  os insumos delas.
- **O módulo de faturamento** (ADR-0018 D12) e o **método** da inferência tarifária: aqui só se fixa que o
  total do bilhete é inferido da lista, não que método a análise usa.
- **A Carga** — a `categoria` a recebe sem reforma (ADR-0023 D9); o que ela guarda é ADR dela.

## Referências

- [ADR-0023](0023-passagem-por-categoria-e-referencia.md) — o domínio que esta fronteira serve
- [ADR-0019](0019-camada-de-dados-dinamica-e-dto-por-caso-de-uso.md) — `Map` na fronteira e DTO por caso de
  uso; a pergunta aberta dele fecha em D8
- [ADR-0018](0018-agregado-passagem-participantes-modo-e-lancamentos.md) — pools, lançamentos, cancelamento
- [ADR-0011](0011-regras-firestore-por-cargo.md) — a regra é parte da fatia, e o limite se declara em teste
- [`docs/design/fronteira-de-dados-passagem.md`](../design/fronteira-de-dados-passagem.md) — o estudo, com a
  medição do contrato de hoje e o aprofundamento do lançamento