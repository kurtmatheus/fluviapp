# ADR-0017: Eixo de storage — Firestore-only (o Room deixa de ser datasource)

**Status:** **Aceita (direção)** — decisões do analista em 2026-07-31 (perguntas 1 a 6 do estudo). Sem
código: este ADR fixa o eixo e o plano; a implementação é faseada abaixo. Formaliza o
[estudo do eixo de storage](../design/eixo-de-storage-firestore-only.md) e **realiza a Opção 3 que o
[ADR-0009](0009-sincronizacao-reativa-firestore-room.md) registrou e adiou** — *"trocar o eixo de storage
(Firestore-only / estratégia offline) — mudança estrutural maior; ortogonal e adiada para um ADR futuro"*.
**Supera parcialmente o [ADR-0003](0003-modelo-de-memoria-do-dado.md)**: o nível "cacheada" do modelo de
memória deixa de ser o Room e passa a ser o cache do SDK.

> Conversa com o [ADR-0003](0003-modelo-de-memoria-do-dado.md) (Firestore = verdade, Room = espelho),
> o [ADR-0009](0009-sincronizacao-reativa-firestore-room.md) (pipeline único, porta `FonteSnapshots`,
> telemetria `sync_*`), o [ADR-0004](0004-snapshot-e-observabilidade-emissao.md) (rascunho local
> não-autoritativo), o [ADR-0005](0005-autenticacao-sessao-firebase-datastore.md) (sessão no DataStore),
> o [ADR-0011](0011-regras-firestore-por-cargo.md) (regras por cargo) e o
> [ADR-0016](0016-dominio-da-plataforma.md) (domínio da plataforma — cuja F1 entra aqui como piloto).
> Ancorado no código em `2026-07-31`.

**Premissa (não em debate):** design **firestore-driven** — o Firestore é a fonte da verdade e o motor do
fluxo. Este ADR não questiona isso; decide apenas **se ainda vale existir uma segunda persistência local
espelhando a primeira**.

---

## Contexto

### A direção da plataforma

O app deixa de ser o sistema de **uma** empresa e se torna uma **plataforma de gerenciamento de empresas**,
no nível de negócio da própria plataforma. Por isso o ADR-0016 veio primeiro: **o domínio é o que a
plataforma organiza**. O sistema nasce **mobile-first** — atender o cliente e o negócio dele já montando as
bases da plataforma — e, nesse regime, **o Firestore provê estrutura suficiente**, como acesso rápido.

Três eixos, nesta ordem: **domínio → mobile-first → Firestore-only**.

Informação **mais sensível e mais precisa** vai exigir, adiante, um **sistema centralizador (back-end)
próprio da plataforma**. Isso **não** reabre o espelho local: é um movimento *acima* da fronteira do app,
enquanto este ADR decide o que existe *abaixo* dela. **"Firestore-only" não significa "Firestore para
sempre" nem veto ao back-end** — significa uma persistência local em vez de duas, agora.

### O que o Room é hoje

11 entidades, 11 DAOs, 1 banco (`FluviAppDatabase`, v2). **Ninguém fora dos repositórios toca um DAO** — o
grep de `Dao` em `app/src/main` só acerta `database/**`, `di/module/DatabaseModule.kt` e os 9 repositórios
mais o store de rascunho. Nenhum ViewModel, nenhum mapper, nenhuma tela. A fronteira já está onde precisa
estar para uma troca de eixo.

Três constatações mudam o peso da decisão:

1. **Já são dois caches em disco, não um.** Não há nenhuma chamada a `setPersistenceEnabled` /
   `setLocalCacheSettings` no projeto: vale o default do SDK Android, **persistência ligada**. O app já
   mantém o cache offline do Firestore (LevelDB) e um SQLite por cima, com o mesmo conteúdo. O Room **não é
   o que dá offline ao app** — é uma segunda cópia do offline que já existia.
