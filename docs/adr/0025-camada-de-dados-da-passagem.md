# ADR-0025: A camada de dados da Passagem — porta, critério e a tradução que não busca

**Status:** Aceita em direção (decisões do analista em 2026-08-11) · sem código

**Estudo que preparou:** [`docs/design/camada-de-dados-passagem.md`](../design/camada-de-dados-passagem.md)

---

## Contexto

Terceiro e último passo da ordem que o analista fixou para a Passagem: **domínio**
([ADR-0023](0023-passagem-por-categoria-e-referencia.md)) → **fronteira**
([ADR-0024](0024-fronteira-de-dados-da-passagem.md)) → **camada**. O domínio disse o que o agregado é; a
fronteira, como ele atravessa; aqui se decide **quem o entrega às telas**.

As sete entidades revitalizadas já têm uma anatomia comum — **porta** (`interface XRepository`), **codec
privado**, **repositório concreto compondo `ColecaoFirestore`**, e o que é próprio da entidade no concreto. O
estudo mediu a Passagem contra ela e encontrou cinco desvios. O primeiro explica um vazio antigo: **ela é a
única entidade sem porta**, com a classe concreta de 272 linhas injetada em **dez lugares** — e é por isso que
**não existe teste de ViewModel de passagem**, porque sem porta não há fake.

Uma restrição de método atravessa este ADR e está declarada no fim: **nada aqui se justifica por desempenho
projetado**. Os consumidores que fariam essa conta pesar — ocupação, balanço, análise — **não têm domínio
planejado**, e estimá-los seria somar o código de hoje ao agregado de amanhã como se fosse requisito.

## Decisão

### D1 — Nasce a porta `PassagemRepository`, e ela se define pelas ausências

```kotlin
interface PassagemRepository {
    /** Emite: cria o documento e devolve o id. Não existe "salvar por cima". */
    suspend fun emitir(passagem: Passagem): String

    suspend fun obterPorId(id: String): Passagem?
    /** Ao vivo, fora do cache: o QR pode chegar num aparelho que nunca viu o bilhete. */
    suspend fun obterDoServidorPorId(id: String): Passagem?

    suspend fun consultar(criterio: CriterioPassagem): List<Passagem>

    /** Avança a FSM. Cancelar é transição, não remoção. */
    suspend fun transicionar(id: String, novo: StatusPassagem)
    suspend fun confirmarEmbarque(id: String, operadorId: String): ResultadoEmbarque

    /** Reserva o próximo número da ocorrência — atômico no servidor (ADR-0024 D6). */
    suspend fun reservarNumero(ocorrencia: OcorrenciaViagem): Int
}
```

**Sem `editar`, sem `deletar`, sem `observarTodas`** — e as três ausências são decisões anteriores tomando
forma de código: bilhete não se reescreve, **delete físico não existe** (ADR-0024 D11) e dado que cresce sem
limite não se observa inteiro (ADR-0024 D9). É o mesmo recurso que o `ViagemRepository` usa para dizer que a
viagem é imutável: **a imutabilidade não é um comentário, é um método que não existe**.

Duas notas sobre o que a porta **não** carrega:

- **o cancelamento não ganha método próprio** porque é uma transição (`transicionar(id, CANCELADA)`), e *quem
  pode cancelar* é da política (`PermissoesUsuario`, [ADR-0010](0010-autorizacao-por-cargo.md)), consultada
  **antes** da chamada. Uma porta que perguntasse "posso?" criaria uma segunda fonte de autorização, e o
  ADR-0010 existe para haver uma;
- **`reservarNumero` esconde a subcoleção.** Quem chama não precisa saber que existe
  `viagens/{id}/ocorrencias/{data}` — isso é do repositório concreto, como as `atuacoes` são da Empresa.

**Consequência colateral, e ela é grande:** com a porta, o ViewModel da emissão passa a ser testável em JVM
pela primeira vez. E o **`LoginViewModel` deixa de conhecer a passagem** — ele só a injetava para anexar o
listener do contador global, que morreu no ADR-0024 D6. O acoplamento não é combatido: **deixa de ter motivo**.

