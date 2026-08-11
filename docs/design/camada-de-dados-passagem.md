# A camada de dados da Passagem — o padrão das sete, e os cinco desvios dela

> **Status:** **aberto · pronto para virar ADR** — mede a camada como ela existe em `1bbcd1d` (2026-08-11) e
> expõe as decisões que faltavam. **As cinco foram respondidas no mesmo dia** (§5); a segunda virou o
> aprofundamento do §3.3, que **corrigiu uma afirmação do ADR-0024** — a junção tem dois regimes, e o *lookup em
> memória* não cobre os pools.
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

> **Decidido pelo analista (2026-08-11): objeto de critério.**
>
> Uma consequência que vale antecipar: o critério é **dado**, então ele pode ser **validado** e **explicado**.
> A consulta de hoje aceita `agencia = ""` como *"sem recorte"* — uma string vazia carregando a semântica mais
> perigosa que existe naquele método (ver tudo). Com um tipo, *"sem recorte"* deixa de ser um vazio e passa a
> ser um valor com nome — o mesmo movimento que o `EscopoDoPool` fez na F8, e pela mesma razão.

### 3.3 Onde a junção mora — aprofundado *(a pedido do analista, 2026-08-11)*

#### O problema, em uma frase

Depois do ADR-0023 D8, **a passagem guarda só ids**. Para mostrar um bilhete são necessários o nome da
empresa, o da embarcação, os portos de origem e destino e o nome dos clientes — e **nada disso está no
documento**. Em banco relacional isso seria um `JOIN`; **o Firestore não faz `JOIN`**, então alguém, em Kotlin,
tem de casar o bilhete com as entidades de referência. A pergunta é **quem**: a peça que traduz, ou quem a
chama.

#### Forma A — o mapper busca o que falta (é o que existe hoje)

```kotlin
// domain/mappers/PassagemDadosPassagemMapper.kt — código real, resumido
@Singleton
class PassagemDadosPassagemMapper @Inject constructor(
    private val empresaRepository: EmpresaRepository,        // ← a peça de tradução
    private val embarcacaoRepository: EmbarcacaoRepository,  //   depende da camada de dados
) : Mapper<Passagem, DadosPassagem> {
    override suspend fun map(entry: Passagem): DadosPassagem {
        val empresa = empresaRepository.obterPorId(entry.empresaId)      // busca aqui dentro
        val embarcacao = embarcacaoRepository.obterPorId(entry.embarcacaoId)
        …
    }
}
```

Três propriedades desta forma — todas verificáveis **no código que existe**, sem supor nada sobre telas
futuras:

1. **testar exige fake.** Para exercitar uma tradução — *"este bilhete vira esta linha de tela"* — é preciso
   montar dois repositórios falsos. O teste existe (`PassagemDadosPassagemMapperTest`, entre as 22 classes
   congeladas), e boa parte dele é andaime;
2. **contamina com `suspend`.** A interface `Mapper` é `suspend fun map`, então tudo que chama uma tradução
   passa a precisar de corrotina — inclusive código que só quer formatar;
3. **o número de buscas é invisível no ponto de chamada.** Quem lê `passagens.map { mapper.map(it) }` não vê
   que cada volta do laço faz duas buscas, porque a busca mora dentro da tradução. Isto é uma propriedade da
   **forma** — a chamada não revela o que ela custa —, e não uma medida de custo: quanto isso pesa depende de
   consumidores que ainda não têm domínio planejado, e dimensioná-los agora seria inventar requisito;
4. **inverte a direção da dependência.** O arquivo vive em `domain/mappers/` e importa
   `services.repository.cadastro.viagem.EmpresaRepository` — **o domínio importando a camada de dados**. É
   exatamente um dos itens que o estudo do domínio da plataforma já registrava como dívida.

#### Forma B — a função recebe pronto (é o que a F8 fez, e o que recomendo)

```kotlin
// A tradução não busca nada: recebe o que precisa e devolve o DTO.
fun paraLinhaDeConsulta(
    passagem: Passagem,
    embarcacoes: Map<String, Embarcacao>,   // já carregadas, indexadas por id
    clientes: Map<String, Cliente>,
): LinhaDeConsulta = …

// Quem chama carrega UMA vez, antes do laço:
val embarcacoes = embarcacaoRepository.obterTodos().associateBy { it.id }
val clientes = clienteRepository.obterPorIds(passagens.flatMap { it.clientes })
val linhas = passagens.map { paraLinhaDeConsulta(it, embarcacoes, clientes) }
```

É o estilo que a F8 usou em `disponiveisAPartirDe(agora, dias)`, `contarOcupacaoEmbarcacao(embarcacao, passagens)`
e `inicioDoPainel(…)` — e é por isso que aquela fase fechou com 516 testes verdes: **testa-se montando objetos
e chamando a função**, sem fake, sem corrotina, sem Hilt.