2. **Não existe SQL de verdade para perder.** A superfície somada dos 11 DAOs é `SELECT *` por `id`,
   `SELECT *` sem filtro, ~6 igualdades de campo único, um `SELECT agencia`, dois `COUNT(*)` (um deles sem
   chamador) e três `DELETE` por chave. **Zero JOIN, zero GROUP BY, zero range, zero ORDER BY.** O custo que
   o ADR-0003 previa envelheceu inteiro: `obterPorCodigo` morreu no ADR-0008, a agregação do balanço já é
   Kotlin, e as duas consultas pesadas de passagem — contagem e pesquisa — **já vão direto ao Firestore**
   (o `dao.salvarTodas(passagens)` que vem depois **ninguém lê**).
3. **O espelho já é reconhecidamente furado.** É *upsert-only* (documento apagado no servidor fica local
   para sempre — só `TarifaViagem` resolve, com `delete+insert`, e o comentário no
   `ViagemFirestoreRepository` explicita a assimetria); tem buraco de **cold start** (o
   `FuncionarioFirestoreRepository.obterPorEmailDoServidor` pula o Room de propósito, porque no primeiro
   acesso o espelho está vazio — o buraco está na janela mais crítica, o login); e a emissão grava o Room,
   segue a UI e dispara `set(...)` *fire-and-forget*, de modo que uma recusa do servidor deixa o bilhete só
   naquele aparelho, **para sempre**, com um warning como único desfecho.

## Opções consideradas

1. **Status quo** — manter o espelho. Custo recorrente de schema, duas cópias do mesmo offline, e os três
   furos acima permanecem, cada um com correção própria a construir.
2. **Consertar o espelho** — reconciliação de remoção, cold start e divergência de escrita. Resolve os
   sintomas construindo mais máquina em cima da duplicação que os causa.
3. **Firestore-only (esta)** — o cache do SDK assume o papel de camada local; a fonte reativa passa a ser
   a porta `FonteSnapshots` (ADR-0009 §10) direto para a UI; o Room sai.
4. **Room mínimo só para o resíduo local** (§D4) — mantém o SQLite vivo por três tabelas que não são
   espelho. **Rejeitada pelo analista**: paga o custo inteiro de schema por dado que o DataStore e o
   sistema de arquivos guardam melhor.

## Decisão

Opção 3. **Uma persistência local em vez de duas, e a que fica é a que o SDK já mantinha de graça.**

- **D1 — A fonte reativa deixa de ser o DAO.** `sincronizarColecao` mantém idempotência, `parar()` no
  logout e telemetria; só troca o destino: em vez de `salvarTodos` numa tabela, publica num **`StateFlow`
  compartilhado por coleção** (`shareIn(syncScope, replay = 1)`). A porta `FonteSnapshots` já emite
  `DocumentoBruto` **neutro** com `doCache` — foi criada para testar o ciclo de vida sem Firebase, e é
  exatamente a peça que permite trocar a fonte **sem** vazar tipo Firebase para cima. O
  `MainScreenViewModel.observarViagens()` não muda de forma nenhuma: continua coletando um Flow.
- **D2 — As portas de repositório não mudam de assinatura.** `obterTodosPorCategoria` continua `suspend` e
  os 5 chamadores continuam iguais; muda só de onde a impl tira a resposta — do `StateFlow`, não do DAO.
  **Mudança de comportamento a assumir:** hoje `dao.…first()` responde na hora, eventualmente com lista
  vazia se o sync ainda não gravou; a partir daqui a leitura **espera a primeira emissão** (que vem do
  cache, portanto rápida). Trocamos um resultado vazio silencioso por uma espera curta — é o comportamento
  correto, mas precisa de teste que o fixe.
- **D3 — Escrita vai direto ao documento; a fila offline do SDK responde pela durabilidade.** Deixa de
  existir gravação local "de sucesso" e, com ela, deixam de existir dois registros que podem discordar para
  sempre. A divergência do *fire-and-forget* morre **por construção**, não por tela nova (§D7).
- **D4 — O resíduo local sai do Room, cada um para a casa certa.** São as três coisas que não são espelho:
  - **`rascunho_passagem`** (snapshot do ADR-0004) → **DataStore**. A porta `RascunhoStore` já existe e
    `RascunhoPassagemStoreRoom` é "o único ponto que conhece o mecanismo de persistência": troca-se a impl
    e nenhum chamador percebe.
  - **`Usuario.ultimoUsuarioLogado`** → **DataStore**, junto das chaves de sessão que já moram lá desde o
    ADR-0005 (`USUARIO_ATUAL`/`PAPEL_ATUAL`/`CARGO_ATUAL`). É flag de sessão, não dado de negócio.
  - **`PassagemDigital`** → **a tabela morre**; o PNG vai para a **galeria (MediaStore)** e a re-emissão
    **recupera do próprio arquivo** (§D5).
