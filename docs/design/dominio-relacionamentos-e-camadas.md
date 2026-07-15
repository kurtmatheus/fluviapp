# Estudo de design do domínio — entidades, relacionamentos e camadas de dados

> Estudo-base do FluviApp. Alimenta o [ADR-0008](../adr/0008-relacionamentos-por-identidade.md)
> (relacionar por identidade) e conversa com o [ADR-0003](../adr/0003-modelo-de-memoria-do-dado.md)
> (modelo de memória do dado). Ancorado no código concreto em `2026-07`.

## 1. O que o domínio modela

Negócio: **venda e emissão de passagens de balsa fluvial**. Nove entidades persistidas, mais
*value objects* embutidos e projeções de tela (`screendata`).

| Entidade | Papel de negócio | Identidade | Natureza |
|---|---|---|---|
| **Empresa** | operadora da balsa | `id` + `nome`, `cnpj` | master data |
| **Navio** | embarcação (capacidades por acomodação) | `id` + `descricaoNome` | master data |
| **Viagem** | uma travessia (código, origem→destino) | `id` + `codigo` | master data / operacional |
| **Passagem** | o bilhete emitido (~50 campos planos) | `id` + `numero` | **transacional / evento** |
| **Agente** | vendedor (agência, lotação, capability) | `id` + `descricaoNome` | master data |
| **Constante** | catálogo de vocabulários (tipos, docs, acomodações…) | `id` + `descricaoNome` | referência |
| **Usuario** | operador logado (cargo → permissões) | `id` + `email` | suporte / auth |
| **ContadorBilhete** | sequência do número do bilhete (linha única `id=1`) | fixo | serviço de numeração |
| **PassagemDigital** | caminho do arquivo do bilhete (Room-only) | `idPassagem` | suporte local |

**Value objects** (sem coleção/entidade própria, embutidos em `PassagemDocumento`):
`PassageiroDocumento` ×3 e `VeiculoDocumento`. É um sinal de DDD já presente no código:
**Passagem é um agregado** cujo DTO aninhado (Firestore) é a forma natural, e cuja forma plana
(Room, ~50 colunas) é o artefato de tenacidade discutido no ADR-0003.

## 2. Como as entidades se relacionam

```
Empresa ─1:N─> Navio          (Navio.empresa)
Empresa ─1:N─> Viagem          (Viagem.empresa)
Navio   ─1:N─> Viagem          (Viagem.navio)
Viagem  ─1:N─> Passagem        (Passagem.codigoViagem + campos copiados da viagem)
Agente  ─1:N─> Passagem        (Passagem.agente / agencia)
Passagem ─1:1─> PassagemDigital (PassagemDigital.idPassagem)
Passagem ─contém─> Passageiro×3, Veiculo   (value objects — agregado)
Constante ─catálogo─> alimenta vocabulários em Passagem/Navio/Agente
```

### 2.1 O achado central: todo relacionamento vivo é feito por NOME, não por id

- `Navio.empresa: String` guarda o **nome** da empresa — o dropdown faz `listaEmpresas.map { it.nome }`
  (`ContentNavioAreaForm`), então o que se seleciona e persiste é o rótulo.
- `Viagem.empresa` e `Viagem.navio`: idem.
- `Passagem` copia `empresa`, `navio`, `origem`, `destino`, `codigoViagem`, `agente`, `agencia`
  como strings soltas (`PassagemDocumento.toPassagem`).

As entidades **têm** `id` surrogate estável (auto-id do Firestore), mas os relacionamentos **não o
usam**. Isso mistura dois conceitos que precisam ser separados:

**(A) Referência viva por chave natural mutável — bug latente.**
`Navio→Empresa`, `Viagem→Empresa`, `Viagem→Navio` são relações entre *master data mutável*. Usar o
nome como chave estrangeira significa: **renomear uma Empresa órfã silenciosamente** todos os
Navios/Viagens que apontavam pro nome antigo. É o clássico *natural key vs surrogate key* — e a
ambiguidade fica pior se dois registros compartilham nome.

**(B) Snapshot histórico por valor — correto, não é bug.**
`Passagem` copiar os dados da Viagem é **apropriado e desejável**: um bilhete é um registro
imutável. Um bilhete impresso ano passado *não deve* mudar se a Viagem for editada depois. Em DDD,
Passagem é agregado próprio e os dados da viagem são um **value snapshot** do momento da emissão.

> A melhoria **não** é "normalizar tudo". É separar os dois casos: **referências vivas por `id`**
> (nome resolvido na leitura) e **snapshots por valor** onde o histórico importa (Passagem). Hoje o
> código usa a mesma técnica — copiar o nome — para os dois, e só um está certo.

### 2.2 Nuance madura: um snapshot pode carregar id **e** valor

`BalancoPassagensMapper` precisa das *capacidades* do Navio (master data vivo) para uma Passagem
(snapshot). Hoje ele casa por nome: `navioRepository.obterTodos().extrairPorDescricao(viagem.navio)`.
Se o Navio for renomeado, passagens antigas não casam mais. Logo, a Passagem deveria guardar:

- **value snapshot** (nome/origem/destino da viagem) → registro histórico imutável para exibição;
- **id estável** (`viagemId`/`navioId`/`empresaId`) → link para relacionar com o master data vivo
  (agregação, filtros).