O que se ganha, ponto a ponto contra a lista acima:

1. **o teste vira aritmética**: entram objetos, sai objeto, compara-se;
2. **nada de `suspend`** — a tradução é síncrona porque traduzir não é buscar;
3. **o carregamento fica visível**: a função **não tem como** buscar, então quem carrega, carrega no claro. Não
   é que fique mais rápido — é que fica **legível**, e o que for lento passa a ser lento à vista;
4. **a dependência volta a apontar para dentro**: `domain/` deixa de importar `services/`.

Note que nenhum dos quatro é argumento de desempenho. Isso é deliberado: **desempenho aqui só se mede quando os
consumidores existirem** — e os que fariam a conta pesar (ocupação, balanço, análise) não têm domínio planejado.
Os quatro são argumentos de **forma**, e valem hoje.

#### O custo, que é real e tem nome

**Quem chama fica com mais trabalho.** As duas linhas de carregamento acima existiam antes escondidas no
mapper, e agora estão no ViewModel. Isso é bom (ficaram visíveis) e é ruim (o ViewModel engorda). A mitigação
que a casa já tem: **um coletor na camada de dados** — um método de caso de uso que carrega o pacote de
referências e devolve os DTOs prontos, mantendo a **tradução** pura por dentro. É o desenho do
`ContagemPassagensMapper`, que já é hoje uma classe com repositório envolvendo a função pura
`contarOcupacaoEmbarcacao`: o híbrido não é um meio-termo tímido, é a divisão certa — **coletar é da camada de
dados, traduzir é do domínio.**

#### Uma correção ao que eu afirmei no ADR-0024

Escrevi lá que *"as entidades de referência inteiras já vivem em `StateFlow`, então a junção é lookup em
memória, não leitura extra"*. Isso vale para **empresa, embarcação, localidade, porto, rota e viagem** —
coleções pequenas, que a sessão carrega inteiras.

**Não vale para os pools.** `clientes` e `veiculos` crescem sem limite, exatamente como as passagens, e pela
mesma razão **não podem** ser `observarTodos` (ADR-0024 D9). Logo a junção tem dois regimes, e é bom que isso
esteja escrito antes de alguém descobrir na tela:

| O que se junta | Como | Por quê |
|---|---|---|
| empresa, embarcação, rota, porto, localidade, viagem | **lookup em memória** sobre o `StateFlow` da sessão | coleção pequena, útil inteira |
| **cliente, veículo** | **leitura por ids**, em lote | pool que cresce sem limite; carregar inteiro é o erro que a Passagem já não comete |

A leitura em lote tem um limite de plataforma que vale registrar como mecanismo: o `whereIn` do Firestore aceita
**30 valores por consulta**, então `obterPorIds` particiona a lista em blocos de 30. **Não dimensiono o que isso
pesa em cada tela** — depende de consumidores que ainda não têm domínio planejado.

#### E o `Mapper<E, O>`

A interface (`util/Mapper.kt`, três linhas) **não serve** para a Forma B: ela é `suspend` e 1→1, e uma junção
pura tem **várias** entradas e nenhuma suspensão. Ela não precisa ser trocada por outra abstração — uma
**função de topo com nome bom** (`paraBilhete`, `paraLinhaDeConsulta`, `paraOcupacao`) é o que a F8 usou, e é
mais legível que uma interface genérica cujo único método se chama `map`.

### 3.4 Os DTOs, agora com tipo

O ADR-0024 D8 decidiu **tipo, não texto**. O que isso faz com o que existe:

`DadosPassagem` tem ~58 campos, quase todos `String` formatada — inclusive `idPassageiro1 = ""` e
`idVeiculo = ""`, campos que existem e são preenchidos com vazio. Ele serve **quatro** consumidores ao mesmo
tempo (bilhete, detalhes, consulta, impressão), e é por isso que tem 58 campos: é a união de quatro
necessidades.

Com DTO por caso de uso ele se divide em projeções pequenas, cada uma carregando tipo, e a formatação
(`formataParaMoedaBrasileira`, `extrairDocumentoFormatado`, `rotulo()`) sai do mapper e vai para a apresentação.

**Quantas projeções, e quais, não se decide aqui.** A versão anterior deste estudo listava quatro nomes —
incluindo uma de ocupação e uma de balanço — e isso era erro de método: **ocupação, balanço e análise não têm
domínio planejado**, e nomear a projeção de um consumidor é decidir de que campos ele precisa antes de saber o
que ele é. Cada projeção nasce **quando o seu consumidor for planejado**, que é a ordem que este próprio
trabalho seguiu (domínio → fronteira → camada). O que fica decidido é o **critério**: uma projeção por
consumidor, e cada campo existe porque a pergunta daquele consumidor o exige.