- **D5 — O bilhete digital vai para a galeria, com nome derivado da passagem.** Hoje o arquivo nasce em
  `getExternalFilesDir(DIRECTORY_PICTURES)` — diretório privado, que **não** é a galeria — com nome
  `passagem_<timestamp>.png` (`PassagemDigitalHelper.kt:47`). **É o timestamp que obriga a tabela a
  existir**: o arquivo não é localizável a partir do `idPassagem`, então a linha do Room é o único mapa
  `idPassagem → caminho`. Destino e nome mudam **juntos** — trocar só o destino deixaria a re-emissão cega.
  Com o nome derivado, a busca vira consulta ao MediaStore e o índice local perde a função.
  **Arquivo ausente não é erro, é regenerar:** o bilhete é renderizado a partir da `Passagem`, que está no
  Firestore. Ele nunca foi dado de origem — é cache de conveniência, o mesmo estatuto do cache do SDK.
- **D6 — O offline passa a ser o cache do SDK, e isso é escolha declarada.** Hoje a persistência está
  ligada por *default herdado*; passa a ser **configuração explícita** (`PersistentCacheSettings`), para que
  o offline seja uma decisão visível no código e não um acidente de default. Fica aceito que o cache é
  **LRU com teto** e que **não há garantia de retenção** — o Firestore poda quando quer. Se algum fluxo
  precisar de "tudo que já vi continua aqui", a resposta é armazenamento próprio **para aquele fluxo**, não
  o espelho inteiro de volta.
- **D7 — Emissão rejeitada fica fora deste ADR.** A rejeição prática é **passagem sem tarifa** (célula
  ausente é *fail-closed* no ADR-0013); os pré-requisitos de emissão serão sanados antes da homologação e
  **não há passagem antiga** — o app não tem produção. Sobra a recusa por regra de agência/cargo
  (ADR-0011), que **não é caso de negócio**: é uso indevido ou bug, e o lugar dela é a telemetria, não um
  fluxo de UI. *"Fora de escopo" não quer dizer "não pode acontecer"* — quer dizer que não se constrói tela
  para isso agora.
- **D8 — Critério de pronto: a telemetria é a única prova.** Sem tabela para inspecionar, `sync_*` deixa de
  ser conforto e vira o instrumento. `FakeFonteSnapshots` já existe e os testes de ciclo de vida
  (`SincronizarColecaoTest`) sobrevivem à troca porque testam a **porta**, não o DAO. Os 52 arquivos de
  teste JVM **não instanciam Room** — a suíte não sente a remoção.

## O piloto: `Catalogo` (F1)

O primeiro incremento junta o **piloto do eixo** e a **F1 do ADR-0016** (`Constante` → `Catalogo`), mais a
entrada de menu com **CRUD restrito ao ADM** — o primeiro pedaço concreto do painel administrativo. A
coleção foi escolhida por ser a mais simples do inventário: duas consultas por igualdade, cinco chamadores,
todos `suspend` one-shot.

**Isto desbloqueia o ADR-0016.** O *Ponto aberto 1* de lá diz que renomear a entidade *"toca
`FluviAppDatabase`, o `DDL_V2` e o `schemas/…/2.json`"* e conclui: **"é o único bloqueio para começar"**.
Sem espelho, **não existe entidade Room para renomear** — o bloqueio não é resolvido, deixa de existir. O
mesmo vale para o *Ponto aberto 5* ("a Rota precisa de espelho no Room?"): a resposta passa a ser **não,
por construção**. Dois pontos abertos do ADR-0016 fecham como efeito colateral deste ADR.

Há um segundo limite que cai junto. O ADR-0016 §3 registra que renomear a coluna `descricaoNome` → `nome`
*"fica fora deste round"* por causa do Room. Acontece que **no documento o campo já se chama `descricao`**
(`ConstanteDocumento.kt:6`) — `descricaoNome` só existe do lado Kotlin/Room. Sem a entidade, o nome do
modelo acompanha o do documento sem negociação com schema nenhum.