Isto é o design maduro: um agregado transacional legitimamente guarda **id para relacionar** e
**valor para lembrar**.

## 3. Contrato de identidade — inconsistente hoje

`IObjetoSimplificado` (`id` + `descricaoNome`, com `extrairPorId`/`extrairPorDescricao`/`mapDescricao`)
é a abstração certa para "coisa selecionável por nome, resolvível por id". Mas só
**Navio/Agente/Constante** a implementam. **Empresa** usa `nome` (não `descricaoNome`), **Viagem**
usa `codigo`, **Usuario** não tem contrato. Padronizar destrava resolução id↔nome genérica (o
primitivo `extrairPorId` já existe) e uniformiza dropdowns/lookup.

## 4. As camadas de dados (ADR-0003)

```
Firestore (VERDADE) ──addSnapshotListener──> Room (CACHE espelho) ──Flow/DAO──> UI state + screendata (VOLÁTIL)
   sólida               sincronizarColecao       cacheada                          edição viva
      ^                                                                               |
      └───────────── repository.salvar (Room otimista + ack Firestore + telemetria) ──┘
```

- **Firestore = origem de sincronização/verdade**; **Room = cache de leitura** (toda leitura vem de
  DAO); **UI = estado volátil**. REST/Retrofit removido (era morto — ADR-0003 passo 4).
- Escrita no molde (ADR-0006/0007): Room otimista → aguarda ack → `salvou`/`pendenteDeSync`/`falhou`.
- Falta o nível **rascunho/snapshot** (cacheada durável não-autoritativa) para a emissão de Passagem.

## 5. O SQL do Room ainda é necessário? Dá pra ir 100% Firestore?

O que o Room/SQL **de fato faz** hoje (todas as `@Query` levantadas):

| Uso | Queries reais | Firestore cobre? |
|---|---|---|
| Lookup por chave | `obterPorId/Nome/Codigo/Email` | Sim (`get` / `where`) |
| Lista filtrada | `Constante.obterTodosPorCategoria`, `Agente.obterTodosPorAgencia` | Sim (`where`) |
| Projeção/DISTINCT | `Agente.obterTodasAgencias` | Não (filtra em memória) |
| Count | `Viagem.obterContagem` | Sim (`count()` agregado) |
| **Agregação (balanço)** | `BalancoPassagensMapper` | **já é feito em Kotlin, não em SQL** |
| Stream reativo | DAOs retornam `Flow` | Sim (snapshot listener é reativo) |
| Offline | leitura local | **Firestore tem persistência offline nativa** |

**Conclusão honesta:** para as necessidades reais deste app, o Room é **em grande parte redundante
com a persistência offline nativa do Firestore**. O SQL quase não é usado *como* SQL — a única carga
analítica (balanço) já roda como `groupBy` + contagem manual em Kotlin (com `runBlocking`, dívida
conhecida). Room-espelho e cache de disco do Firestore fazem ~90% da mesma coisa.

O que o Room agrega além do cache do Firestore: (1) expressividade SQL (joins, DISTINCT) que o app
**mal usa**; (2) schema tipado em compilação (a "tenacidade" do ADR-0003); (3) `PassagemDigital` é
**Room-only** (arquivo local, nunca sincroniza) — precisa de storage local, mas caberia em
DataStore/tabela isolada.

### Três direções (eixo de storage)

1. **Manter Room como cache puro e consertar os relacionamentos.** ⭐ Não mexe no eixo SQL×NoSQL;
   ataca o bug real (relacionar por id). Corrige correção sem migração de storage arriscada e deixa
   qualquer futuro mais limpo. **É o escopo do ADR-0008.**
2. **Colapsar para Firestore-only** (endgame "dinamismo" do ADR-0003): persistência offline + `Flow`
   de snapshot + mappers de normalização em memória + `runTransaction` no contador. Migração barata
   (Room é espelho, a verdade re-sincroniza). Custos: perde schema tipado; reimplementa queries;
   `PassagemDigital` à parte; **custo de leitura por documento** e query fraca do Firestore.
3. **Híbrido**: Room só onde se paga (Passagem + PassagemDigital); catálogos read-only
   (Empresa/Navio/Constante/Agente) servidos direto do cache do Firestore.

**Recomendação:** começar pela **1** — o "por-nome" é bug de correção latente e **ortogonal** ao
storage; consertá-lo vale sob qualquer futuro. Decidir 2/3 depois, deliberadamente, em ADR próprio.

## 6. Smells adjacentes revelados pelo estudo

- **N+1 + `runBlocking` no `BalancoPassagensMapper`**: por passagem faz `obterPorCodigo` +
  `obterTodos` (perf + threading; dívida anotada no ADR-0006).
- **DIP quebrado**: `BalancoPassagensMapper` depende do concreto `ViagemFirestoreRepository`, não da
  porta — inconsistente com o molde.
- **Contrato de identidade inconsistente** (§3).
- **Dupla fonte de vocabulário**: enums hardcoded (`Constante.Descricao/Categoria`,
  `Agente.Agencia/Lotacao`) convivem com o catálogo `Constante` sincronizado — duas verdades.
- **ContadorBilhete** como linha única sincronizada é frágil pra sequência monotônica
  multi-dispositivo — pede transação atômica (`runTransaction`/`FieldValue.increment`).
