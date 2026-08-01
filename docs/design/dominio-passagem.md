# Desenho de domínio — o agregado Passagem

**Status:** Rascunho — Claude rascunhou; **revisado em `2026-08-01`** contra o [ADR-0015](../adr/0015-rework-agente-equipe.md)
(implementado), o [ADR-0016](../adr/0016-dominio-da-plataforma.md) (domínio da plataforma) e o
[ADR-0017](../adr/0017-eixo-de-storage-firestore-only.md) (Firestore-only). A revisão traz a fotografia de
volta ao código **e** abre a direção do analista: **extrair os participantes do agregado como entidades
referenciadas — a primeira é o `Cliente`** (§11).

> Complementa o [estudo transversal de domínio](dominio-relacionamentos-e-camadas.md) e o
> [catálogo do domínio da plataforma](dominio-da-plataforma.md) (§3.11 resume o que aqui está por extenso).
> Decisões-fonte: [ADR-0003](../adr/0003-modelo-de-memoria-do-dado.md) (camadas do dado, hoje superado em
> parte pelo ADR-0017), [ADR-0008](../adr/0008-relacionamentos-por-identidade.md) (id vs. snapshot),
> [ADR-0010](../adr/0010-autorizacao-por-cargo.md)/[ADR-0011](../adr/0011-regras-firestore-por-cargo.md)
> (autorização), [ADR-0012](../adr/0012-ciclo-de-vida-passagem-e-embarque-qr.md) (ciclo de vida + QR),
> [ADR-0013](../adr/0013-tabela-de-tarifa-e-tipo-tarifario.md) (tarifa) e
> [ADR-0015](../adr/0015-rework-agente-equipe.md) (equipe, agência, cargo).

---

## 0. O que esta revisão corrige (o doc estava velho)

O texto anterior fotografava `2026-07`, logo após o ADR-0012. Desde então três ADRs passaram por cima dele:

| Afirmação antiga | Estado em `2026-08-01` |
|---|---|
| `model/passagem/Passagem.kt` | o pacote virou **`domain/`** — todos os caminhos mudaram |
| campo `agente` no bilhete | **morreu** (ADR-0015 P2.3): o emissor já é `funcionarioResponsavel` + `funcionarioId` |
| `agencia` como dado da venda | é **derivada**, não digitada: a agência do funcionário que emitiu, congelada na emissão |
| cargos `ADM, DIRETOR, COLABORADOR_MASTER, OPERADOR` | dois eixos: **papel** (`ADM`/`GESTOR`/`OPERADOR`) × **cargo** (`SUPERVISOR`/`AGENTE`) |
| "Room = cache espelho" nas camadas | o **ADR-0017** tira o Room do caminho; a F5 é justamente a Passagem |
| não citava tarifa | `tarifaBase` e `cilindrada` entraram com o **ADR-0013** (congelam a célula da tabela) |
| "Passagem é o único agregado, o resto é master data" | continua verdade **hoje**, mas o ADR-0016 multiplica o master data (Localidade, Rota, Ativo, Atuação) e §11 quer **quebrar essa exclusividade** |

O que **não** mudou e segue valendo: a FSM do status, o QR como ponteiro, a dualidade id × snapshot e a
leitura de que passageiro e veículo são **participantes de mesmo nível**.

## 1. O que a Passagem é no domínio

A **Passagem** é o bilhete emitido — o registro **transacional / de evento** do negócio. Hoje é o único
agregado do app: as demais entidades são *master data*; a Passagem é o fato que as consome e congela.

**Quem viaja são os participantes do agregado — e eles são de mesmo nível.** O **sujeito** da passagem é o
**titular**, e quem é o titular depende do **modo** (§11.3): nos três modos de pessoa é o **passageiro 1**
(obrigatório), com os passageiros 2 e 3 como acompanhantes opcionais (`temPassageiro2`/`temPassageiro3`); no
modo veículo é o **próprio veículo**. O **veículo é participante de mesmo nível do passageiro — não um value
object descritivo** (derivado `ehVeiculo`) precisamente porque pode ser sujeito do seu próprio bilhete.

O **responsável pela retirada** — cujos campos moram no estado do veículo (`FormVeiculoUiState`), não num
estado de passageiro — é **opcional por regra de negócio, não por descuido** (§11.3): ele nem sempre é
informado e costuma ser definido na hora, informalmente, entre despachante, transportadora e quem retira.
Bilhete de veículo **sem nenhuma pessoa nomeada é a forma normal do modo**, não uma inconsistência tolerada.

> Isto **revisa** o rótulo "value object" que o [estudo transversal](dominio-relacionamentos-e-camadas.md)
> deu a passageiros/veículo. E é exatamente a tensão que a §11 resolve: no plano do domínio eles são
> participantes; no plano da persistência ainda são campos achatados **sem identidade própria**.

`Passagem.kt` ainda acumula três papéis num só arquivo — `@Entity` do Room, modelo de domínio e fonte dos
snapshots. **O ADR-0017 tira o primeiro:** sem Room, a anotação sai e o agregado deixa de conhecer o banco
(hoje `temPassageiro2` — regra de negócio — carrega um `@Ignore`).

`app/src/main/java/dev/matheus/fluviapp/domain/passagem/Passagem.kt:9`

## 2. Anatomia da entidade

| Grupo | Campos | Natureza |
|---|---|---|
| Identidade | `id` (PK), `numero` | surrogate + número do bilhete (do `ContadorBilhete`) |
| **Ponteiros por id** (ADR-0008) | `viagemId`, `navioId`, `empresaId`, `funcionarioId` | link vivo p/ relacionar/agregar |
| **Snapshot por valor** | `codigoViagem`, `empresa`, `navio`, `origem`, `destino`, `dataViagem`, `horaViagem`, `agencia`, `funcionarioResponsavel` | histórico imutável do bilhete |
| Dinheiro (ADR-0013) | `valorPix/Dinheiro/Debito/Credito`, `tarifaBase` | `Double?` na fronteira; `tarifaBase` = célula congelada |
| Categoria tarifária | `tipoPassagem`, `gratuidade`, `acomodacao` | Strings de tipos fechados |
| Passageiros ×3 | `nome/documento/numeroDocumento/dataNascimento` (×3) | participantes; **passageiro 1 = titular** |
| Veículo (+ responsável) | `tipoVeiculo`, `modeloVeiculo`, `placaVeiculo`, `corVeiculo`, `cilindrada` + `*ResponsavelRetirada` (×3) | participante de **mesmo nível** |
| **Ciclo de vida** (ADR-0012) | `status`, `embarcadaPorId`, `embarcadaPor`, `embarcadaEm` | FSM + carimbo do check-in |
| Derivados `@Ignore` | `temPassageiro2`, `temPassageiro3`, `ehVeiculo` | calculados, não persistidos |

Todos os campos aditivos nasceram com default `""`/`null` — portfólio regenera via seed, não faz backfill.
As migrações Room que os acompanharam **deixam de existir como categoria** com o ADR-0017 (§9 do ADR-0015 já
colapsou as 19 migrações num DDL único).

`Passagem.kt:11-87` · `agencia` documentada em `:28-34` (por que o `agente` morreu).

## 3. Ciclo de vida — `StatusPassagem` como tipo + FSM

```
  criar                 emitir/imprimir            escanear QR no embarque
 ───────►  A_EMITIR ───────────────────► EMITIDA ────────────────────────► EMBARCADA
 (rascunho)            (QR passa a valer)          (check-in: valida e            (terminal —
                                                    consome o bilhete)             irreversível)
```

- `enum class StatusPassagem { A_EMITIR, EMITIDA, EMBARCADA }` — `proximos` define as arestas; `EMBARCADA`
  é terminal. `podeTransicionarPara(destino)`, `ehTerminal()`.