**Ordem dentro da fatia.** O incremento carrega três mudanças — eixo, rename de domínio e tela com política
de acesso. É mais do que um piloto normalmente deveria carregar, e se algo quebrar a causa fica ambígua.
Não é motivo para dividir em três fatias (as três recaem sobre a coleção mais simples do app), mas é motivo
para **ordenar dentro dela**:

1. **o eixo primeiro**, com o nome antigo — a coleção passa a ler do `StateFlow`, e o padrão se prova
   isolado, sem nenhuma outra mudança por cima;
2. **depois o rename** `Constante` → `Catalogo` (com as remoções da F1: `Descricao`, `Categoria`, e
   `IObjetoSimplificado` restrito ao catálogo);
3. **por último o menu e o CRUD.**

**A escrita entra em cena aqui.** Até este ponto o estudo tratou só de leitura; o CRUD do ADM é o primeiro
caso de **escrita de cadastro sem Room** (D3), numa tela — não na emissão. É de propósito: o piloto expõe o
regime novo no lugar mais barato.

**Duas consequências de política, que o ADR-0016 herda:**

- **É a primeira vez que `ADM` e `GESTOR` se separam.** A política de hoje trata os dois como um bloco só
  (`ehPapelPlataforma`, e `podeAcessar` devolve isso para `VIAGEM`/`EMPRESA`/`NAVIO`). "CRUD só ao ADM"
  exige um predicado mais estreito — `Papel.de(papel) == ADM` — e é a semente da distinção entre *operar a
  plataforma* e *administrar a plataforma*.
- **Abrir escrita exige regra no servidor.** Segurança por UI não basta desde o ADR-0011: a nova coleção
  `catalogo` precisa de `firestore.rules` com escrita **só por `ADM`** e da suíte de emulador
  correspondente, no mesmo incremento. Regra escrita depois é regra que passou um tempo aberta.

Sobre renomear a coleção `constants` → `catalogo`: **não há dado de produção**, o conteúdo vem do
`SeedFirestore`, então isto é **regenerar, não migrar** — o seed passa a escrever a coleção nova. (Matar o
seed continua sendo a F2 do ADR-0016; este CRUD é o primeiro caminho pelo qual um humano escreve catálogo.)

## Plano de migração (faseado)

Cada repositório é dono do próprio pipeline, então dá para migrar **uma coleção de cada vez** com o app
compilando e a suíte verde a cada passo — mesma tradição do ADR-0009 (D1→D5) e do ADR-0015.

- **F1 — Piloto: `Catalogo`.** Como acima, nos três tempos (eixo → rename → menu/CRUD + regra).
- **F2 — Resíduo local** (D4/D5): `RascunhoStore` com impl DataStore, `ultimoUsuarioLogado` junto das
  chaves de sessão, e o bilhete digital para a galeria. O passo do bilhete tem conteúdo próprio (destino,
  nome e permissão) e merece fatia sozinho.
- **F3 — Cadastros:** Empresa, Navio, Funcionario — e o `UsuarioRepository`, que ficou fora do saneamento
  do ADR-0009 (§Achados).
- **F4 — Viagem + TarifaViagem:** o caso reativo de verdade (Main Screen) e o achatamento do mapa de
  tarifas. Se a F7 do ADR-0016 (`Viagem` → `Rota`) chegar antes, esta fase é onde as duas se encontram.
- **F5 — Passagem + Contador:** o mais fácil, ironicamente — as consultas já são Firestore. É remover o
  `salvarTodas` órfão e trocar `obterPorId` por `document(id).get()`.
- **F6 — Remover o Room:** entidades perdem anotação, `DatabaseModule` some, `room-compiler` sai do KSP.

**A ordem entre F1 e F2 foi decidida — piloto antes** (analista, 2026-07-31): o resíduo é trabalho garantido
em qualquer cenário (seria feito mesmo se o eixo fosse abandonado), enquanto o piloto é o que pode **mudar o
plano**; informação que muda plano vale mais cedo.

## Consequências

**O que se ganha**

