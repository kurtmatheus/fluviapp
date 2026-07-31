# Estudo de design — Eixo de storage: Firestore-only (aposentar o Room como datasource)

**Status:** **Fechado** — todas as perguntas do §10 decididas pelo analista em 2026-07-31 e formalizadas no
[ADR-0017](../adr/0017-eixo-de-storage-firestore-only.md). Este documento fica como o **estudo que originou
a decisão** (inventário, medições e alternativas); o que vale daqui para frente é o ADR. Realiza a
**Opção 3** que o [ADR-0009](../adr/0009-sincronizacao-reativa-firestore-room.md)
registrou e adiou explicitamente: *"Trocar o eixo de storage (Firestore-only / estratégia offline) —
mudança estrutural maior; ortogonal e adiada para um ADR futuro"* (§Opções, item 3) e, nas alternativas
futuras, *"Eixo de storage — ADR próprio, **com este pipeline já limpo**"*. O pipeline está limpo.

> Conversa com o [ADR-0003](../adr/0003-modelo-de-memoria-do-dado.md) (Firestore = verdade, Room =
> espelho/cacheada), o [ADR-0009](../adr/0009-sincronizacao-reativa-firestore-room.md) (pipeline único,
> portas neutras, telemetria `sync_*`), o [ADR-0004](../adr/0004-snapshot-e-observabilidade-emissao.md)
> (rascunho local não-autoritativo), o [ADR-0005](../adr/0005-autenticacao-sessao-firebase-datastore.md)
> (sessão no DataStore) e o [ADR-0016](../adr/0016-dominio-da-plataforma.md) (domínio dinâmico
> multi-empresa). Ancorado no código em `2026-07-31`.

**Premissa herdada (não em debate):** design **firestore-driven** — o Firestore é a fonte da verdade e o
motor do fluxo. Este estudo não questiona isso; questiona apenas **se ainda vale existir uma segunda
persistência local espelhando a primeira**.

**Direção da plataforma (decisão do analista, 2026-07-31) — responde a §10.1:** o app deixa de ser o
sistema de uma empresa e se torna uma **plataforma de gerenciamento de empresas**, no nível de negócio da
própria plataforma. Por isso o [ADR-0016](../adr/0016-dominio-da-plataforma.md) veio primeiro: **o domínio
é o que a plataforma organiza**, e é ele que sustenta os outros dois eixos. O sistema nasce **mobile-first**
— atender o cliente e o negócio dele já montando as bases da plataforma — e, nesse regime, **o Firestore
provê estrutura suficiente**, como **acesso rápido**. A alternativa a esta decisão não é o Room: é o
back-end próprio, e ele fica para depois.

Três eixos, portanto, e nesta ordem: **domínio → mobile-first → Firestore-only**.

> **O que fica combinado para o futuro (fora do escopo deste estudo):** informação **mais sensível e mais
> precisa** vai exigir um **sistema centralizador (back-end) próprio da plataforma**. Isso **não** reabre o
> espelho local — é um movimento em outra direção (para cima, na fronteira do app), enquanto este estudo
> decide o que existe **abaixo** dela. O ADR precisa dizer isso explicitamente, para que "Firestore-only"
> não seja lido como "Firestore para sempre" nem como veto ao back-end.

---

## 1. O que o Room é hoje — inventário

11 entidades, 11 DAOs, 1 banco (`FluviAppDatabase.kt`, v2, `exportSchema = true`).

| Entidade | Origem | Quem lê | Papel real |
|---|---|---|---|
| `Constante` | espelho da coleção | `ConstanteFirestoreRepository` | catálogo espelhado |
| `Empresa` | espelho | `EmpresaFirestoreRepository` | cadastro espelhado |
| `Navio` | espelho | `NavioFirestoreRepository` | cadastro espelhado |
| `Viagem` | espelho | `ViagemFirestoreRepository` | cadastro espelhado (**único caminho reativo real**) |
| `TarifaViagem` | espelho (achatado do mapa do doc) | `ViagemFirestoreRepository` | tabela-filha do ADR-0013 |
| `Funcionario` | espelho | `FuncionarioFirestoreRepository` | cadastro espelhado |
| `Usuario` | espelho **+ 1 flag local** | `UsuarioRepository` | espelho + `ultimoUsuarioLogado` |
| `ContadorBilhete` | espelho de 1 documento | `PassagemFirestoreRepository` | espelho puro |
| `Passagem` | espelho parcial (write-through + salvar-após-query) | `PassagemFirestoreRepository` | espelho **quase não lido** (§2.2) |
| `PassagemDigital` | **local-only** | `PassagemDigitalRepository` | caminho do arquivo gerado |
| `rascunho_passagem` | **local-only** | `RascunhoPassagemStoreRoom` | snapshot JSON do ADR-0004 |