- **String só na fronteira**: `de(valor: String?)` converte na leitura (tolerante à grafia legada); grava-se
  o `.name`; `rotulo()` só para exibição. Convenção hoje uniforme no domínio (catálogo §4).
- **Cancelar não é estado** — segue *delete físico*. `CANCELADA`/`EXPIRADA` ficaram como futuro.

`domain/passagem/StatusPassagem.kt:13` · transição de aplicação em
`PassagemFirestoreRepository.transicionar(id, novo)` — hoje espelha Room + Firestore; **com o ADR-0017 F5
sobra só o documento**, e o "espelho best-effort" some junto com a chance de os dois discordarem.

## 4. Confirmação de embarque por QR

O QR do bilhete é **ponteiro, não credencial**: o payload é sempre `Passagem.id`, idêntico no bilhete físico
e no digital. O scanner (CameraX + ML Kit, offline) lê o id e **a validação acontece contra o servidor** —
o bilhete pode ter sido emitido em outro device.

`EmbarqueViewModel` → `PassagemFirestoreRepository.confirmarEmbarque`: lê ao vivo, avalia pela FSM, e se
legal carimba `status=EMBARCADA` + autoria + timestamp. O resultado é um `sealed interface ResultadoEmbarque`
(`Confirmada` / `JaEmbarcada` / `NaoEmitida` / `NaoEncontrada`) que a UI consome caso a caso.

A leitura ao vivo é a razão de a Passagem **nunca ter sido espelhada por listener** — e por isso o ADR-0017
a trata como a fase mais barata: as consultas pesadas já vão direto ao Firestore.

## 5. Relações por identidade e snapshots (ADR-0008)

A Passagem é o caso onde **id-para-relacionar e valor-para-lembrar coexistem legitimamente**:

- **Ponteiros por id, congelados na emissão** (`viagemId`, `navioId`, `empresaId`, `funcionarioId`):
  sobrevivem a rename do master data. O balanço agrega por `navioId` frozen.
- **Snapshot por valor** (`empresa`, `navio`, `origem`, `destino`, `codigoViagem`, `agencia`,
  `funcionarioResponsavel`): registro imutável do que o bilhete dizia. Bilhete impresso não muda depois.

Este é o padrão que a §11 estende aos **participantes** — hoje eles são o único bloco do agregado que tem
valor sem chave.

## 6. Formas do dado e mappers

```
PassagemDocumento  ──toPassagem(id)──►  Passagem  ──map──►  DadosPassagem
  (Firestore,          ◄──toPassagemDocumento──   (domínio,       (tela: detalhes/
   aninhado/DTO)                                   plano)          impressão)
```

| Forma | Arquivo | Papel |
|---|---|---|
| `PassagemDocumento` | `services/repository/firebase/documents/PassagemDocumento.kt:5` | DTO Firestore **aninhado** (`viagem`, `passageiro1..3`, `veiculo`) |
| `Passagem` | `domain/passagem/Passagem.kt:9` | domínio (+ `@Entity` até o ADR-0017 F5) **plano** |
| `DadosPassagem` | `domain/screendata/DadosPassagem.kt` | projeção de tela (`situacao` já rotulada) |
| `RascunhoPassagem` | `domain/rascunho/RascunhoPassagemMapper.kt` | rascunho da emissão (ADR-0004) — vai para o DataStore (ADR-0017 D4) |

**O achatamento é herança do Room, não do domínio.** O documento **já nasce aninhado**
(`PassageiroDocumento`, `VeiculoDocumento`); quem exige as ~28 colunas planas `nomePassageiro1`,
`documentoPassageiro2`, … é a tabela. Removido o Room, o mapper `toPassagem` deixa de ter motivo para
desmontar o que o documento entrega montado — e a §11 fica barata **por causa disso**.

## 7. Autorização — dois eixos, e o segundo cresceu (ADR-0010/0011/0012/0015)

A política única (`domain/operacoes/PermissoesUsuario.kt`) é a fonte de verdade Kotlin, **espelhada** nas
regras Firestore. Hoje ela decide por **par `(papel, cargo)`**:

- **Papel** (sistema): `ADM` administra a plataforma, `GESTOR` opera, `OPERADOR` é o coringa que corresponde
  a um funcionário. `ADM`/`GESTOR` **não emitem passagem** (ADR-0016).
- **Cargo** (negócio, no `Funcionario`): `SUPERVISOR` edita qualquer passagem da sua agência; `AGENTE` emite
  e edita as próprias. `Funcionario.kt:44`.
- **Posse**: `ehDono` = `funcionarioId == uid`.
- **Confirmar embarque** é eixo próprio (ADR-0012): **qualquer cargo conhecido** — embarque é ação de doca.
- **Recorte por agência** (ADR-0015 P2.6): a consulta de passagens filtra pela agência do logado; agência em
  branco = papel de plataforma, sem recorte. Continua isolamento **por cliente** — as regras do servidor não
  recortam por agência (débito registrado).

## 8. A FSM em três lugares — dever de paridade

| Camada | Onde | O que impõe |
|---|---|---|
| **Domínio** | `StatusPassagem.kt` (`proximos`) | arestas legais, terminal, fail-closed |
| **Aplicação** | `PassagemFirestoreRepository.transicionar` / `confirmarEmbarque` | idempotência, carimbo |
| **Servidor** | `firestore.rules` → `transicaoStatusLegal()` / `ehConfirmacaoEmbarque()` | fronteira real |

No servidor, `ehConfirmacaoEmbarque()` exige `EMITIDA→EMBARCADA` **tocando só os 4 campos** do embarque e
carimbando o **próprio uid**; `funcionarioId` é imutável. `firestore.rules:42-75, 114-148`; suíte de
emulador em `firestore-tests/rules.test.js`.

> Mudou uma aresta? muda nos três lugares. A suíte de emulador é a rede que trava a divergência.

## 9. Dívidas e pontos abertos (atualizados)

- **Navio ainda por snapshot na leitura** — `PassagemDadosPassagemMapper` resolve empresa por id, mas lê o
  navio do snapshot; `navioId` está congelado, só falta consumi-lo.
- **Enum legado remanescente** — `Constante.Descricao.{A_EMITIR, EMITIDA}` sobrevive como opção de dropdown
  de filtro. Some com a F1 do ADR-0016 (`Constante` → `Catalogo`).
- **`N+1` + `runBlocking`** — `BalancoPassagensMapper`, `PassagemDigitalHelper` e `adicionarContador`.
  Parte morre no ADR-0017 (o `salvarTodas` que ninguém lê), parte não.
- **Validação offline do embarque** — exige rede. Token assinado + sync posterior segue como futuro.
- **`getListaNome()` devolve `emptyList()`** (`PassagemFirestoreRepository.kt:168`) — e três campos de nome
  do form (passageiros 1/2/3) mais o do responsável pela retirada consomem essa lista como **sugestão de
  autocompletar**. **A tela promete reaproveitar pessoa e o domínio não tem o que responder.** É a cova que
  o `Cliente` da §11 preenche — e a melhor prova de que a apresentação já pediu o que falta embaixo.
- **`androidx.room.Ignore` dentro de `ui/states/passagem/FormPassageiroUiState.kt:3`** — anotação de
  persistência num UiState. Não faz nada; some com o ADR-0017, mas revela o vazamento de camada.
- **O form de emissão entrou no molde** (o [estudo do form](form-passagem-validacao-exibicao.md) descreve o
  estado **anterior**): a validação já é **pura** (`ValidacaoPassageiro`/`ValidacaoVeiculo`/
  `ValidacaoDadosPassagem` devolvem `Erros*`, sem mutar estado), os três UiStates já são **puros** (sem
  lambdas) e os eventos vão threadados por parâmetro no `FormPassagemScreen`. O que sobra de estrutural é o
  **eixo implícito** que a §11.3 fecha.