### D2 — A consulta recortada é um **objeto de critério**

`consultar(criterio: CriterioPassagem)` — e não um método por combinação de filtros, nem um lambda recebendo
`Query`.

O que decide entre os três é a testabilidade: **critério é dado**, então traduzir critério → consulta é uma
função pura verificável em JVM, e a porta não vaza tipo do Firebase. O lambda com `Query` parece o mais
flexível e é o único caminho que quebra a garantia que o `DocumentoBruto` foi criado para dar — mapeamento e
consulta testáveis sem Firebase. Métodos nomeados, por sua vez, crescem um por combinação: é assim que se
chegou ao `obterTodasPorDataStatus(data, status, funcionario, agencia)`.

**E há um ganho de expressão que vem de graça:** hoje `agencia = ""` significa *"sem recorte"* — uma string
vazia carregando a semântica mais perigosa daquele método, que é *ver tudo*. Com um tipo, **"sem recorte" deixa
de ser um vazio e passa a ser um valor com nome**, como o `EscopoDoPool` fez na F8 e pela mesma razão.

### D3 — A tradução não busca: coletar é da camada de dados, traduzir é do domínio

A junção que o fim do snapshot (ADR-0023 D8) exige passa a ser **função pura**: recebe a passagem e as
referências já carregadas, devolve o DTO. Quem carrega é quem chama — ViewModel ou um coletor no repositório.

Quatro propriedades sustentam a escolha, e **todas são verificáveis no código de hoje**:

1. **testar deixa de exigir fake** — entram objetos, sai objeto;
2. **acaba a contaminação por `suspend`** — traduzir não é buscar;
3. **o carregamento fica visível.** Hoje quem lê `passagens.map { mapper.map(it) }` não vê que cada volta faz
   duas buscas, porque a busca mora dentro da tradução. Não se trata de ficar mais rápido: trata-se de **o que
   for lento ser lento à vista**;
4. **a dependência volta a apontar para dentro** — `domain/mappers/` deixa de importar
   `services/repository/`, que é uma das dívidas que o estudo do domínio da plataforma já registrava.

O desenho tem um precedente pronto no próprio repositório: o `ContagemPassagensMapper` é uma classe com
repositório envolvendo a função pura `contarOcupacaoEmbarcacao`. Esse híbrido **não é um meio-termo tímido —
é a divisão certa**, e agora é regra.

**A interface `Mapper<E, O>` sai** (para este uso): ela é `suspend` e 1→1, e uma junção pura tem várias
entradas e nenhuma suspensão. Não é substituída por outra abstração — uma função de topo com nome bom é o que
a F8 usou, e é mais legível que uma interface cujo único método se chama `map`.

**A junção tem dois regimes**, e isto corrige por escrito uma afirmação do ADR-0024:

| O que se junta | Como | Por quê |
|---|---|---|
| empresa, embarcação, rota, porto, localidade, viagem | **lookup em memória** sobre o `StateFlow` da sessão | coleção pequena, útil inteira |
| **cliente, veículo** | **leitura por ids, em lote** | pool que cresce sem limite — pela mesma razão do D9, não se observa inteiro |

O `whereIn` do Firestore aceita **30 valores por consulta**, então a leitura por ids particiona. Isso é
mecanismo de plataforma; **o que pesa em cada tela não se dimensiona aqui** (ver *o que este ADR não decide*).

### D4 — DTO por consumidor: fica o critério, não a lista

`DadosPassagem` tem ~58 campos, quase todos texto formatado, e serve **quatro** consumidores ao mesmo tempo —
por isso é grande: é a **união** de quatro necessidades. O sintoma está no arquivo: `idPassageiro1 = ""` e
`idVeiculo = ""`, campos que existem e são preenchidos com vazio porque *algum* consumidor talvez os queira.

**O corte é por consumidor** — cada projeção responde a uma pergunta, e um campo só existe se aquela pergunta o
exigir. O que isso rejeita é o corte **por entidade** (uma `PassagemDto` com tudo), que é exatamente o que
produziu os 58 campos.