- **Uma persistência local em vez de duas**, e a que fica é a que o SDK já mantinha de graça.
- **Fim do espelho que mente:** remoção remota reflete, o cold start do login deixa de ter buraco, e a
  emissão passa a ter um registro só (a fila offline) em vez de dois que podem discordar para sempre.
- **Uma tradução a menos:** `Documento → modelo → SQLite → modelo → UI` vira `Documento → modelo → UI`.
- **O domínio para de conhecer o banco:** `Passagem.kt` importa `androidx.room`, e `temPassageiro2` — regra
  de negócio — carrega um `@Ignore`. Anotação de persistência dentro do agregado é exatamente o que o
  ADR-0003 queria questionar.
- **Some o custo recorrente de schema:** o DDL do `DatabaseModule`, o `fallbackToDestructiveMigrationOnDowngrade`,
  o `room.schemaLocation` e a obrigação (ADR-0015 §9) de reexportar o `createSql` a cada mudança de entidade.
- **Destrava o ADR-0016** (pontos abertos 1 e 5) e alinha com ele: catálogo dinâmico, multi-empresa e
  multi-segmento brigariam com schema tipado toda semana. Um domínio que ainda vai crescer em forma é o pior
  candidato a DDL congelado.

**O que se paga**

- **O cache é do SDK, não seu.** LRU com teto, podado quando o SDK quiser. O Room era previsível: o que
  entrou, ficou. Concretamente: um bilhete antigo pode não estar no cache e a busca offline volta menor,
  **sem aviso**. Aceito em D6.
- **A galeria é do usuário.** Ele apaga o bilhete quando quiser; e após uma reinstalação o app perde a posse
  da entrada no MediaStore (precisaria de `READ_MEDIA_IMAGES` para reencontrá-la). Reinstalou, re-emitiu,
  gera de novo — aceitável porque o bilhete é derivado, mas precisa estar escrito para não virar bug
  reportado depois.
- **Sem SQL de escape para consulta nova.** Enquanto for **só igualdade e sem `orderBy`**, o Firestore
  resolve com merge de índices simples — é o caso de **todas** as consultas atuais. O custo nasce quando
  entrar **range/ordenação** (relatórios do ADR-0016), e aí índice composto é configuração de infra.
- **Índices não são versionados.** `firebase.json` declara só `rules` e os emuladores; não há
  `firestore.indexes.json`. Já é dívida hoje; **vira dívida crítica quando o SQL não estiver mais lá**. O
  gatilho para pagá-la é a primeira consulta com range ou ordenação.
- **Leitura tem preço.** SQLite era grátis; documento lido do servidor, não. Mitigado pelo cache e pelos
  listeners (que cobram deltas) — e é o regime em que as consultas de passagem já operam.
- **Coleção espelhada vira memória.** Os cadastros passam a viver num `StateFlow` por coleção. São coleções
  pequenas. **`passagens` nunca foi espelhada por listener**, então nada muda lá — e isso é limite
  explícito, não omissão.

**Reversibilidade.** Barata: o Room aqui é derivado — se voltar, re-sincroniza do Firestore. O único dado
insubstituível é o resíduo local (D4), e ele sai atrás de porta justamente por isso. O risco real não é
técnico, é de **regime**: aceitar que a completude do offline passa a ser gerida pelo SDK, não pelo app.

## O que este ADR não decide

- **Mudar a forma do dado** (blob JSON / DTO-cêntrico, passo 2 do ADR-0003). Aqui o modelo Kotlin continua
  idêntico — só perde as anotações. São dois movimentos diferentes e não precisam acontecer juntos.
- **O back-end centralizador.** Está na direção da plataforma (Contexto), mas é outro ADR: acontece *acima*
  da fronteira do app.
- **Desfecho de UI para emissão rejeitada** (D7).
- **Dívidas colaterais que o caminho expõe**, e que valem fatia própria:
  `UsuarioRepository.carregarUsuarios()` ficou fora do saneamento do ADR-0009 (listener vazando,
  `runBlocking` por documento e um `throw` dentro do callback — os três smells que o ADR-0009 matou nos
  outros); `PassagemDigitalHelper` e `PassagemFirestoreRepository.adicionarContador` ainda usam
  `runBlocking`; e `ViagemDao.obterContagem()` não tem chamador — código morto que some sozinho na F6.