> **Decidido pelo analista (2026-08-11): o corte é por consumidor.**
>
> Vale dizer o que o corte por consumidor **rejeita**, porque é a alternativa que sempre reaparece: cortar por
> *entidade* (uma `PassagemDto` só, com tudo). É ela que produz o `DadosPassagem` de hoje — 58 campos que são a
> **união** de quatro necessidades, com o bilhete carregando campos de consulta e a consulta carregando dados
> de impressão. O sintoma clássico está no arquivo: `idPassageiro1 = ""` e `idVeiculo = ""`, campos que existem
> e são preenchidos com vazio porque *algum* consumidor talvez os queira.
>
> Com o corte por consumidor, **cada projeção responde a uma pergunta**: o bilhete pergunta *"o que se imprime"*,
> a consulta pergunta *"o que se lista"*, a ocupação *"quantos couberam"*, o balanço *"quanto entrou"*. Um campo
> só existe se a pergunta o exige — e o campo vazio de conveniência deixa de ter onde nascer. É o *passo 2* do
> ADR-0003 que o ADR-0019 formalizou, aplicado à entidade que mais o precisava.

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

> **Decidido pelo analista (2026-08-11): três desfechos, renomeados.** E a decisão está mais certa que a minha
> recomendação: o que o desfecho local mede **não** é "qual banco gravou" — é **o que o operador pode afirmar ao
> passageiro** antes de a rede confirmar. O cache do SDK dá essa garantia igual ao Room dava; o que muda é só
> o nome do lugar. Suprimir o estado apagaria a distinção que **mais** importa numa bilheteria de beira de rio:
> *aceito aqui* × *confirmado no servidor*.
>
> Fica: **`aplicadaLocalmente`** (o `set` entrou no cache e o bilhete já vale), **`sincronizou`** (ack do
> servidor) e **`pendenteDeSync`** (o servidor recusou ou está fora — degradado, não fatal), mais `falhou`.
> Os KDocs que dizem *"durável no Room"* passam a dizer *"aplicada no cache do SDK, e o SDK reconcilia"*.

## 4. O que morre na camada

| Peça | Por quê |
|---|---|
| `PassagemDao`, `ContadorDao`, `PassagemDigitalDao`, `RascunhoPassagemDao` | o Room sai (ADR-0017 F5) |
| `RascunhoPassagemStoreRoom` | trocado por impl DataStore — **a porta fica** |
| `PassagemDIgitalRepository` | o índice local do bilhete não tem substituto (ADR-0017 D5) |
| `PassagemDadosPassagemMapper` + `DadosPassagem` | divididos em projeções tipadas por consumidor, uma a cada consumidor planejado (§3.4) |
| `Mapper<E, O>` (para este uso) | junção pura tem várias entradas e não suspende (§3.3) |
| `getListaNome()` | substituído pela consulta recortada do pool `Cliente` |
| o listener do contador no `LoginViewModel` | o contador vira por ocorrência (§2.2) |
| `obterTodasPorData` devolvendo `Task<QuerySnapshot>` | a porta devolve domínio, não tipo do Firebase |

## 5. As perguntas, e o que o analista respondeu (2026-08-11)

| # | Pergunta | Resposta |
|---|---|---|
| 1 | consulta: métodos nomeados × **objeto de critério** × lambda com `Query` (§3.2) | **objeto de critério** |
| 2 | a junção vira função pura com as listas por parâmetro? (§3.3) | *"não entendi, aprofunde"* → o §3.3 foi reescrito com os dois estilos em código real. E o aprofundamento **corrigiu o ADR-0024**: *lookup em memória* vale para as referências, **não** para os pools `cliente`/`veiculo`, que exigem leitura por ids em lote |
| 3 | projeções tipadas no lugar do `DadosPassagem` (§3.4) | **corte por consumidor** — o que rejeita explicitamente o corte por entidade, que produziu os 58 campos. **Quais** projeções não se decide aqui: cada uma nasce com o seu consumidor planejado |
| 4 | telemetria com dois desfechos × três renomeados (§3.6) | **três renomeados** — e a decisão está mais certa que a minha recomendação: o desfecho local mede *o que o operador pode afirmar*, não qual banco gravou |
| 5 | a porta do §3.1 está completa? (§3.1) | **sim** |

Sobre a **5**, uma nota que a resposta deixa implícita e vale escrita: o cancelamento **não** ganha método
próprio porque ele é uma **transição** (`transicionar(id, CANCELADA)`), e *quem pode cancelar* não é assunto da
porta — é da política (`PermissoesUsuario`, ADR-0010), consultada **antes** da chamada, como em toda ação de
seção. Uma porta que perguntasse "posso?" teria duas fontes de autorização, e o ADR-0010 existe para haver uma.

### O que falta para virar ADR

Nada de estrutural. Os cinco pontos estão decididos, e o §3.3 fechou com uma correção de escopo que o ADR da
camada precisa carregar: **a junção tem dois regimes** (memória para referência, leitura em lote por ids para
os pools, com o `whereIn` particionando em blocos de 30).