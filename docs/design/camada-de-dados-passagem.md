# A camada de dados da Passagem — o padrão das sete, e os cinco desvios dela

> **Status:** **aberto** — mede a camada como ela existe em `1bbcd1d` (2026-08-11) e expõe as decisões que
> faltam. As perguntas estão no §5.
>
> É o **terceiro passo** da ordem que o analista fixou: **domínio** ([ADR-0023](../adr/0023-passagem-por-categoria-e-referencia.md))
> → **fronteira** ([ADR-0024](../adr/0024-fronteira-de-dados-da-passagem.md)) → **camada**. A fronteira decidiu
> como o agregado atravessa; aqui se decide **quem o entrega às telas**, e com que forma.

## 1. O padrão das sete

As sete entidades revitalizadas (Empresa, Embarcação, Localidade, Porto, Funcionário, Rota, Viagem) têm a
**mesma anatomia**, e ela cabe em quatro linhas:

1. **uma porta** — `interface XRepository` — que é o que os ViewModels conhecem e o que os fakes substituem;
2. **um codec privado** no mesmo arquivo da porta (`private object XCodec : CodecFirestore<X>`);
3. **um repositório concreto** que **compõe** `ColecaoFirestore` em vez de herdá-la, e delega;
4. **o que é próprio da entidade fica no concreto**: subcoleção, consulta específica, delete lógico.

Duas coisas que o padrão já provou e que a Passagem vai usar:

- **a porta expressa o domínio pela ausência.** `ViagemRepository` **não tem** `editar` nem `deletar` — e o
  KDoc diz por quê: *"reescrever o horário mudaria a hora impressa em bilhetes de terceiros já emitidos"*. A
  imutabilidade não é um comentário: é um método que não existe;
- **delete lógico é uma escrita, não uma variação de apagar** (`PortoFirestoreRepository.kt:71-74`): reusa o
  `salvar` com `ativo = false`, e o listener emite o novo estado como em qualquer edição.

E o precedente de **subcoleção** já está construído: as `atuacoes` da Empresa são lidas e escritas **no
repositório concreto**, direto no `firestore`, com `runBatch` para acrescentar e remover no mesmo ato
(`EmpresaFirestoreRepository.kt:81-138`) — *"a subcoleção fica aqui, porque é da Empresa, não da coleção"*. É
exatamente onde o contador por ocorrência (ADR-0024 D6) vai morar.

## 2. A Passagem medida contra o padrão — cinco desvios

### 2.1 Ela é a única entidade **sem porta**

`PassagemFirestoreRepository` é uma **classe concreta de 272 linhas**, e está injetada em **dez lugares**:

| Onde | Arquivo |
|---|---|
| 6 ViewModels | `FormPassagemViewModel:48`, `DetalhesPassagemViewModel:32`, `EmbarqueViewModel:23`, `PesquisarPassagemViewModel:26`, `ContagemPassagemViewModel:22`, `LoginViewModel:46` |
| 4 helpers | `FormPassagemHelper:22`, `FormPassageiroHelper:14`, `FormVeiculoHelper:13`, `ImpressaoHelper:25` |

Não é estilo: é **a razão pela qual não existe teste de ViewModel de passagem**. Sem porta não há fake, e sem
fake o ViewModel só sobe com Firestore de verdade. Os 156 testes congelados cobrem tipos, validações e
mappers — nenhum cobre um ViewModel da emissão, e agora sabe-se por quê.

### 2.2 O **login** conhece a passagem

`LoginViewModel` injeta o repositório de passagem para anexar o listener do contador
(`sincronizarNumeroBilheteEmTempoReal`) — a tela de entrada do app depende do agregado de venda.

Isso **morre por consequência** da ADR-0024 D6: com o contador **por ocorrência**, em subcoleção da viagem e
incrementado atomicamente na venda, não há mais um número global a manter sincronizado desde o login. O
acoplamento não precisa ser combatido; ele **deixa de ter motivo**.

### 2.3 As leituras são consultas *ad hoc*, uma por consumidor

Não há uma peça de consulta — há quatro métodos que cada um monta a sua:

| Método | O que faz | Para quem |
|---|---|---|
| `obterTodasPorDataStatus(data, status, funcionario, agencia)` | encadeia igualdades opcionais e **espelha no Room** | listagem/consulta |
| `obterTodasPorData(data)` | devolve `Task<QuerySnapshot>` **cru**, sem mapear | contagem/ocupação |
| `contarGratuidadePorViagem(viagemId, gratuidade, excetoId)` | lê tudo da viagem e **conta em memória** | cota de gratuidade |
| `obterDoServidorPorId(id)` | leitura ao vivo, fora do cache | embarque por QR |

Três formas de retorno diferentes para o mesmo tipo de operação — lista de domínio, `Task` do Firebase e
objeto único —, e o filtro de agência por **nome**. A ADR-0024 D9 promoveu a **consulta recortada** a peça de
primeira classe; falta dar forma a ela.

### 2.4 Os mappers fazem I/O — e a casa já tem o estilo melhor