**Ninguém fora dos repositórios toca um DAO.** O grep de `Dao` no `app/src/main` só acerta `database/**`,
`di/module/DatabaseModule.kt` e os 9 repositórios + o store de rascunho. Nenhum ViewModel, nenhum mapper,
nenhuma tela. A fronteira já está onde precisa estar para uma troca de eixo.

## 2. As três constatações que mudam o peso da decisão

### 2.1 Já existem dois caches locais em disco, não um

Não há **nenhuma** chamada a `setPersistenceEnabled` / `FirebaseFirestoreSettings` /
`setLocalCacheSettings` no projeto. Vale o default do SDK Android: **persistência ligada**. Ou seja, o app
já mantém um cache offline do Firestore (LevelDB) e mantém um SQLite por cima dele, com o mesmo conteúdo.

O Room, aqui, **não é o que dá offline ao app** — é uma segunda cópia do offline que já existia.

### 2.2 Não existe SQL de verdade para perder

Superfície somada dos 11 DAOs: `SELECT *` por `id`, `SELECT *` sem filtro, ~6 igualdades de campo único
(`categoria`, `agencia`, `viagemId`, `email`, `descricaoNome`, `nome`), um `SELECT agencia`, dois
`COUNT(*)` (um deles — `ViagemDao.obterContagem` — **sem nenhum chamador**), e três `DELETE` por chave.

**Zero JOIN. Zero GROUP BY. Zero range. Zero ORDER BY.** Tudo é `whereEqualTo` ou `document(id)`.

O custo que o [ADR-0003 §trade-off](../adr/0003-modelo-de-memoria-do-dado.md) previa — *"perdem-se as
consultas SQL que o app usa: `AgenteDao.obterTodosPorAgencia`, `ViagemDao.obterPorCodigo`, `ContadorDao`, e
a agregação do `BalancoPassagensMapper`"* — envelheceu inteiro:

- `obterPorCodigo` **morreu** no ADR-0008 (relacionar por id);
- a agregação do balanço **já é Kotlin**, não SQL (`BalancoPassagensMapper`, laço de contadores);
- `obterTodosPorAgencia` é um `whereEqualTo` — e a versão de negócio disso (recorte por agência da
  consulta de passagem, P2.6) **já é query de Firestore**;
- as duas consultas pesadas de passagem — contagem (`PassagemFirestoreRepository.kt:146`) e pesquisa
  (`:182`) — **já vão direto ao Firestore**. O Room só recebe um `dao.salvarTodas(passagens)` depois
  (`:206`) que **ninguém lê**: das leituras de `Passagem` no app, sobra `obterPorId` (detalhe, edição e
  `transicionar`).

### 2.3 O espelho já é reconhecidamente furado — e o código admite

- **Upsert-only:** documento apagado no servidor permanece na tabela local para sempre. O único lugar que
  resolve isso é `TarifaViagem`, com `delete+insert` manual — e o comentário em
  `ViagemFirestoreRepository.kt:128-138` explicita a assimetria: *"espelho honesto: uma célula removida no
  Firestore não fica órfã localmente (**diferente do upsert-só das entidades**)"*.
- **Cold start:** `FuncionarioFirestoreRepository.obterPorEmailDoServidor` **pula o Room de propósito** —
  *"no primeiro acesso o espelho ainda está vazio; sincronização só começa depois do login"*. O espelho
  tem um buraco estrutural na janela mais crítica do app (o login).