**Quais projeções existem não se decide aqui.** Nomear a projeção de um consumidor é decidir de que campos ele
precisa antes de saber o que ele é — e ocupação, balanço e análise **não têm domínio planejado**. Cada projeção
nasce **com o seu consumidor**, na mesma ordem que este trabalho seguiu.

Com tipo no DTO (ADR-0024 D8), a formatação — `formataParaMoedaBrasileira`, `extrairDocumentoFormatado`,
`rotulo()` — sai da camada de dados e vai para a apresentação, que é a única que sabe *para quem* formata.

### D5 — A telemetria mantém três desfechos, renomeados

`RegistroEmissao` tem `salvaLocal`, `sincronizou`, `pendenteDeSync` e `falhou`, e os KDocs dos dois primeiros
dizem *"durável no Room"* e *"Firestore confirmou"*. Sem Room, o primeiro perde referente.

**Os três desfechos ficam, com nome novo** — e a razão é melhor do que a que eu havia proposto (suprimir um):
o desfecho local **não mede qual banco gravou**; mede **o que o operador pode afirmar ao passageiro** antes de
a rede confirmar. O cache do SDK dá essa garantia como o Room dava; muda só o nome do lugar. Suprimi-lo
apagaria a distinção que mais importa numa bilheteria de beira de rio: **aceito aqui × confirmado no
servidor**.

| Desfecho | Significa |
|---|---|
| **`aplicadaLocalmente`** | o `set` entrou no cache do SDK e o bilhete já vale — o SDK reconcilia |
| `sincronizou` | ack do servidor |
| `pendenteDeSync` | o servidor recusou ou está fora — **degradado, não fatal** |
| `falhou` | desfecho que impede a emissão |

### D6 — Os pools ganham portas no molde das sete, com uma operação nova

`Cliente` e `Veiculo` entram como as outras — porta + codec privado + `ColecaoFirestore` —, com duas
diferenças que já estavam decididas e agora têm lugar:

- **criar ou assinar** (ADR-0018 D3): tenta criar a entrada; se já existe, faz `arrayUnion` na assinatura
  (`agenciaIds`). São **duas escritas no pior caso**, e é a única operação do app que funciona por tentativa e
  queda;
- **`obterPorIds(ids)`** em lote, que é o segundo regime da junção (D3). Nenhuma das sete precisou disso,
  porque nenhuma delas cresce sem limite.

A consulta recortada do pool (`array-contains` em `agenciaIds`) **substitui o `getListaNome()` que devolve
lista vazia** — a cova que o ADR-0018 D2/D3 preenche.

### D7 — O que morre na camada

| Peça | Por quê |
|---|---|
| `PassagemDao`, `ContadorDao`, `PassagemDigitalDao`, `RascunhoPassagemDao` | o Room sai (ADR-0017 F5) |
| ~~`RascunhoPassagemStoreRoom`~~ | **revisto no mesmo dia — não é trocado.** Ver a nota abaixo |
| `PassagemDIgitalRepository` | o índice local do bilhete **não tem substituto**: o arquivo vai para a galeria com nome derivado do `idPassagem` (ADR-0017 D5) |
| `PassagemDadosPassagemMapper` + `DadosPassagem` | projeções por consumidor (D4) |
| `Mapper<E, O>` neste uso | junção pura tem várias entradas e não suspende (D3) |
| `getListaNome()` | consulta recortada do pool (D6) |
| o listener do contador no `LoginViewModel` | contador por ocorrência (ADR-0024 D6) |
| `obterTodasPorData` devolvendo `Task<QuerySnapshot>` | a porta devolve domínio, não tipo do Firebase |

O **rascunho é o dividendo visível de ter isolado o mecanismo**: a porta nasceu no ADR-0004, vinte decisões
antes de haver motivo para mexer nela — e é ela que absorve a mudança que veio horas depois.