`PassagemDadosPassagemMapper` implementa `Mapper<Passagem, DadosPassagem>`, cuja interface é
`suspend fun map(entry: E): O` — e, para montar o DTO, **injeta dois repositórios** e chama `obterPorId`
(`:30`, `:35`). O mapper é, portanto, uma peça que **depende de rede** e só se testa com fake.

Ao lado, a F8 produziu o estilo oposto, e ele é melhor: **funções puras sobre listas já carregadas** —
`disponiveisAPartirDe(agora, dias)`, `inicioDoPainel(...)`, `contarOcupacaoEmbarcacao(embarcacao, passagens)`.
Testam-se em JVM sem fake nenhum, e é por isso que a F8 fechou com 516 testes verdes.

O `ContagemPassagensMapper` é o híbrido que mostra a fronteira entre os dois: uma classe com repositório
(`:20-28`) envolvendo uma **função pura** (`:48`). O que muda a classe de dentro para fora é só **quem carrega
a lista**.

### 2.5 A telemetria de emissão perde um estado, e ele era o principal

`RegistroEmissao` tem quatro desfechos: `salvaLocal`, `sincronizou`, `pendenteDeSync`, `falhou`. Os KDocs
dizem literalmente o que os dois primeiros significam: *"SUCESSO local: passagem durável no Room"* e
*"SUCESSO remoto: Firestore confirmou"*.

Sem Room, **`salvaLocal` não tem referente**. E o que ele media era o que mais importava numa bilheteria de
beira de rio: *"o bilhete está garantido mesmo se a rede cair"*. O cache do SDK dá a mesma garantia prática,
mas não é observável do mesmo jeito — a escrita otimista do `ColecaoFirestore` já aplica local e reconcilia
depois. Isto **não é detalhe de log**: é a definição de "emitido com sucesso" mudando de lugar.

## 3. As decisões da camada

### 3.1 A porta `PassagemRepository` — e o que ela **não** tem

Recomendo declará-la pelo mesmo critério das outras: a ausência é o que documenta o domínio.

```kotlin
interface PassagemRepository {
    /** Emite: cria o documento e devolve o id. Não existe "salvar por cima" — bilhete não se reescreve. */
    suspend fun emitir(passagem: Passagem): String

    suspend fun obterPorId(id: String): Passagem?
    /** Leitura ao vivo, fora do cache: o QR pode chegar num aparelho que nunca viu o bilhete. */
    suspend fun obterDoServidorPorId(id: String): Passagem?

    suspend fun consultar(criterio: CriterioPassagem): List<Passagem>

    /** Avança a FSM (ADR-0012). Cancelar é uma transição, não uma remoção (ADR-0024 D11). */
    suspend fun transicionar(id: String, novo: StatusPassagem)
    suspend fun confirmarEmbarque(id: String, operadorId: String): ResultadoEmbarque

    /** Reserva o próximo número da ocorrência — atômico no servidor (ADR-0024 D6). */
    suspend fun reservarNumero(ocorrencia: OcorrenciaViagem): Int
}
```

**Sem `editar`, sem `deletar`, sem `observarTodas`.** As duas primeiras ausências são o ADR-0018 D17 e o
ADR-0024 D11; a terceira é o ADR-0024 D9 — dado que cresce sem limite não se observa inteiro.

### 3.2 A forma da consulta recortada

Três formas possíveis, e a diferença é de manutenção:

| Forma | Como fica | Custo |
|---|---|---|
| **métodos nomeados** (hoje) | `porDataStatus(...)`, `porViagem(...)`, `porCliente(...)` | cresce um método por combinação, e a assinatura vira lista de parâmetros opcionais — foi assim que nasceu o `obterTodasPorDataStatus(data, status, funcionario, agencia)` |
| **objeto de critério** | `consultar(CriterioPassagem(ocorrencia=…, agenciaId=…, status=…))` | uma porta só; o concreto traduz critério → `Query`. Testável como dado |
| **genérico na `ColecaoFirestore`** | `colecao.consultar { it.whereEqualTo(…) }` | o lambda vaza `Query` do Firebase para quem chama, e a porta deixa de ser testável sem Firebase |

**Recomendo o objeto de critério**, por uma razão que a casa já valoriza: ele é **dado**, então a tradução
critério → consulta é uma função pura testável em JVM, e a porta não vaza Firebase. O terceiro caminho é o que
parece mais flexível e é o único que quebra a testabilidade que o `DocumentoBruto` foi criado para garantir.

### 3.3 Onde a junção mora — e o que acontece com o `Mapper`

Com o ADR-0023 D8 (nada congelado), montar qualquer DTO exige juntar. Duas formas:

- **como hoje**: o mapper injeta repositórios e busca o que falta. Simples de chamar, impossível de testar sem
  fake, e esconde N leituras dentro de um `map()`;
- **como a F8**: uma **função pura** recebe a passagem e as listas de referência já carregadas, e devolve o
  DTO. Quem carrega é o ViewModel (ou um coletor no repositório), e as listas **já estão em memória** — as
  entidades de referência vivem em `StateFlow` (ADR-0024, consequências).