- **Divergência sem reconciliação na emissão:** `PassagemFirestoreRepository.salvar` (`:82-112`) grava o
  Room (sucesso durável, UI segue), depois dispara `documento.set(...)` *fire-and-forget*. Se o servidor
  **rejeitar** (regra de agência/cargo — ADR-0011), o bilhete existe só naquele aparelho, para sempre, e o
  desfecho é um `registroEmissao.pendenteDeSync` de nível warning. Não há caminho de volta.

## 3. Equivalências — o que substitui cada capacidade do Room

| Capacidade hoje | Substituto Firestore-only | Perda? |
|---|---|---|
| `dao.obterPorId(id).first()` | `document(id).get()` (cache-first) | não |
| `dao.obterTodas().first()` | `collection.get()` (cache-first) ou o replay do listener | não |
| `dao.obterTodosPorCategoria/PorAgencia/PorNome` | `whereEqualTo(...)`, resolvido no cache quando offline | não |
| `dao.obterTodas(): Flow` (SSOT reativo, D1) | `FonteSnapshots.observar(colecao)` já é um Flow reativo com `doCache` | não — **encurta um salto** |
| `salvarTodos` do `sincronizarColecao` | deixa de existir; o pipeline termina num `StateFlow` compartilhado | — |
| escrita durável antes do ack | fila offline do SDK (latency compensation + replay) | não — **melhora** (§2.3) |
| `SELECT agencia FROM Funcionario` + `.distinct()` | `map { it.agencia }.distinct()` (já é `.distinct()` em Kotlin hoje) | não |
| `COUNT(*)` | `.size` da coleção em memória (o único `COUNT` restante é código morto) | não |
| retenção previsível do que já foi visto | **não há substituto** — o cache do SDK é LRU gerido | **sim** (§5) |

O ponto arquitetural: a porta `FonteSnapshots` (ADR-0009 §10) já emite `DocumentoBruto` **neutro** com
`doCache`. Ela foi criada para testar o ciclo de vida sem Firebase — e é exatamente a peça que permite que
a fonte reativa deixe de ser o DAO **sem** vazar tipo Firebase para cima. O trabalho do `sincronizarColecao`
passa a ser `fonte.observar(colecao).map(paraModelo).shareIn(syncScope, replay = 1)`: mesma idempotência,
mesmo `parar()` no logout, mesma telemetria — só troca o destino, de tabela para `StateFlow`.

## 4. O resíduo local — o que **não** é espelho

Três coisas não têm contraparte no Firestore e não podem simplesmente sumir. **É aqui que mora o trabalho
real**, não na remoção:

1. **`rascunho_passagem`** — snapshot JSON do ADR-0004, deliberadamente local e não-autoritativo. A porta
   `RascunhoStore` já existe e `RascunhoPassagemStoreRoom` é *"o único ponto que conhece o mecanismo de
   persistência"*. Troca-se a impl por arquivo/DataStore e nada mais no app percebe.
   → **Decisão (2026-07-31): DataStore/cache.** É trocar `RascunhoPassagemStoreRoom` por
   `RascunhoPassagemStoreDataStore` atrás da porta que já existe; nenhum chamador muda.
2. **`PassagemDigital`** — caminho do arquivo do bilhete gerado no device; nunca sincroniza. O próprio
   ADR-0003 já marca com "atenção: não re-sincroniza".
   → **Decisão (2026-07-31): a tabela morre. O arquivo vai para a galeria (MediaStore) e a re-emissão
   recupera do próprio arquivo**, não de um índice local. Detalhado em §4.1.
3. **`Usuario.ultimoUsuarioLogado`** — flag de sessão, não é dado de negócio. Casa natural é o DataStore,
   que **já é** a casa da sessão desde o ADR-0005 (`PreferencesKey.USUARIO_ATUAL/PAPEL_ATUAL/CARGO_ATUAL`).
   → **Decisão (2026-07-31): DataStore**, junto com as chaves de sessão que já estão lá.

Com as três decisões, o Room fica **vazio de propósito** — e a pergunta "vale manter um SQLite para isso?"
se responde sozinha.

### 4.1 O que a decisão 2.2 implica no código

Hoje (`PassagemDigitalHelper.kt:40-68`) o bilhete é gravado em
`getExternalFilesDir(DIRECTORY_PICTURES)` — diretório **privado do app**, que não aparece na galeria e some
na desinstalação — com nome `passagem_<timestamp>.png` (`:47`). **É o nome por timestamp que obriga a
tabela a existir:** o arquivo não é localizável a partir do `idPassagem`, então a linha do Room é o único
mapa `idPassagem → caminho`.