- **Assento numerado** — só `acomodacao` (classe), sem poltrona.

## 10. O que o ADR-0017 muda aqui (F5)

A Passagem é a **fase 5** do plano Firestore-only, e é a mais barata: as duas consultas pesadas (contagem e
pesquisa) já vão direto ao Firestore, e o `dao.salvarTodas(passagens)` que vem depois **ninguém lê**. Sobra
trocar `obterPorId` por `document(id).get()` e apagar o espelho.

Três consequências de domínio, não de infra:

1. **O agregado para de conhecer o banco** — sem `@Entity`/`@Ignore`, `temPassageiro2` volta a ser só regra.
2. **A forma plana perde a razão de ser** (§6) — o achatamento existia para caber em colunas.
3. **A emissão passa a ter um registro só** — hoje grava Room, segue a UI e dispara `set(...)`
   fire-and-forget; uma recusa do servidor deixa o bilhete **só naquele aparelho, para sempre**.

O ADR-0017 é explícito em **não** decidir a forma do dado ("aqui o modelo Kotlin continua idêntico — só
perde as anotações"). A §11 é justamente a decisão de forma que ele deixou em aberto, e **por isso ela vem
depois, não junto**.

## 11. Direção do analista — participantes como entidades referenciadas; a primeira é o `Cliente`

**Decisão de 2026-08-01.** Com o Room saindo, a Passagem deixa de ser uma linha de tabela e volta a ser um
**agregado de entidades**. Cada participante passa a ser gravado no documento como **chave para referência +
valores para leitura do bilhete gravado**. A primeira entidade a nascer é o **`Cliente`**, com os campos que
já existem hoje na passagem — e ele serve **tanto ao passageiro quanto ao responsável pela retirada**, que
são a mesma coisa no mundo: uma pessoa.

### 11.1 A forma: chave + valores, no mesmo lugar

É o **ADR-0008 aplicado ao participante**. Nada de resolver a pessoa por junção na leitura — o Firestore não
faz junção, e o bilhete não pode mudar quando o cadastro for corrigido:

```
passagens/{id}
  passageiro1: { clienteId: "abc123",   ← chave: relaciona, agrega, reaproveita
                 nome, documento, numeroDocumento, dataNascimento }   ← valores: o bilhete
  veiculo:     { …, responsavel: { clienteId: "…", nome, documento, numeroDocumento } }

clientes/{id}
  nome, documento, numeroDocumento, dataNascimento  ← o pool (§11.8): acumulativo, não canônico
```

O bilhete lê **sempre pelos valores** — `DadosPassagem`, impressão e bilhete digital não passam a depender de
uma segunda leitura. O `clienteId` serve para o que valor não serve: reaproveitar no próximo form, contar
quantas vezes a pessoa viajou, corrigir o cadastro sem reescrever histórico.

**`clienteId` vazio é estado válido.** Igual ao responsável pela retirada, que já é opcional: a passagem
emitida sem cliente salvo é um bilhete completo com um participante anônimo. Fail-open aqui, porque o oposto
— barrar a emissão porque a gravação do cliente falhou — troca uma inconsistência tolerável por uma venda
perdida na fila do porto.

### 11.2 Salvar o cliente com um botão, no meio do preenchimento

A gravação do cliente **sai de dentro da emissão**: um botão salva a pessoa enquanto o operador preenche
outro participante ou avança de etapa. Consequências assumidas:

- **Cliente órfão é aceitável** — passagem abandonada deixa uma pessoa no pool, não lixo: o próximo bilhete
  dela já sai preenchido, e **o pool é acumulativo por natureza** (§11.8).
- **Escrita fora da emissão precisa de regra no servidor no mesmo incremento** (ADR-0011): `clientes` é a
  primeira coleção de **dado pessoal de terceiro** fora do bilhete. Regra escrita depois é regra que passou
  um tempo aberta.
- **É a coleção que mais pede o back-end** que o ADR-0017 já previu para "informação mais sensível". Não
  bloqueia o mobile-first de agora, mas deve nascer sabendo disso (§11.7).
- **Salvar também assina** — o botão grava a pessoa **e** carimba a agência do logado no documento (§11.7),
  que é o que torna a pessoa reencontrável no próximo bilhete daquela agência.

### 11.3 O modo da passagem — quatro valores exclusivos

**Decisão de 2026-08-01.** **REDE, SUÍTE, CAMAROTE e VEÍCULO são quatro modos exclusivos de um eixo único.**
Veículo **não tem acomodação**; e escolher acomodação **já diz** que o bilhete é de passageiro. Some o par
`acomodacao: String` + `isVeiculoChecked: Boolean` — o que existe é **um** tipo fechado de quatro valores.

**O código já se comportava assim; faltava o tipo.** A exclusividade não é novidade de UI — é invariante que
hoje se sustenta por limpeza reativa e `if`:

- `FormPassagemHelper.checkVeiculo()` chama `limparCamposPassageiroOuVeiculo()`, que **zera acomodação,
  tipo de passagem, gratuidade e os três passageiros** ao marcar veículo (`FormPassagemHelper.kt:53-79`).
- `FormPassagemScreen.kt:202` **troca a área inteira** de passageiro pela de veículo.
- `ContentPagamentoAreaForm.kt:102` força `TipoPassagem.INTEIRA` no modo veículo — veículo não tem meia nem
  gratuidade, porque essas categorias pertencem ao modo **rede**, o único em que a unidade vendida é uma
  pessoa (§11.9c).
- **A tabela de tarifa já é de uma dimensão só**: `TarifaViagem.chave` é canônica ∈ {`REDE`, `SUITE`,
  `CAMAROTE`, `CARRO`, `CARRETA`, `CAMINHAO`} — **nunca existiu célula `SUITE × CARRO`**.

O último item **corrige a descrição do ADR-0013** ("dois eixos: acomodação × classe de veículo"): a
implementação sempre foi de um eixo; o que estava implícito era o modo. Precisão a manter: o modo **não é**
a chave tarifária, é **quem decide de onde ela sai** — nos três modos de passageiro a chave é o próprio
modo; no modo veículo, é a **classe** (`tipoVeiculo`), com moto por regra de cilindrada (ADR-0013).

Consequências que caem direto desta decisão:

- **O titular do bilhete de veículo é o veículo** *(decisão de 2026-08-01)*. O responsável pela retirada
  **não pode ser obrigatório** — na operação real ele "nem sempre é informado, e é definido na hora e
  informalmente pelos interessados: despachante, transportadora, retirador". Exigi-lo **travaria o processo
  como ele é**. A recomendação de promovê-lo a titular obrigatório foi **rejeitada, e por motivo de
  negócio**: quem retira não é quem o bilhete identifica — é uma anotação **logística e tardia** sobre a
  entrega, não a identidade da venda. **A passagem de veículo só precisa existir com o seu tipo e o seu
  valor.**
  Isto fecha o §1 em vez de contradizê-lo: **todo bilhete tem titular, e o titular é quem dá nome ao modo** —
  o passageiro 1 nos três modos de pessoa, o **veículo** no modo veículo. Passageiro e veículo continuam
  participantes de mesmo nível justamente porque cada um pode ser sujeito do seu bilhete. O bilhete de
  veículo pode, portanto, não ter **nenhuma pessoa** — e isso deixa de ser inconsistência tolerada (§1) para
  ser **a forma normal do modo**.
- **Capacidade é propriedade do modo.** REDE já esconde o checkbox do passageiro 2 (`ehAcomodacaoRede`, um
  derivado de UiState): rede é individual; suíte e camarote aceitam até 3. Tipado o eixo, isso vira
  `maxParticipantes` do modo — 1 / 3 / 3 / 1 — e sai da UI.
- **`ehVeiculo` deixa de ser inferência.** Hoje é `!placaVeiculo.isNullOrEmpty()` — o modo deduzido de um
  campo de dado. Vira `modo == VEICULO`.
- **O que se grava.** `acomodacao` com quatro valores seria justamente a mentira que a decisão mata: nasce
  `modo` no documento. Sem produção, é **regenerar**, não migrar (a pesquisa, que hoje tem dois checkboxes
  independentes — `PesquisarPassagemUiState.filtrarTodos/filtrarVeiculos/filtrarPassageiros` — vira filtro
  por conjunto de modos).

**E é isto que as abas refletem:** quatro abas = os quatro modos, cada uma sabendo qual state/helper entra;
data, hora e pagamento são as etapas comuns. A tela ganha a forma do eixo **porque o eixo existe** — não o
contrário.

### 11.4 Pontos que faltam decidir (semente do ADR)

1. ~~**Identidade do cliente.**~~ **Decidido em 2026-08-01** (§11.8): **chave natural** (documento
   apresentado), com a duplicidade da mesma pessoa por credenciais diferentes **aceita**.
2. ~~**De quem é o cliente.**~~ **Decidido em 2026-08-01** (§11.7): **da plataforma, assinado pela
   agência** — pessoa é uma só; o recorte é de alcance, não de existência.
3. ~~**Divergência valor × cadastro.**~~ **Decidido em 2026-08-01** (§11.7): **só a plataforma
   sobrescreve.** A agência cria e assina; corrigir o que já está no pool é ato de curadoria, do painel.
4. ~~**Ordem contra o ADR-0017.**~~ **Decidido em 2026-08-01:** nenhuma das duas — **fecha-se o domínio
   inteiro do agregado primeiro** (§11.9), e só então se discute por onde a implementação entra.
5. **Quem é o segundo.** O veículo é a candidata natural a virar a segunda entidade extraída (placa é chave
   natural de verdade). Fica registrado, não decidido.
6. ~~**Titular do modo veículo.**~~ **Decidido em 2026-08-01** (§11.3): o titular é o veículo; o responsável
   pela retirada segue **opcional para sempre**, por regra de negócio.
7. ~~**O núcleo obrigatório do modo veículo.**~~ **Decidido em 2026-08-01** (§11.6): placa e modelo
   **continuam obrigatórios** — são o que identifica o veículo, e entram no "tipo"; mas **modelo é exigido
   por classe**: carro e moto sim, caminhão e carreta não.
8. **A classe do veículo vira tipo fechado do domínio** — aceito em direção (§11.5/§11.6); falta o recorte
   de implementação (quem some do catálogo, o que continua livre).

## 11.5 "Quantos, qual veículo e qual preço" — o dado que é a alma da plataforma

**Direção do analista, 2026-08-01.** O produto do modo veículo não é o cadastro da pessoa: é a série
**contagem × classe × preço**. É o que a plataforma vende como informação — e isso muda o que precisa ser
rigoroso no bilhete.

Se a agregação por classe é o ativo, então **a classe do veículo não pode ser texto de catálogo editável** —
e o §11.6 acrescenta a segunda razão: é a classe que decide **o que o veículo precisa informar**.
Hoje `tipoVeiculo` é uma `String` escolhida do catálogo (`obterTodosPorCategoria(VEICULO)`,
`FormVeiculoHelper.kt:40`), e a gravação é o **nome** — não um id. Duas forças batem nisso:

- o **ADR-0016 F1** dissolve `Constante.Descricao` e torna o catálogo **dinâmico e multi-segmento**: quem
  administra a plataforma passa a criar entradas. Uma entrada nova ("CARRO PEQUENO") ou um rename e a série
  histórica **se parte em duas** sem ninguém perceber;
- o **ADR-0008** já decidiu que relação viva se faz por identidade, não por nome — e agregação é relação.

Portanto: **classe de veículo é tipo fechado do domínio** (como `StatusPassagem` e `TipoPassagem` já são),
com o catálogo servindo, no máximo, para **rotular** e para o que é livre de fato (modelo, cor). A tabela de
tarifa já pede isso: `TarifaViagem.chave` é **canônica** — `CARRO`/`CARRETA`/`CAMINHAO` — e um valor fora
dessa lista simplesmente não encontra célula (fail-closed do ADR-0013, que hoje aparece ao operador como
"sem tarifa cadastrada").

Curiosidade que confirma o eixo: `Constante.Descricao` já traz `VEICULO` e `PASSAGEIRO` sob o comentário
`//Categoria Passagem` (`Constante.kt:48-50`) — o modo da §11.3 **já existia embrionário no catálogo**, com
dois valores em vez de quatro, e nunca foi usado como tipo.

Três consequências para registrar, sem decidir agora:

1. **A moto agrega por faixa, não por célula.** `tarifaMotoBase = floor(cc/100)*100` (ADR-0013) faz da moto
   uma classe **derivada da cilindrada**. Para a série "quantos e qual preço", moto é uma família de faixas,
   e a `cilindrada` gravada no bilhete é o que permite reconstruí-las.
2. **A contagem por classe é leitura, e leitura agora tem preço.** O ADR-0017 avisa: sem SQL, `range`/
   `orderBy` exigem índice composto — e `firestore.indexes.json` **não existe** no `firebase.json`. O
   relatório de veículos é exatamente o gatilho que ele previu para pagar essa dívida.
3. **O balanço já é o lugar.** O [estudo do balanço financeiro](balanco-financeiro.md) mede hoje só
   ocupação; "quantos × classe × preço" é a face de veículo do mesmo relatório — reusa `tarifaBase`
   congelada e `navioId`/`viagemId`, sem persistência nova.

## 11.6 A classe governa o veículo — o que cada uma exige

**Decisão de 2026-08-01.** **Placa e modelo continuam obrigatórios** — são as informações que **identificam
o veículo**, e é isso que "existir com o seu tipo" quer dizer. Mas a exigência **não é uniforme**: ela é
propriedade da **classe**.

- **`modelo` é a linha** — HB20, Strada, Biz. É o vocabulário do negócio, e por isso a palavra fica: não é
  "linha" nem "nome" no bilhete, é **modelo**, que é como se fala na doca.
- **Caminhão e carreta não levam modelo**: na semântica do negócio **a própria classe já é o modelo**. Pedir
  linha para uma carreta é pedir informação que ninguém dá.

Isto dá à classe a mesma natureza que o modo tem no §11.3 — um tipo fechado com **regras próprias**, em vez
de um rótulo de catálogo:

| Classe | Exige modelo | Exige cilindrada | De onde sai a tarifa |
|---|---|---|---|
| `CARRO` | sim | não | célula `CARRO` da tabela da viagem |
| `MOTO` | sim | **sim** (ADR-0013) | **regra** `floor(cc/100)*100` |
| `CAMINHAO` | **não** | não | célula `CAMINHAO` |
| `CARRETA` | **não** | não | célula `CARRETA` |

Placa é exigida pelas quatro — é o que identifica o bem individual na doca e no embarque.

**Divergência a corrigir:** hoje `validarVeiculo` exige `modeloVeiculo` **sempre**
(`ValidacaoVeiculo.kt:29`), sem olhar a classe — logo **carreta e caminhão não passam sem modelo**, contra a
regra de negócio. A validação já é pura e JVM-testável, então isso é uma fatia pequena com teste próprio: a
mesma forma que a cilindrada da moto já tem (`:33`) aplicada ao modelo, invertida.

> Nota de vocabulário para o ADR: `tipoVeiculo` no código é o que aqui se chama **classe** (o eixo fechado
> `CARRO`/`MOTO`/`CAMINHAO`/`CARRETA`), enquanto `modelo` é a linha comercial. Vale fixar os dois nomes antes
> de tipar, senão "tipo" cobre as duas coisas.

## 11.7 De quem é o cliente — da plataforma, assinado pela agência

**Decisão de 2026-08-01.** O `Cliente` é **da plataforma**: uma pessoa é um documento, não um por tenant.
Mas cada documento carrega a **assinatura das agências** que já o atenderam (metadado), e **toda leitura é
recortada por ela** — "assim, a agência não pega o `listaNome` de todo mundo, **pode onerar**".

**Identidade única, alcance recortado.** É o que desempata o trade-off que estava aberto: cliente por
empresa duplicaria a mesma pessoa em cada tenant; cliente da plataforma sem recorte devolveria a base
inteira no autocomplete. A assinatura separa as duas coisas — **existência é global, visibilidade é local**.

A forma segue o padrão que o [catálogo do domínio](dominio-da-plataforma.md) já registra para os três
muitos-para-muitos da plataforma (`Funcionario ↔ Empresa` via `empresaIds[]`): um array pequeno por
natureza, consultado com `array-contains`.

```
clientes/{id}
  nome, documento, numeroDocumento, dataNascimento
  agenciaIds: ["ag-santana", "ag-belem"]   ← assinatura: quem já atendeu esta pessoa
```

- **A lista de nomes vira consulta recortada** — `whereArrayContains("agenciaIds", agenciaDoLogado)`. É o
  que enche a cova do `getListaNome()` (§9) **sem** varrer a base: o motivo declarado é **custo de leitura**
  (o ADR-0017 assume "leitura tem preço" e não tem SQL de escape), e o efeito colateral é PII não circular
  entre agências.
- **A regra do servidor recorta pela mesma cláusula** (ADR-0011). Atenção ao mecanismo, que o ADR-0016 já
  registra para o navio: **a regra não filtra a query — ela nega a query inteira** se o cliente não filtrar.
  Logo o filtro por agência é obrigatório no app **e** exigido no servidor, e é isso que o torna real.
- **Assinar é escrita num documento de outro tenant.** Reusar uma pessoa cadastrada por outra agência
  acrescenta o próprio `agenciaId`. A regra permite **só o append do próprio id** — mesmo endurecimento que
  `ehConfirmacaoEmbarque()` faz com os quatro campos do embarque (`hasOnly` + o id ser o do requisitante).
- **Só a plataforma sobrescreve** *(decisão de 2026-08-01)*. A escrita da agência tem exatamente dois
  direitos: **criar** a entrada que não existe e **assinar** a que existe. **Corrigir conteúdo** — nome,
  nascimento — é ato de **curadoria da plataforma**, do painel administrativo (ADR-0016), no mesmo eixo em
  que o ADR-0017 §7.1 começou a separar `ADM` de `GESTOR`. Isso resolve o `set(merge)` que sobrescreveria o
  pool a cada bilhete: ele deixa de ser permitido para quem emite.
  Na regra, os dois direitos são estados distintos: **create** (`resource == null`) com o conteúdo, e
  **update** restrito ao `arrayUnion` da assinatura. Como o agente **não pode ler** o que ainda não assinou,
  ele não sabe qual dos dois é o caso — a mecânica é tentar um e cair no outro (duas escritas baratas no pior
  caso). Consequência assumida: **entrada criada com nome errado só a plataforma conserta**; o operador segue
  emitindo, porque o bilhete carrega os valores dele (§11.1) e não depende do pool.

Duas consequências que vêm junto, e valem estar escritas:

1. **Descoberta cruzada não existe** — uma pessoa cadastrada por outra agência **não aparece** para quem não
   a assinou. Com a chave natural do §11.8 isso **não vira duplicata**: gravar é `set(merge)` no id
   derivado do documento e a assinatura é `arrayUnion`, então duas agências que atendem o mesmo CPF
   **convergem no mesmo doc sem nunca se enxergarem**. Sobra só a duplicata por **credencial diferente**
   (CPF numa agência, RG noutra) — que é justamente a aceita em §11.8.
2. **A assinatura é recorte de alcance, não segredo.** Quem tem o documento da pessoa na mão (o operador
   sempre tem) assina e passa a ver. Isso é aceitável para o regime mobile-first de hoje e é exatamente o
   caso que o ADR-0017 aponta para o **back-end centralizador**: "informação mais sensível e mais precisa vai
   exigir um sistema centralizador próprio". `clientes` é a primeira coleção que pede esse degrau.

## 11.8 O `Cliente` é um pool, não um master data — chave natural e duplicidade aceita

**Decisão de 2026-08-01.** A identidade do cliente é a **chave natural**: o **documento apresentado** (tipo +
número). E a consequência que isso traz — a mesma pessoa cadastrada com CPF numa agência e RG noutra virar
**dois clientes** — é **aceita como inevitável**, não combatida.

O reenquadramento é o que importa, e ele muda o estatuto da entidade: **"não é redundância, é questão de
análise de dados. O importante é ter os dados; pode ser até normalizado depois. Não é um master data em
si."** O `Cliente` não entra no domínio no mesmo nível de Empresa, Navio ou Viagem — aqueles são referência
governada, com dever de unicidade e de correção. O `Cliente` é um **pool acumulativo**: o que ele deve
garantir é que a informação **exista** e seja atribuível, não que seja canônica.

O que cai fora do caminho crítico por causa disso:

- **Deduplicar é etapa analítica posterior**, não invariante de escrita. Casar duas entradas da mesma pessoa
  (nome + nascimento, por exemplo) é trabalho de análise, e acontece **fora da emissão** — nada no fluxo do
  operador espera por isso. Emitir passagem nunca deve custar uma varredura de pessoas parecidas.
- **Um "cliente" identifica a credencial, não a pessoa.** `clientes/{CPF:…}` é *a pessoa como se apresentou
  com aquele documento*. Duas entradas da mesma pessoa não são erro de modelo: são dois fatos de
  atendimento. O `clienteId` do bilhete (§11.1) aponta para a entrada usada **naquela venda** — o que é
  exatamente o que um bilhete deve lembrar.
- **Todo passageiro tem documento** *(decisão de 2026-08-01)*: **"não existe criança sem documento nesse
  negócio"** — a certidão/RG é apresentada como qualquer outra credencial. Some a objeção que eu tinha
  levantado contra a chave natural: ela cobre **100% dos passageiros**, não uma fatia. `clienteId` vazio
  (§11.1) deixa de ser o caso da criança e passa a cobrir só duas situações: o **responsável pela retirada**
  não informado (§11.3) e o **fail-open** de uma gravação que não completou.
  **Divergência a corrigir:** hoje o documento é exigido **condicionalmente** — `validarPassageiro` só cobra
  o número **se um tipo de documento foi escolhido** (`ValidacaoPassageiro.kt:54, 61-62, 65-66`), de modo que
  deixar o tipo em branco passa e emite bilhete sem credencial nenhuma. Com esta decisão, tipo **e** número
  passam a ser obrigatórios para o titular e para os acompanhantes marcados — e é isso que garante que todo
  passageiro tenha entrada no pool.

**O limite a manter escrito**, porque a §11.5 diz que a informação é a alma da plataforma: **"clientes
únicos" é uma métrica aproximada por construção**. Contagem de pessoas distintas no pool é estimativa até
que a normalização posterior rode; contagem de **atendimentos** (que é o que a passagem registra) continua
exata. Confundir as duas seria vender precisão que o modelo não promete — e o modelo escolheu, de propósito,
ter o dado em vez de ter a unicidade.

## 11.9 Fechar o domínio antes de implementar — as partes que faltam

**Decisão de 2026-08-01.** Primeiro **fecha-se o domínio do agregado inteiro**; só depois se decide por onde
a implementação entra. O `Cliente` foi a primeira parte a ser resolvida (§11.1–11.8), não a única — enquanto
as outras estiverem indefinidas, qualquer código escrito escolhe por elas.

**Estado em 2026-08-01: o domínio do agregado está fechado.** Todas as decisões de negócio foram tomadas;
o que resta em "o que falta" é **forma**, a resolver no ADR. Três partes já estavam no padrão chave +
valores:

| Parte | Estado | O que falta |
|---|---|---|
| Participante pessoa | **fechada** (§11.1–11.8) | — |
| Modo / acomodação | **fechada** (§11.3) | — |
| Viagem | **fechada** (§11.10a) — a passagem aponta para a **ocorrência** (o dia exato) | forma da ocorrência (§11.10a) |
| Emissor | **fechada** (§11.10b) — agência **por id** + nome snapshot | — |
| Embarque | **fechada** (§11.10c) — o estado é o status; o carimbo vira sub-objeto | ajustar a regra do servidor junto |
| **Veículo** | **fechada** (§11.9a/§11.10d) — pool, chave = placa, máscara na entrada | — |
| **Pagamento** | **fechada** (§11.9b/b′) — lançamentos `{id, forma, valor}` embutidos; emissão pós-pagamento | — (faturamento é módulo à parte) |
| Carimbo de tempo | **fechada** (§11.9b) — `criadoEm` / `alteradoEm` | campos novos: hoje a passagem não registra quando foi emitida |
| **Tipo tarifário** | **fechada** (§11.9c) — é da acomodação | — |
| Capacidade / estoque | **fechada** (§11.9e) — é do navio, no cadastro | forma: onde a capacidade mora no `Navio` |
| **Numeração** | **fechada** (§11.9d) — por viagem | forma: contador atômico por viagem |

### 11.9a O veículo é entidade de pool — a placa é a chave natural

**Decisão de 2026-08-01.** O veículo vira a **segunda entidade extraída**, no mesmo regime do `Cliente`:
pool acumulativo (não master data), **chave natural = a placa**, assinatura por agência (§11.7) e
sobrescrita só pela plataforma. No bilhete, o mesmo par do §11.1: **`veiculoId` (a placa) + os valores
congelados**.

**A placa é uma chave melhor do que o documento de pessoa** — é única por construção e não tem o problema
CPF × RG do §11.8. O pool de veículos, portanto, **não "polui"**: duplicata só nasce de erro de digitação.
E isso desloca o peso da curadoria — como só a plataforma corrige (§11.7), placa digitada errada é entrada
errada até alguém do painel arrumar.

Daí sai uma exigência que o `Cliente` não tinha: **a placa precisa de forma canônica**. Sem normalizar
caixa, hífen e espaço, `ABC-1234` e `abc1234` viram dois veículos — e o formato Mercosul (`ABC1D23`) convive
com o antigo. É o mesmo princípio de "String só na fronteira" que os enums do domínio já seguem: normaliza
na entrada, grava o canônico.

**O que o pool guarda é o que identifica o veículo** — classe, modelo, cor e, na moto, a cilindrada. Note
que a cilindrada **é do veículo**, não da venda: ela está hoje no bilhete porque justifica a tarifa
(ADR-0013), e continua lá como valor congelado, mas o lugar natural do atributo é o veículo. O
**responsável pela retirada não entra no pool** — é pessoa (vai para `clientes`, quando informado) e o
vínculo é da **passagem**, não do veículo: quem retira muda a cada travessia (§11.3).

Dois ganhos, e o primeiro eu tinha subestimado:

- **Serve ao operador, não só à análise.** Digitar a placa preenche classe, modelo e cor — o análogo do
  autocomplete de nome (§9), e mais direto: aqui a busca é **get por id**, porque o operador digita a chave
  inteira, sem precisar de lista. A mecânica do §11.7 (tentar criar, cair no `arrayUnion`) resolve inclusive
  o caso do veículo já cadastrado por outra agência: não se enxerga o documento, tenta-se criar, o create
  falha, assina-se — e a partir dali ele aparece.
- **Torna a série por classe confiável** (§11.5). A classe passa a ser atributo **do veículo**, estável entre
  travessias, em vez de digitação repetida a cada venda; e uma placa cadastrada como `CARRO` numa venda e
  `CAMINHAO` noutra deixa de ser invisível — vira divergência que o pool expõe.

**Limite herdado:** placa é dado pessoal indireto (rastreia deslocamento de alguém). Vale a mesma
consideração do §11.7 — recorte por assinatura agora, back-end centralizador quando o dado exigir mais.

### 11.9b Pagamento — a análise (a pedido do analista, 2026-08-01)

Hoje são quatro colunas nomeadas — `valorPix`/`valorDinheiro`/`valorDebito`/`valorCredito` — e **tudo o que
o sistema lê delas é a soma**: `valorCobrado = pix + dinheiro + débito + crédito`
(`PassagemDadosPassagemMapper.kt:43`), que vira a "receita real" do ADR-0014. Nenhuma consulta filtra por
forma; nenhuma regra do servidor as toca individualmente.

**O que essa forma não consegue registrar** — não por falta de campo, mas por construção:

| O fato | Por que não cabe |
|---|---|
| Dois lançamentos da **mesma forma** (dois cartões, dois pagadores no pix) | colapsam num número só |
| **Quando** se pagou | tudo é implícito "no ato" — não existe pagar depois |
| **Estorno / devolução** | não há o que estornar: cancelar hoje é *delete* físico (§3) |
| **Troco** | cabe o apurado, não o recebido |
| **NSU / autorização / txid** | sem lugar — e é o que permite **conciliar** com o extrato da adquirente |
| **Quem recebeu** | infere-se do emissor; num caixa com turnos, recebedor ≠ emissor |
| **Taxa da adquirente** | a "receita real" do ADR-0014 é **bruta**; a líquida não é derivável |

Três desses batem direto na proposta da plataforma. **Conciliação** (o que o adquirente depositou × o que o
sistema diz) é o insight financeiro mais valioso de uma bilheteria e hoje é impossível. **Receita líquida ×
bruta** muda o número que o gestor lê: crédito com taxa e prazo não é dinheiro em caixa hoje. E **pagar
depois** não é hipótese remota — os interessados do §11.3 (despachante, transportadora) são exatamente quem
opera em conta corrente.

**O argumento que decide, e é o mesmo que o analista já aplicou ao pool (§11.8):** somar por forma no
momento da escrita **descarta informação de forma irreversível**. O lançamento é o **fato**; a soma é
derivável dele, e o inverso nunca. Se a plataforma existe para guardar o dado e extrair insight depois, a
estrutura tem de preservar o evento — "o importante é ter os dados, pode ser até normalizado depois" vale
para o dinheiro tanto quanto para as pessoas.

**O custo é menor do que parece.** A soma deixa de ser leitura de campo, mas **já hoje** ninguém soma no
banco: o balanço lê as passagens e agrega em Kotlin (ADR-0014 §1), e o ADR-0017 remove o SQL de qualquer
forma. Nenhuma consulta atual filtra por forma de pagamento, então **nada quebra no servidor**. E a UI pode
continuar exatamente igual — quatro formas fixas com seus campos —, com o mapper montando os lançamentos: a
mudança é de **registro**, não de tela.

**Decisão de 2026-08-01: lista de lançamentos, embutida, com identidade própria — e só isso.** O item é
`{id, forma, valor}`. **Nada de NSU, txid, taxa ou recebedor no lançamento: seria over-engineering.** O `id`
existe para que promover a lista a coleção `lancamentos` seja **mover, não redesenhar**, se o dia chegar.

Duas das lacunas que a análise listou se resolvem **fora** do lançamento, e melhor:

- **Quando se pagou** → **`criadoEm` e `alteradoEm` na passagem**. Como a emissão é pós-pagamento
  (§11.9b′), o instante da criação **é** o instante do pagamento. E aqui há uma ausência que a decisão
  corrige: **a Passagem hoje não registra quando foi emitida** — tem `dataViagem` (quando se viaja) e
  `embarcadaEm` (quando embarcou), mas nenhum carimbo de criação ou de última alteração. Relatório por
  período hoje só sabe agrupar por data de viagem, não por **quando o dinheiro entrou**.
- **Quem recebeu** → é **auto-explicativo**: quem emitiu recebeu, e o emissor já está congelado
  (`funcionarioId` + `funcionarioResponsavel`, §5). Campo novo seria duplicar fato existente — o mesmo erro
  que o `agente` cometia antes de morrer no ADR-0015.

### 11.9b′ Emissão é pós-pagamento — e por isso o faturamento é outro módulo

**Decisão de 2026-08-01: a emissão é pós-pagamento.** A passagem **nasce paga**; não existe bilhete "em
aberto" dentro do agregado.

Isso dissolve, por decisão de negócio, o eixo "a receber" que a análise apontava como a peça cara — e com
ele os três pontos onde o app assumia pagamento no ato. **Nenhum precisa mudar**, e agora se sabe por quê:

1. `validarDadosPassagem` exigir ao menos uma forma de pagamento (`ValidacaoDadosPassagem.kt:46-48`) não é
   limitação: é **a regra**.
2. `descontoDerivado(devida, cobrado)` continua íntegro. Era o risco maior — bilhete com `cobrado = 0`
   apareceria no ADR-0014 como **receita zero e desconto de 100%** —, e ele **não se materializa**, porque
   não existe passagem sem pagamento.
3. A FSM de emissão (§3) segue com **um eixo só**. Não nasce a segunda máquina de estados, que o §8 mostra
   custar paridade em três camadas.

**O faturamento vira módulo à parte**, a desenhar quando o negócio pedir — não uma extensão da passagem. É
onde a transportadora que embarca vários veículos e acerta em conta será tratada de verdade, e onde os
campos que ficaram de fora do lançamento por over-engineering (§11.9b) voltam a fazer sentido:

- **conciliação** (NSU / autorização / txid) contra o extrato da adquirente;
- **taxa e prazo por forma** — receita líquida × bruta, crédito que cai em D+30 (o ADR-0014 mede bruta);
- **crédito / conta corrente do pagador**, com o pagador saindo do pool por `CNPJ:…` (§11.8) sem entidade
  nova;
- **estorno e devolução**, que hoje não existem porque cancelar é *delete* físico (§3);
- **fechamento de caixa por turno**, que a dupla `criadoEm` + emissor (§11.9b) passa a tornar possível.

O que o módulo **não** deve fazer é voltar a colar isso na passagem: o bilhete registra o que se pagou
naquela venda; o faturamento é a visão do dinheiro através de muitas vendas. São escalas diferentes.

### 11.9c O tipo tarifário é da acomodação — a unidade vendida é o espaço

**Decisão de 2026-08-01.** O tipo tarifário **não é da pessoa**: é da **acomodação**. **"Um camarote vale um
camarote"**, e comporta até três pessoas; a suíte, idem. O que se vende é o **espaço**, e o preço é dele.

Isso responde a pergunta em aberto de forma mais limpa do que qualquer das duas hipóteses que eu havia
levantado. Não é que "todo mundo no camarote sai inteira" — é que **não há pessoa sendo tarifada ali**, logo
meia e gratuidade não têm onde se aplicar. A categoria tarifária pertence ao **modo rede**, o único em que a
unidade vendida coincide com uma pessoa:

| Modo | Unidade vendida | Categoria tarifária |
|---|---|---|
| `REDE` | a pessoa (uma rede, uma pessoa) | **inteira / meia / gratuidade** |
| `SUITE` / `CAMAROTE` | o espaço (até 3 ocupantes) | não se aplica — o espaço não tem idade |
| `VEICULO` | o veículo (classe) | não se aplica |

E **o código já está correto**: `validarPassageiro` só exige tipo tarifário na rede
(`ValidacaoPassageiro.kt:51`) e o ADR-0013 já registrava "tipo tarifário só existe na REDE". O que faltava
era a **razão** estar escrita — sem ela, a ausência do campo em suíte/camarote parecia esquecimento, e foi
assim que a li. `tipoPassagem`/`gratuidade` no bilhete continuam onde estão, e **são nulos por construção**
fora da rede: valor preenchido em outro modo deve ser ignorado, não interpretado (fail-closed).

Um efeito colateral que fecha sozinho: a **cota de gratuidade** (2 por categoria por viagem, ADR-0013) conta
**por passagem**, e como gratuidade só existe na rede — onde uma passagem é uma pessoa —, contar por
passagem já é contar por pessoa. Não há divergência a corrigir.

**Os quatro resíduos, decididos em 2026-08-01:**

1. **A capacidade é do navio** — do **ativo** do ADR-0016, não do tipo de acomodação. Detalhado em §11.9e.
2. **Um bilhete = uma unidade** — "o negócio já está abstraído assim: três nomes e documentos em um
   bilhete". Grupo de cinco em dois camarotes são **duas passagens**. Não existe bilhete de dois camarotes.
3. **A capacidade é atribuída no cadastro do navio, e é isso que dá controle** — §11.9e.
4. **Nomear ≠ tarifar, e o modo governa quem existe.** Os passageiros 2 e 3 são nominados com documento
   obrigatório (§11.8) sem efeito no preço: a lista é o **manifesto de embarque**. E **só faz sentido acionar
   o passageiro 2 em suíte ou camarote** — "outro cliente, tanto no state quanto na apresentação quanto na
   lógica". Ou seja, **cada participante é um `Cliente`** (§11.1), com o seu próprio `clienteId` e o seu
   botão de salvar (§11.2), e **quantos existem é propriedade do modo** (§11.3): rede 1, suíte/camarote até
   a capacidade, veículo nenhum — só o responsável opcional.
   > Consequência de forma a considerar no ADR: hoje o participante é **triplicado por numeração**
   > (`nomePassageiro1/2/3` na entidade, ×3 no UiState, ×3 na validação). Se cada um é o mesmo conceito
   > repetido, o que o modelo pede é uma **coleção de participantes** limitada pela capacidade — não três
   > conjuntos de campos com sufixo. Proposta minha, não decisão.

### 11.9e A capacidade vem do navio — e o estoque passa a ser controlável

**Decisão de 2026-08-01.** A capacidade é do **navio** — o *ativo* do ADR-0016 — e é **atribuída no
cadastro do navio**: quantas redes, quantas suítes, quantos camarotes (e, pelo ADR-0016, se leva veículo e
de que classes). **"Assim se tem controle."**

Duas coisas mudam de estatuto com isso:

- **A tela deixa de codificar o negócio.** O "até 3" que hoje é constante do form (checkbox de passageiro
  2 e 3) passa a ser leitura da capacidade do navio da viagem — e a suíte de um navio pode não ser a suíte
  de outro, que é o que a plataforma multi-empresa exige.
- **Ocupação deixa de ser só relatório.** Hoje o balanço mede ocupação **depois** da venda; com capacidade
  declarada, existe um **estoque** por viagem e a emissão pode confrontá-lo **antes**.

**O mecanismo já existe no app, e é o mesmo da cota de gratuidade.** O ADR-0013 conta o que já foi emitido
naquela viagem (`contarGratuidadePorViagem`, com `excetoId` para a edição não bloquear a si mesma) e
devolve um caso tipado que a UI mostra como bloqueio persistente (`ResultadoEmissao.CotaGratuidadeAtingida`).
Capacidade é o **mesmo padrão** com outro contador: contar as passagens da viagem por modo e comparar com o
declarado no navio, fail-closed, com o mesmo desenho de banner. Herda também a mesma limitação já assumida
lá: a contagem é *firestore-driven* e a **paridade no servidor é parcial** — dois caixas vendendo o último
camarote ao mesmo tempo é a corrida que o cliente sozinho não resolve.

**O que ainda falta desenhar aqui** (não é decisão de negócio, é forma): onde a capacidade mora no
`Navio` — um mapa `{modo → quantidade}` acompanha o eixo do §11.3 e a tabela de tarifa da viagem, que já é
`{chave → valor}`; e se a capacidade de veículo é contagem de vagas ou metragem, que o ADR-0016 trata como
capacidade do ativo.

### 11.9d A numeração é por viagem

**Decisão de 2026-08-01.** O escopo da numeração do bilhete é a **viagem**. Hoje `ContadorBilhete` é **um
documento único** (`id = 1`) com uma contagem global — numa plataforma multi-empresa, todas as empresas
dividiriam a mesma sequência e disputariam o mesmo documento a cada emissão.

O que a decisão traz:

- **A sequência reinicia a cada travessia**, e o número passa a significar algo: *o n-ésimo bilhete daquela
  viagem*. Casa com o manifesto de embarque (§11.9c.4), que é a lista daquela travessia, e com o eixo
  financeiro do ADR-0014, que já agrega **por viagem**.
- **Unicidade passa a ser do par `(viagemId, numero)`** — o número sozinho deixa de identificar. O `id` do
  documento continua sendo a identidade técnica (é ele que o QR carrega, §4), então nada no embarque muda.
- **O contador vira um por viagem** — `contadores/{viagemId}` ou campo na própria viagem. E a concorrência
  fica **mais concentrada**: dois caixas da mesma travessia disputam o mesmo contador, o que hoje é
  ler-somar-gravar (`adicionarContador`, ainda com `runBlocking` — dívida do §9). Incremento atômico do
  servidor (`FieldValue.increment`) resolve, e é o mesmo tipo de garantia que a capacidade do §11.9e vai
  precisar. **Numerar e ocupar são o mesmo problema de corrida** — vale resolvê-los juntos.
- **Reemissão não renumera** — já é o comportamento atual (`numero = passagemExistente?.numero ?: novo`).

## 11.10 As resoluções de forma (2026-08-01)

### 11.10a A viagem é atômica por dia da semana e hora — a passagem aponta para a ocorrência

**Decisão.** A **Viagem** é atômica em **dia da semana + hora** — "terça-feira, 15:00". Ela é a **escala**, e
o sistema **infere as ocorrências no intervalo de segunda a domingo**. O que a passagem consome é o **item
de viagem**: a ocorrência que **grava o dia exato** e guarda o que é daquela travessia específica, para
controle e apresentação.

**Os atalhos de criação de passagem continuam funcionando como hoje** — a tela lista a escala da semana e a
emissão se ancora no item do dia. É o que torna a mudança barata: muda o que a passagem aponta, não como o
operador chega nela.

Isso resolve o "o que se congela" da tabela: a passagem congela a **ocorrência**, não a escala. E força uma
precisão em duas decisões já tomadas, que sem ela ficariam ambíguas:

- **A numeração por viagem (§11.9d) é por ocorrência.** O n-ésimo bilhete *daquela travessia de terça 15:00
  do dia 12*, não da escala das terças. Contador na ocorrência.
- **A capacidade / estoque (§11.9e) também é por ocorrência.** O navio tem 10 camarotes **em cada saída** —
  vender contra a escala misturaria semanas inteiras.
- O eixo financeiro do ADR-0014, que agrega por `viagemId`, passa a agregar **por ocorrência** — que é o que
  "receita da travessia" sempre quis dizer.

> Conversa direto com o ADR-0016 (**Rota** = a viagem de verdade, com portos, tarifas e **agenda**) e com a
> nota de vocabulário do [viagem × trecho](viagem-vs-trecho.md). O que este parágrafo acrescenta é a
> **atomicidade**: a unidade da agenda é `(dia da semana, hora)`, e a ocorrência é a materialização dela numa
> data.

### 11.10b A agência do emissor entra por id

**Decisão.** `agencia` deixa de ser só texto: a passagem passa a congelar **`agenciaId` + o nome como
snapshot**, o mesmo par que já vale para viagem, navio, empresa e funcionário (§5, ADR-0008). A agência é
entidade desde o ADR-0015; faltava a passagem tratá-la como tal.

Ganho concreto: o recorte por agência da consulta (ADR-0015 P2.6) e a identidade visual por agência no
bilhete passam a depender de **id**, não de casamento de nome — que é exatamente o tipo de fragilidade que o
ADR-0008 nasceu para matar. O nome continua no bilhete impresso, congelado.

### 11.10c Embarque: o estado é o status; o que vira sub-objeto é o carimbo

**Esclarecimento** — a pergunta da tabela não era sobre o estado. O **estado continua sendo o
`status = EMBARCADA`** da FSM (§3); nada disso muda. O que está em discussão são os **três campos do
carimbo** — `embarcadaPorId`, `embarcadaPor`, `embarcadaEm` —, hoje planos e com default `""`.

**Decisão: viram um sub-objeto `embarque` ausente quando não houve embarque.** O ganho é tornar
**impossível de representar** o estado meio-preenchido (carimbo vazio com status `EMBARCADA`, ou carimbo
preenchido com status `EMITIDA`): ou o objeto existe inteiro, ou não existe. Hoje isso só se detecta lendo
três strings vazias e interpretando.

Custo a assumir junto: a regra do servidor `ehConfirmacaoEmbarque()` endurece exatamente esses quatro nomes
de campo (`hasOnly(['status','embarcadaPorId','embarcadaPor','embarcadaEm'])`, §8) e a suíte de emulador
cobre isso. Mudar a forma **muda a regra e o teste no mesmo incremento** — é o dever de paridade do §8, e
não dá para fatiar em dois.

### 11.10d Placa: máscara na entrada, e o traço é do padrão antigo

**Decisão.** A normalização da placa (§11.9a) **não é cara**: resolve-se com **controle de caractere no
campo**. O traço está no teclado numérico, e a máscara **aceita** o traço digitado e **o insere sozinha**
quando faltar — a regra é a própria forma da placa:

- **antigo `LLL-NNNN`**: três letras seguidas de **quatro dígitos** → leva traço (inserido se o operador não
  digitar);
- **Mercosul `LLLNLNN`**: três letras, dígito, **letra**, dois dígitos → **não** leva traço.

Ou seja, a grafia canônica **é a oficial de cada padrão**, não "tudo sem traço": o que a máscara garante é
que a mesma placa só tenha **uma** grafia possível ao entrar. Como a placa é a chave natural do pool
(§11.9a), isso é o que impede `ABC-1234` e `abc1234` de virarem dois veículos — e é validação de fronteira,
o mesmo lugar onde os enums do domínio já fazem `de()`.

## 12. Referências

- [ADR-0017](../adr/0017-eixo-de-storage-firestore-only.md) — Firestore-only (a F5 é esta entidade)
- [ADR-0016](../adr/0016-dominio-da-plataforma.md) — domínio da plataforma
- [ADR-0015](../adr/0015-rework-agente-equipe.md) — equipe, agência e cargo (matou o `agente` do bilhete)
- [ADR-0013](../adr/0013-tabela-de-tarifa-e-tipo-tarifario.md) — tarifa tabelada e tipo tarifário
- [ADR-0012](../adr/0012-ciclo-de-vida-passagem-e-embarque-qr.md) — ciclo de vida + embarque por QR
- [ADR-0008](../adr/0008-relacionamentos-por-identidade.md) — relacionar por identidade
- [ADR-0010](../adr/0010-autorizacao-por-cargo.md) / [ADR-0011](../adr/0011-regras-firestore-por-cargo.md) — autorização e regras
- [Catálogo do domínio da plataforma](dominio-da-plataforma.md) · [Estudo do form de passagem](form-passagem-validacao-exibicao.md) · [Estudo da camada de apresentação](camada-de-apresentacao.md)