**Recomendo o segundo**, e com ele uma consequência de forma: a interface `Mapper<E, O>` (`util/Mapper.kt`)
**não serve** — ela é `suspend` e 1→1, e uma junção pura tem várias entradas e nenhuma suspensão. Ela não
precisa ser substituída por outra abstração: uma função de topo com nome bom (`paraBilhete(...)`,
`paraLinhaDeConsulta(...)`) é o que a F8 usou, e é mais legível do que uma interface genérica.

### 3.4 Os DTOs, agora com tipo

O ADR-0024 D8 decidiu **tipo, não texto**. O que isso faz com o que existe:

`DadosPassagem` tem ~58 campos, quase todos `String` formatada — inclusive `idPassageiro1 = ""` e
`idVeiculo = ""`, campos que existem e são preenchidos com vazio. Ele serve **quatro** consumidores ao mesmo
tempo (bilhete, detalhes, consulta, impressão), e é por isso que tem 58 campos: é a união de quatro
necessidades.

Com DTO por caso de uso, ele se divide em quatro projeções pequenas, e cada uma carrega tipo:
`BilhetePassagem`, `LinhaDeConsulta`, `OcupacaoDaOcorrencia`, `LinhaDeBalanco`. A formatação
(`formataParaMoedaBrasileira`, `extrairDocumentoFormatado`, `rotulo()`) sai do mapper e vai para a camada de
apresentação.

### 3.5 O contador, os pools, o rascunho e o bilhete

- **contador**: no repositório concreto, como as `atuacoes` — `viagens/{id}/ocorrencias/{data}` com
  `FieldValue.increment(1)`. A porta expõe só `reservarNumero(ocorrencia)`: quem chama não precisa saber que
  existe subcoleção;
- **pools `Cliente` e `Veiculo`**: duas portas novas no molde das sete (porta + codec + `ColecaoFirestore`),
  com **uma operação que nenhuma outra tem** — *criar ou assinar*: tenta criar, e se já existe faz `arrayUnion`
  na assinatura (ADR-0018 D3). São **duas escritas no pior caso**, e isso já estava previsto;
- **rascunho**: a porta `RascunhoStore` **já existe** (ADR-0004) e o Room está atrás dela
  (`RascunhoPassagemStoreRoom`). Trocar por DataStore é **uma implementação nova e nada mais** — é o dividendo
  de ter isolado o mecanismo três ADRs atrás;
- **bilhete digital**: `PassagemDIgitalRepository` (13 linhas, com o typo no nome) indexa arquivos numa tabela.
  Com o arquivo na galeria e o nome derivado do `idPassagem` (ADR-0017 D5), **o índice não é substituído: ele
  deixa de existir**.

### 3.6 A telemetria depois do Room

`salvaLocal` perde referente (§2.5). Duas opções honestas:

- **remover o estado** e passar a registrar *emitida* (o `set` aplicado no cache, que é o que o usuário vê) e
  *sincronizada* (ack do servidor) — dois desfechos em vez de três;
- **renomear** para `aplicadaLocalmente`, mantendo três desfechos e deixando explícito que o local agora é o
  cache do SDK, não um banco nosso.

Recomendo o primeiro, porque o terceiro estado só existia para distinguir *dois* mecanismos de persistência —
e depois da F9 há um só.

## 4. O que morre na camada

| Peça | Por quê |
|---|---|
| `PassagemDao`, `ContadorDao`, `PassagemDigitalDao`, `RascunhoPassagemDao` | o Room sai (ADR-0017 F5) |
| `RascunhoPassagemStoreRoom` | trocado por impl DataStore — **a porta fica** |
| `PassagemDIgitalRepository` | o índice local do bilhete não tem substituto (ADR-0017 D5) |
| `PassagemDadosPassagemMapper` + `DadosPassagem` | divididos em quatro projeções tipadas (§3.4) |
| `Mapper<E, O>` (para este uso) | junção pura tem várias entradas e não suspende (§3.3) |
| `getListaNome()` | substituído pela consulta recortada do pool `Cliente` |
| o listener do contador no `LoginViewModel` | o contador vira por ocorrência (§2.2) |
| `obterTodasPorData` devolvendo `Task<QuerySnapshot>` | a porta devolve domínio, não tipo do Firebase |

## 5. Perguntas ao analista

1. **A consulta recortada é objeto de critério?** (§3.2) — é a decisão mais estrutural desta rodada: ela define
   a porta e o que é testável em JVM.
2. **A junção vira função pura com as listas por parâmetro** (§3.3), com o ViewModel carregando? É o estilo da
   F8, e leva embora a interface `Mapper`.
3. **Quatro projeções tipadas no lugar do `DadosPassagem`** (§3.4) — os nomes e o recorte servem, ou você
   prefere outro corte de consumidores?
4. **A telemetria fica com dois desfechos ou com três renomeados?** (§3.6)
5. **A porta `PassagemRepository` do §3.1 está completa?** Em especial: falta algo para o **cancelamento** além
   de `transicionar`, já que ele exige política de quem pode (ADR-0018 D17)?