Logo, "recuperar dos files" só fecha se o **nome do arquivo passar a ser derivável da passagem** (o
`idPassagem`, ou o número do bilhete). Feito isso, a busca vira uma consulta ao MediaStore por nome, e o
índice local não tem mais função. São duas mudanças que precisam andar juntas — trocar o destino sem
trocar o nome deixaria o arquivo na galeria e a re-emissão cega.

O que isso muda de regime:

- **A galeria é do usuário, não do app.** Ele pode apagar o bilhete. Então "recuperar dos files" é
  **best-effort por definição**: arquivo ausente **não é erro**, é regenerar. E regenerar é barato — o
  bitmap é renderizado a partir da `Passagem`, que está no Firestore. O bilhete é **derivado**, nunca foi
  dado de origem; o arquivo é cache de conveniência, exatamente como o cache do SDK (§2.1). Isso alinha o
  resíduo ao mesmo princípio do resto do estudo.
- **Escopo de acesso.** A partir do Android 10 o app enxerga sem permissão apenas as entradas de MediaStore
  que **ele mesmo** criou; após uma reinstalação a posse se perde e a imagem antiga fica visível na galeria
  mas **invisível para o app** sem `READ_MEDIA_IMAGES`. Consequência prática: reinstalou, re-emitiu, gera de
  novo — aceitável justamente por causa do ponto anterior, mas precisa estar escrito no ADR para não virar
  bug reportado depois.
- **Ganho de produto embutido:** hoje o bilhete só existe dentro do app e sai por `ACTION_SEND`; na galeria
  ele fica onde o passageiro (e o agente) naturalmente procura, e o compartilhamento continua igual.
- **Comportamento herdado, não regressão:** reaproveitar o arquivo antigo serve um PNG gerado no momento da
  emissão; se a passagem mudar depois, a imagem fica velha. Isso **já acontece** hoje (o Room devolve o
  mesmo caminho). Se o ADR quiser corrigir, o gancho é o ciclo do ADR-0012 — mas é fatia própria.

## 5. O que se paga

- **O cache passa a ser do SDK, não seu.** É LRU com teto (default ~100 MB) e o Firestore poda quando
  quer. O Room era previsível: o que entrou, ficou. Consequência concreta: um bilhete antigo pode não estar
  no cache e a busca offline volta menor — **sem aviso**. Se algum fluxo precisar de garantia de retenção,
  a resposta é armazenamento próprio para *aquele* fluxo, não o espelho inteiro de volta.
- **Sem SQL de escape para consulta nova.** Hoje o Room é um plano B que ninguém usa, mas existe. Depois,
  toda consulta é query de Firestore ou filtro em memória. Enquanto for **só igualdade e sem `orderBy`**, o
  Firestore resolve com merge de índices simples (é o caso de todas as consultas atuais, inclusive as
  quatro igualdades encadeadas de `obterTodasPorDataStatus`). O custo nasce quando entrar **range/ordenação**
  — relatórios do ADR-0016 — e aí índice composto é configuração de infra, não código.
- **Índices não são versionados.** `firebase.json` declara só `rules` e os emuladores; não há
  `firestore.indexes.json`. Já é dívida hoje; vira dívida crítica quando o SQL não estiver mais lá.
- **Leitura tem preço.** SQLite era grátis; documento lido do servidor, não. Mitigado pelo cache do SDK e
  pelos listeners (que cobram deltas) — e é o regime em que as consultas de passagem já operam.
- **Coleção espelhada vira memória.** Os cadastros (empresas, navios, viagens, funcionários, constantes,
  tarifas) passam a viver num `StateFlow` por coleção. São coleções pequenas; `passagens` **nunca** foi
  espelhada por listener, então nada muda lá — mas isso precisa ser dito no ADR como limite explícito.
- **Falha vira estado de UI obrigatório.** Sem gravação local "de sucesso", a rejeição permanente do
  servidor precisa aparecer na tela (o banner do D4 já existe; o caminho de *emissão rejeitada* não).