> **Revisão do mesmo dia (2026-08-11), por decisão do analista.** O rascunho **não vai para o DataStore**, e
> isso derruba tanto a linha acima quanto o **D4 do [ADR-0017](0017-eixo-de-storage-firestore-only.md)**. A razão
> é que ele **deixou de ser resíduo**: um snapshot passa a ser uma **passagem incompleta**, com **vários por
> agente** e uma **tela de recuperação** — *"com garantia do Room"*. Cai também o **slot único** do
> [ADR-0004](0004-snapshot-e-observabilidade-emissao.md).
>
> Consequência de eixo, e ela é maior que a linha: **o Room não morre inteiro na F9**. O Firestore-only vale para
> o que é **fato compartilhado**; o atendimento em curso é local por natureza — e é por ser local que ele
> sobrevive a app fechado e rede ausente. O ADR-0017 F6 (remover o Room) passa a ter um habitante com razão de
> ser, além de `Usuario` e `Constante`.
>
> **A passagem incompleta não é uma `Passagem` com campos nulos**: o ADR-0023 D1 fez o agregado não se construir
> sem ocorrência, lançamento e metadados, e admitir nulos para servir ao incompleto desfaria o D1 por dentro. É
> **outro tipo**, que se **promove** a `Passagem` quando fecha. O desenho está em
> [`docs/design/apresentacao-passagem.md`](../design/apresentacao-passagem.md) §6, com três pontos ainda a
> decidir.

## Consequências

**O que se ganha**

- **o ViewModel da emissão fica testável** pela primeira vez (D1), e com ele a maior tela do app;
- **a autorização continua tendo uma fonte só** (D1), porque a porta não opina sobre permissão;
- **"ver tudo" deixa de ser uma string vazia** (D2);
- **o que a chamada custa deixa de ser invisível** (D3) — e o domínio para de importar a camada de dados;
- **cada campo de DTO passa a ter dono**: um consumidor que o pediu (D4).

**O que se paga**

- **quem chama carrega**: as buscas que estavam escondidas na tradução aparecem no ViewModel ou no coletor. Fica
  mais explícito e mais verboso;
- **duas escritas no pior caso** ao salvar participante (D6);
- **volume de reescrita**: dez pontos de injeção passam a falar com a porta, e a formatação migra para a
  apresentação (ADR-0024 D8);
- **um teste a reescrever por projeção** — os que hoje afirmam a forma antiga afirmam contra a decisão.

## O que este ADR não decide

- **Desempenho e dimensionamento.** Nada aqui se justifica por custo projetado, e a razão é de método
  *(correção do analista, 2026-08-11)*: somar o código que existe ao agregado planejado produz um número que
  parece medição e é projeção — e ele viraria requisito sem nunca ter sido decidido. Quando o consumidor
  existir, a conta se faz com ele na mão.
- **Quais projeções existem** (D4) — e, por consequência, **o domínio de ocupação, balanço e análise**, que
  ainda não foi planejado. A tarifa inferida segue no módulo de faturamento (ADR-0018 D12).
- **O corte e a ordem da F9** — as perguntas 2 e 3 do estudo do terreno seguem abertas; os ADRs 0023, 0024 e
  este são os insumos delas.
- **A camada de apresentação.** A formatação chega nela por D8 do ADR-0024, mas como as telas se organizam é
  matéria do estudo da apresentação.

## Referências

- [ADR-0023](0023-passagem-por-categoria-e-referencia.md) e [ADR-0024](0024-fronteira-de-dados-da-passagem.md) —
  o domínio e a fronteira que esta camada serve
- [ADR-0019](0019-camada-de-dados-dinamica-e-dto-por-caso-de-uso.md) — DTO por caso de uso; D4 aplica o critério
- [ADR-0010](0010-autorizacao-por-cargo.md) — a política que a porta **não** duplica
- [ADR-0004](0004-snapshot-e-observabilidade-emissao.md) — a porta do rascunho, criada antes de haver motivo, e
  que agora paga
- [`docs/design/camada-de-dados-passagem.md`](../design/camada-de-dados-passagem.md) — o estudo, com os cinco
  desvios medidos e os dois estilos de junção em código real