## 6. O que se ganha

- **Uma persistência local em vez de duas** (§2.1) — e a que fica é a que o SDK já mantinha de graça.
- **Fim do espelho que mente** (§2.3): remoção remota reflete, cold start não tem buraco, e a emissão
  passa a ter um único registro (a fila offline) em vez de dois que podem discordar para sempre.
- **Uma tradução a menos:** `Documento → modelo → SQLite → modelo → UI` vira `Documento → modelo → UI`.
  O `MainScreenViewModel.observarViagens()` (`:83`) não muda de forma nenhuma — continua coletando um Flow.
- **O domínio para de conhecer o banco:** `Passagem.kt:3-6` importa `androidx.room`, e `temPassageiro2` —
  regra de negócio — carrega um `@Ignore`. Anotação de persistência dentro do agregado é exatamente o que
  o ADR-0003 queria questionar ("a forma plana do Room é o artefato de tenacidade a ser questionado").
- **Some o custo recorrente de schema:** o DDL de 21 linhas do `DatabaseModule`, o
  `fallbackToDestructiveMigrationOnDowngrade`, o `room.schemaLocation` e a obrigação (ADR-0015 §9) de
  reexportar o `createSql` a cada mudança de entidade. Some também o `room-compiler` do KSP.
- **Alinha com o ADR-0016:** catálogo dinâmico, multi-empresa e multi-segmento vão brigar com schema
  tipado toda semana. Um domínio que ainda vai crescer em forma é o pior candidato a DDL congelado.

## 7. Esboço de caminho (aditivo, coleção por coleção)

Cada repositório é dono do próprio pipeline, então dá para migrar um de cada vez com o app compilando e a
suíte verde a cada passo — mesma tradição do ADR-0009 (D1→D5) e do ADR-0015.

1. **Resíduo local primeiro** (§4, decidido): `RascunhoStore` com impl DataStore, `ultimoUsuarioLogado`
   junto das chaves de sessão do ADR-0005, e o bilhete digital para a galeria com nome derivado do
   `idPassagem` (§4.1). Nada disso depende da decisão de eixo — é ganho isolado, e o passo do bilhete é o
   único com conteúdo próprio de verdade (destino, nome e permissão), então merece fatia sozinho.
2. **A coleção-piloto sem espelho — `Constante` virando `Catalogo`** (decidido): a mais boba do inventário
   (catálogo, duas consultas por igualdade), agora com menu e CRUD só para o ADM. Prova o padrão
   `observar → StateFlow` de ponta a ponta **e** entrega a F1 do ADR-0016. Detalhe em §7.1.
3. **Cadastros** — Empresa, Navio, Funcionario (e o `UsuarioRepository`, que ainda é pré-ADR-0009, §9).
4. **Viagem + TarifaViagem** — o caso reativo de verdade (Main Screen) e o achatamento do mapa de tarifas.
5. **Passagem + Contador** — o mais fácil, ironicamente: as consultas já são Firestore; é remover o
   `salvarTodas` órfão e trocar `obterPorId` por `document(id).get()`.
6. **Remover o Room** — entidades perdem anotação, `DatabaseModule` some, KSP do Room sai do build.

**Critério de pronto (herdado do ADR-0009 §10, e mais importante aqui):** sem tabela para inspecionar, a
telemetria `sync_*` passa a ser a **única** prova de que o offline funciona. `FakeFonteSnapshots` já existe;
os testes de ciclo de vida (`SincronizarColecaoTest`) sobrevivem à troca porque testam a porta, não o DAO.
Os 52 arquivos de teste JVM **não instanciam Room** — a suíte não sente a remoção.

### 7.1 O piloto: `Catalogo` (decisão de 2026-07-31)

O primeiro incremento junta duas coisas que estavam separadas nos dois documentos: o **piloto do eixo**
(coleção sem espelho) e a **F1 do ADR-0016** (`Constante` → `Catalogo`, categoria + descrição,
`IObjetoSimplificado` só nele). Junto entra a entrada de menu com **CRUD restrito ao ADM** — o primeiro
pedaço concreto do painel administrativo.

**Isto desbloqueia o ADR-0016.** O *Ponto aberto 1* de lá diz, textualmente, que renomear a entidade
*"toca `FluviAppDatabase`, o `DDL_V2` e o `schemas/…/2.json`"* e conclui: **"é o único bloqueio para
começar"**. Se a coleção-piloto deixa de ter espelho, **não existe entidade Room para renomear** — o
bloqueio não é resolvido, ele deixa de existir. O mesmo raciocínio vale para o *Ponto aberto 5* ("a Rota
precisa de espelho no Room?"): a resposta passa a ser não, por construção. Dois pontos abertos do ADR-0016
fecham como efeito colateral deste ADR.

**O que eu preciso dizer sobre o formato:** este incremento carrega **três mudanças ao mesmo tempo** —
troca de eixo, rename de domínio e tela nova com política de acesso. É mais do que um piloto normalmente
deveria carregar, e se algo quebrar, a causa é ambígua. Não é motivo para mudar a decisão: as três recaem
sobre a coleção mais simples do app, e separá-las custaria dois incrementos a mais para provar o óbvio. Mas
vale ordenar **dentro** da fatia — primeiro o eixo (a coleção passa a ler do `StateFlow`, com o nome
antigo), depois o rename, por último o menu e o CRUD. Assim, se o padrão `observar → StateFlow` tiver
defeito, ele aparece sozinho, antes de qualquer outra mudança em cima.

**Ponto que o ADR-0017 vai precisar resolver e o piloto expõe primeiro:** até aqui o estudo só tratou de
**leitura** (§3 é uma tabela de equivalências de consulta). O CRUD do ADM traz a **escrita** de cadastro
para dentro do piloto — e escrita sem Room é escrita direto no documento, com a fila offline do SDK
respondendo pelo sucesso. É o mesmo regime do §2.3, mas agora numa tela de cadastro, não na emissão.

### 7.2 Emissão rejeitada — fora de escopo (decisão de 2026-07-31)

A pergunta 5 nasceu do §2.3: hoje a emissão grava o Room, segue a UI e dispara `set(...)` sem esperar; se o
servidor recusar, o bilhete fica só no aparelho e o desfecho é um warning. A decisão do analista é que
**esse caso não terá realidade**: a rejeição prática é **passagem sem tarifa** (célula ausente é
*fail-closed* no ADR-0013), os pré-requisitos de emissão serão sanados antes da homologação, e **não há
passagem antiga** — o app não tem produção. Logo, o ADR-0017 **não** constrói desfecho de UI para emissão
rejeitada.

Duas observações que ficam registradas, sem reabrir a decisão:

- **O Firestore-only já resolve metade do problema sozinho.** Sem gravação local "de sucesso", não existem
  mais dois registros que possam discordar para sempre: ou a escrita entra na fila offline e replica, ou
  falha de forma observável. A divergência permanente do §2.3 morre **por construção**, não por tela nova.
  Era o pior item da lista e ele sai de graça.
- **Sobra um caminho estreito:** a recusa por regra de agência/cargo (ADR-0011) continua existindo no
  servidor mesmo com a tarifa resolvida. Só que ela não é caso de negócio — é uso indevido ou bug, e o
  lugar dela é a telemetria, não um fluxo de UI. Registrar isso no ADR evita que alguém leia "fora de
  escopo" como "não pode acontecer".

## 8. Riscos e pontos de não-retorno

- **Reversível?** Sim, e barato: o Room aqui é derivado: se voltar, re-sincroniza do Firestore. O único
  dado insubstituível é o resíduo local do §4 — e ele sai primeiro, atrás de porta, justamente por isso.
- **O que eu não conseguiria desfazer:** nada estrutural. O risco real não é técnico, é de **regime**:
  aceitar que a completude do offline passa a ser gerida pelo SDK, não pelo app.
- **Fora de escopo deste estudo:** mudar a forma do dado (blob JSON, DTO-cêntrico do ADR-0003 passo 2).
  Aqui o modelo Kotlin continua idêntico — só perde as anotações. São dois movimentos diferentes e não
  precisam acontecer juntos.

## 9. Achados colaterais (dívidas que aparecem no caminho)

- **`UsuarioRepository.carregarUsuarios()` (`:24-40`) ficou fora do saneamento do ADR-0009:** listener
  vazando (registration descartada), `runBlocking { dao.salvar }` por documento e um `throw` dentro do
  callback — os três smells que o ADR-0009 matou nos outros. É o candidato natural a cair primeiro.
- **`PassagemDigitalHelper` usa `runBlocking` (`:28`, `:36`)** em chamada de repositório na thread da UI.
- **`PassagemFirestoreRepository.adicionarContador` (`:114-120`)** ainda tem `runBlocking`.
- **`ViagemDao.obterContagem()`** não tem chamador — código morto.
- **`firebase.json` sem `indexes`** (§5).

## 10. Perguntas para o analista

1. ~~**A premissa vale?** Aceitar que o offline passa a ser o cache do SDK (LRU, gerido, sem garantia de
   retenção) — ou existe algum fluxo que precisa de "tudo que já vi continua aqui"?~~
   **RESPONDIDA (2026-07-31): sim, vale** — ver *Direção da plataforma* no topo. Nenhum fluxo exigiu
   retenção garantida; o eixo é domínio → mobile-first → Firestore-only, com back-end próprio depois para
   o dado sensível. O custo do §5 (cache LRU do SDK) fica **aceito**, não mitigado.
2. ~~**O resíduo local (§4) vai para onde?** DataStore para os três, arquivo para a passagem digital, ou
   manter um Room mínimo só para eles (opção que eu **não** recomendo, mas é legítima)?~~
   **RESPONDIDA (2026-07-31):** rascunho → **DataStore/cache**; bilhete digital → **galeria (MediaStore)**,
   com a re-emissão recuperando **do arquivo** e não de índice local (§4.1 — exige nome derivado do
   `idPassagem`); `ultimoUsuarioLogado` → **DataStore**. Room mínimo **descartado**.
3. **Ordem — reformulada (a versão anterior não estava clara).** O passo 1 (§4, resíduo local) e o passo 2
   (§7.1, `Constante` → `Catalogo` sem espelho) são **independentes**: nenhum precisa do outro para
   funcionar, e os dois já estão decididos *no conteúdo*. A pergunta é só **qual você quer ver primeiro**:
   - **Resíduo antes:** começa pelo que é local de verdade (rascunho, bilhete, flag de sessão). Não prova
     nada sobre o eixo — é limpeza — mas esvazia o Room de tudo que **não** é espelho, e aí a remoção final
     não fica esperando por essas três pontas soltas.
   - **Piloto antes:** começa provando o padrão `observar → StateFlow` de ponta a ponta numa coleção só, e
     de quebra destrava a F1 do ADR-0016. Se o padrão tiver algum defeito, você descobre no caso mais
     barato, antes de ter investido nas outras cinco coleções.

   Minha inclinação é **piloto antes**: o resíduo é trabalho garantido em qualquer cenário (mesmo se o eixo
   fosse abandonado), enquanto o piloto é o que pode **mudar o plano** — e informação que muda plano vale
   mais cedo.

   **RESPONDIDA (2026-07-31): piloto antes.** O plano do ADR-0017 fica F1 = `Catalogo`, F2 = resíduo local.
4. ~~**Escopo do primeiro incremento:** só `Constante`, ou já os cadastros inteiros?~~
   **RESPONDIDA (2026-07-31): só `Constante` — e ela já vai virar `Catalogo`**, com entrada de menu e CRUD
   **acessível só ao ADM**. É a F1 do ADR-0016 e o piloto do eixo no mesmo incremento (§7.1).
5. ~~**Emissão rejeitada:** hoje é warning silencioso; no Firestore-only precisa de desfecho de UI. Isso
   entra neste ADR ou vira fatia própria?~~
   **RESPONDIDA (2026-07-31): fora deste ADR.** A rejeição real é **passagem sem tarifa**, e os
   pré-requisitos de emissão serão todos sanados antes da homologação; **não há passagem antiga** para
   herdar o problema (§7.2).
6. ~~**Isto é um ADR novo (0017) ou uma revisão do ADR-0003?**~~
   **RESPONDIDA (2026-07-31): ADR novo — 0017.** Supera parcialmente o ADR-0003 (o nível "cacheada" deixa
   de ser o Room e passa a ser o cache do SDK) e fecha a alternativa registrada no ADR-0009.