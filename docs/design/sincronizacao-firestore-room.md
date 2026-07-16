# Estudo de design — Sincronização Firestore↔Room (Main Screen)

> **Formalizado pelo [ADR-0009](../adr/0009-sincronizacao-reativa-firestore-room.md)** (sincronização
> reativa). Este é o documento-base companheiro (mapeamento, opções e as decisões D1–D5 + §10).
> Conversa com o [ADR-0003](../adr/0003-modelo-de-memoria-do-dado.md)
> (Firestore = verdade, Room = espelho), o [ADR-0008](../adr/0008-relacionamentos-por-identidade.md)
> (mappers agora `suspend`) e o [fluxo-main-screen.md](fluxo-main-screen.md) (§8.3 — normalizar os
> repositórios espelhados). Ancorado no código concreto em `2026-07`.

## 1. Premissa: design firestore-driven

Decisão consolidada do projeto, não preferência a debater: **Firestore é a fonte de verdade e o
motor do fluxo** (listeners/queries em tempo real); Room é espelho reativo. Origem histórica: o
`LoginViewModel` carregava constantes e dados de uma **planilha do Google (Sheets)** — lento,
acoplado, gargalo no login. A migração para o Firestore resolveu (rápido, tempo real, schemaless).
Logo, **nenhuma alternativa "pull" (planilha/REST/polling) está em jogo** neste estudo; o eixo é
como fazer o pipeline firestore→Room→UI corretamente.

## 2. Estado atual (concreto)

```
MainScreenViewModel.init  (MainScreenViewModel.kt:40)
├─ atualizarListaViagem(isRefreshing=false)          ← LEITURA one-shot
│    delay(1000)                                        (MainScreenViewModel.kt:64)
│    viagemRepository.obterTodas()                      (MainScreenViewModel.kt:66)
│      → dao.obterTodas().first()                       (ViagemFirestoreRepository.kt:67) — pega 1 emissão e PARA
│    → mapeia p/ cards (viagemMapper.map, suspend) → state = HOME
└─ sincronizarFirestore()                            ← ESCRITA (listeners)
     viagemRepository.sincronizar()                     (ViagemFirestoreRepository.kt:28)
       → firestore.sincronizarColecao(...)              (SincronizacaoFirestore.kt:13)
           addSnapshotListener { … runBlocking { dao.salvar(it) } }   (SincronizacaoFirestore.kt:20-23)
     passagemRepository.sincronizarNumeroBilheteEmTempoReal()
     agenteRepository.sincronizar()

refresh()  (MainScreenViewModel.kt:96) → toggle isRefreshing + atualizarListaViagem(true) + sincronizarFirestore()  (de novo)
```

Fatos que ancoram o estudo:
- `ViagemDao.obterTodas(): Flow<List<Viagem>>` (`ViagemDao.kt:18`) — **o caminho reativo existe**,
  mas o repo o colapsa com `.first()`.
- `ViagemDao.salvarTodas(vararg)` (`ViagemDao.kt:27`) — já há escrita em lote disponível.
- A porta `ViagemRepository` **não tem tipos Firebase** (por isso `FakeViagemRepository` a implementa).
- `MainScreenState` só tem `LOADING | HOME` (`MainScreenState.kt`) — **sem estado de erro**.
- `sincronizarNumeroBilheteEmTempoReal()` é anexado em **dois** lugares: `LoginViewModel.kt:214` e
  `MainScreenViewModel.kt:104`.

## 3. O achado central: dois mecanismos desconectados

A Main Screen tem uma **leitura one-shot** e um **listener fire-and-forget** que não se conversam. O
listener escreve no Room; a UI lê o Room **uma vez** (`.first()`). Quando o listener grava dados
novos, a UI **não reage** — só no pull-to-refresh. O `delay(1000)` e o refresh-obrigatório são
gambiarras para mascarar essa desconexão. Isso contradiz o próprio ADR-0003 (Room = espelho **vivo**):
o espelho é vivo na escrita, mas morto na leitura.

## 4. Smells (concretos)

1. **Leitura one-shot desacoplada** (`MainScreenViewModel.kt:66` + `.first()`): UI é snapshot, não reativa.
2. **`delay(1000)` mágico** (`MainScreenViewModel.kt:64`): tenta "esperar o sync"; frágil (>1s = stale, <1s = desperdício).
3. **Vazamento de listener** (`SincronizacaoFirestore.kt:20`): o `ListenerRegistration` é descartado.
   Cada `sincronizar()` (init **e** todo refresh) anexa um listener novo que nunca é removido → acumulam.
4. **`runBlocking` no callback** (`SincronizacaoFirestore.kt:22`): bloqueia a thread do listener; N docs = N escritas sequenciais.
5. **Falha silenciosa** (`SincronizacaoFirestore.kt:18` `onErro` no-op + `MainScreenState` sem `ERROR`): sync que falha não chega na UI.
6. **Sem ciclo de vida** e **duplo-attach**: listeners nascem no `init`, nunca morrem; `sincronizarNumeroBilheteEmTempoReal` anexado em Login **e** Main.

## 5. Princípio-alvo: um pipeline reativo (SSOT)

Uma direção de verdade, reafirmando o ADR-0003:

```
Firestore ──(1 listener gerenciado)──► Room ──(Flow)──► UI reage sozinha
                                        ▲
                      pull-to-refresh ──┘  (ação explícita "buscar agora", não o caminho normal)
```

- A UI **observa** o Room (`Flow → StateFlow`); nunca lê imperativo.
- **Um** listener gerenciado mantém o Room fresco.
- O pull-to-refresh **permanece** (restrição de UX: controle/confiança do usuário), com semântica
  honesta de re-fetch — não como o mecanismo de atualização.

Isso mata, de uma vez: leitura stale, `delay(1000)`, refresh-obrigatório e o vazamento.

## 6. Decisões

### D1 — A UI observa o Room (o coração). **Proposto.**
Repo expõe `observarTodas(): Flow<List<Viagem>>` (devolve o Flow do DAO em vez de `.first()`); o VM
faz `observarTodas().map { it.map(viagemMapper::map) }.stateIn(...)`. Atualiza sozinho quando o Room
muda. **Amarra boa:** `viagemMapper.map` já é `suspend` (ADR-0008) — dentro de `Flow.map` é natural;
a dívida virou encaixe. Remove o `delay(1000)` e a leitura one-shot.

### D2 — Ciclo de vida do listener. **Decidido + FEITO (Fase 2): sessão + (b) callbackFlow.**
Separar em dois eixos:

**Eixo 1 — tempo de vida:**

| | Sessão (login→logout) | Enquanto observado (`WhileSubscribed`) | Por tela (VM) |
|---|---|---|---|
| Room fresco p/ outras telas (detalhe, busca) | ✅ sempre | ⚠️ só se observarem | ❌ só na main |
| Custo ocioso (rede/bateria) | listener aberto a sessão | para ~5s após sair | para ao sair da main |
| Alinha ADR-0003 (espelho **vivo**) | ✅ | parcial | ❌ |
| Churn (re-anexar navegando) | nenhum | baixo | alto |

→ **Tempo de vida = sessão** (viagens é master data pequena, lida em várias telas; ADR-0003 assume
Room sempre fresco). `WhileSubscribed` fica como **alternativa** ("se frescor só importar com a main
aberta"; ganho de bateria, mas muda a premissa do ADR-0003).

**Eixo 2 — dono e mecanismo** (assumindo sessão; a porta `ViagemRepository` **não pode** vazar tipos Firebase):

- **(a) `ListenerRegistration` cru + guarda de idempotência** dentro do repo `@Singleton`.
  `sincronizar()` vira idempotente (`if (registration != null) return`) + `pararSincronizacao()`.
  Simples; Firestore **reconecta sozinho** (registration persiste). Contra: limpeza **manual** (lembrar do logout).
- **(b) `callbackFlow` + `Job` num escopo de sessão** (recomendado). `callbackFlow { val reg =
  addSnapshotListener{…}; awaitClose { reg.remove() } }`, coletado por um app-scope injetado que
  escreve `salvarTodas`. Limpeza **estrutural** (cancelar o escopo → `awaitClose` remove); idiomático;
  **mata o `runBlocking` do D3 de brinde**. Pegadinha: no erro **só logar, nunca `close`** (senão o
  listener morre e perde a reconexão); opcional `.retry {}` para falhas duras.
- **(c) VM-scoped** (rejeitado). Exigiria o repo **retornar o `ListenerRegistration`** (tipo Firebase)
  para o VM guardar/`remove()` no `onCleared` — **vaza Firestore na porta e quebra o fake/DIP**.
  Registrar como não-caminho.

→ **Decisão: sessão + (b) callbackFlow** (ratificado), com (a) como fallback e (c) rejeitado. Escopo de
sessão = `@Singleton @Provides CoroutineScope(SupervisorJob() + Dispatchers.IO)` (padrão Hilt).
Parada natural no `deslogar()` (`MainScreenViewModel`) + logout (`LoginViewModel`). Attach único no
login corrige o duplo-attach (smell #6): `refresh()` nunca re-anexa.

### D3 — Matar o `runBlocking`. **FEITO (Fase 2, junto do D2).**
Trocar `runBlocking { salvar }` por escrita no escopo coletor + **`salvarTodos(snapshot)` em lote**
(1 transação por snapshot, não N bloqueantes). Cai naturalmente do D2(b). **Acoplamento medido na
implementação:** o `sincronizarColecao` é compartilhado por **5 repos** (Viagem/Navio/Empresa/
Constante/Agente), só **2 DAOs** têm salvar-em-lote (faltam Empresa/Agente/Constante) e **não há
escopo de app** — a forma limpa do D3 depende do mesmo escopo de sessão do D2. Por isso D3 **não** é
Fase 1; anda com o D2.

### D4 — Falha visível. **FEITO: banner offline-first.**
**Consome a observabilidade da §10**: `RegistroSincronizacao` alimenta um `@Singleton EstadoSincronizacao`
(`StateFlow<Boolean> comErro`: erro → true; snapshot do **servidor** → false; cache não limpa). O
`MainScreenViewModel` observa e expõe `sincronizacaoComErro` no UiState; a `MainScreen` mostra um
banner não-bloqueante (`errorContainer`) **sobre** os cards do cache — não troca a tela. Campo no
UiState, **não** `MainScreenState.ERROR` (offline-first: o cache continua útil). String
`msg_sincronizacao_offline`. Sem re-threading (o `registro` já circulava). Teste puro cobre erro→liga /
servidor→desliga / cache→mantém.

### D5 — Semântica do pull-to-refresh. **FEITO: (a) forçar busca no servidor.**
`ViagemRepository.atualizarDoServidor()` faz `get(Source.SERVER)` one-shot, grava em lote no Room (o
Flow reativo reflete), e reporta ao `RegistroSincronizacao` (servidor → limpa o banner; falha offline
→ liga). `MainScreenViewModel.refresh()` chama num `launch`, mantendo `isRefreshing` até concluir.
Coexiste com o listener de sessão; atende o "necessário a nível de UX" (ação real, não cosmética).

## 7. Fora de escopo (adiado)

- **Eixo de storage** (Firestore-only / estratégia offline): ortogonal, ADR futuro (ver
  [ADR-0008](../adr/0008-relacionamentos-por-identidade.md) "alternativas futuras"). Este estudo é a
  "Opção 1": consertar dentro do modelo Room-espelho.
- **Syncs caronistas** (`sincronizarNumeroBilheteEmTempoReal`, agente): decidir se sobem para o
  escopo de sessão (junto do de viagem) ou permanecem separados. Relaciona com o §8.3 do
  [fluxo-main-screen.md](fluxo-main-screen.md) (contrato comum dos repositórios espelhados).

## 8. Impacto no código

**Adicionar**
- `ViagemRepository.observarTodas(): Flow<List<Viagem>>` (porta) + impl devolvendo `dao.obterTodas()`
  (sem `.first()`); `FakeViagemRepository` devolve `flowOf(viagens)`.
- `pararSincronizacao()` na porta (fake = no-op).
- Escopo de sessão injetável (Hilt `@Singleton CoroutineScope`).
- (D2b) `callbackFlow` interno no `ViagemFirestoreRepository` + `Job` + `awaitClose { remove() }`.
- `MainScreenState.ERROR` (ou campo de erro no UiState) + banner na `MainScreen`.

**Alterar**
- `SincronizacaoFirestore.sincronizarColecao`: virar `callbackFlow`/gerenciado; `salvarTodas` em lote;
  não `close` no erro.
- `MainScreenViewModel`: `atualizarListaViagem` → coleta reativa (`stateIn`); remover `delay(1000)`;
  `refresh()` → `get(Source.SERVER)`; parar sync no `deslogar()`.
- `sincronizar()` idempotente; attach único no login (remove duplo-attach).

**Remover**
- `delay(1000)`; a leitura `.first()` do caminho de UI.

## 9. Faseamento (revisado na implementação)

Recorte ajustado ao medir o raio do D3 (ver D3): a forma limpa do D3 depende do escopo de sessão do
D2, então D3 desce para a Fase 2.

1. **Reativo (D1)** — ✅ **FEITO**: `ViagemRepository.observarTodas(): Flow` + coleta reativa no
   `MainScreenViewModel` (`observarTodas().map { it.map(viagemMapper::map) }.collect`); removidos
   `delay(1000)` e a leitura one-shot; `refresh()` força re-sync e o Flow encerra o spinner. Entrega o
   auto-update. O `runBlocking` do listener permanece (sem regressão — D1 independe de como o Room é escrito).
2. **Ciclo de vida + escrita (D2 + D3)** — ✅ **FEITO**: escopo de sessão `@SyncScope` (`@Singleton
   CoroutineScope(SupervisorJob()+IO)`); `sincronizarColecao` virou `callbackFlow` gerenciado que
   grava em lote (`salvarTodos`, +3 métodos de DAO) e devolve `Job`; cada repo guarda o `Job` e é
   idempotente (`if (syncJob?.isActive == true) return`); `SincronizacaoSessao.parar()` cancela os
   filhos do escopo no logout (`awaitClose` remove as registrations). O contador de bilhete
   (`sincronizarNumeroBilheteEmTempoReal`) recebeu o mesmo tratamento — some o `throw` dentro do
   callback (derrubava o app). Corrige vazamento (#3), `runBlocking`/thread (#4) e duplo-attach (#6).
3. **Observabilidade + Testabilidade (§10)** — 🔜 **EM ANDAMENTO**: `RegistroSincronizacao` (puro,
   sobre a porta `Telemetry`) instrumenta o ciclo de vida; depois o seam `FonteSnapshots` destrava o
   teste do ciclo sem Firebase. **Vem antes/junto do D4** (o banner do D4 consome o estado/erro que a
   observabilidade produz — ver D4).
4. **UX** ✅ **FEITO**: **D4** — `EstadoSincronizacao` (StateFlow) alimentado pelo
   `RegistroSincronizacao`, banner não-bloqueante na `MainScreen` sobre o cache; **D5** —
   `atualizarDoServidor()` (`get(Source.SERVER)`) no pull-to-refresh, grava no Room e o Flow reflete.

Cada fase é aditiva. Critério "observável + testável" (§10) **atingido** — observabilidade
(`RegistroSincronizacao`) e testabilidade Níveis 1 e 2 (`FonteSnapshots` + testes do ciclo de vida
sem Firebase). **Promovido ao [ADR-0009](../adr/0009-sincronizacao-reativa-firestore-room.md).** Falta
só a validação em runtime no device (que a observabilidade agora torna verificável por telemetria,
não por inspeção manual).

## 10. Observabilidade & Testabilidade (critério de pronto)

"Como sabemos que a sync funciona?" não se responde no device (anedótico, cego para cache×servidor e
para attach duplicado). Responde-se com **observabilidade** (ver os eventos) + **testabilidade**
(asserir a lógica sem Firebase). O app já tem o molde: porta `Telemetry` (`evento`/`rastro`/`naoFatal`,
ADR-0004/0007) com `FakeTelemetry`, e camadas puras testadas (`RegistroCadastro`).

**Observabilidade — `RegistroSincronizacao` (puro, só depende de `Telemetry`).** Eventos do ciclo:
- `sync_iniciado {colecao}` — anexou. Como o `iniciado` só dispara quando `sincronizarColecao` é
  chamado (i.e., passou a guarda de idempotência), **um único `iniciado` por coleção prova que não há
  duplo-attach** (a idempotência é observável, não presumida).
- `sync_snapshot {colecao, docs, origem=cache|servidor}` — de `SnapshotMetadata.isFromCache`: responde
  *"o espelho veio do servidor ou do cache?"*, o cerne do "está fresco?" (hoje descartado).
- `sync_gravado {colecao, n}` (batch no Room) · `sync_erro {colecao, motivo}` (`naoFatal`, reconecta) ·
  `sync_parado {colecao}` (no `awaitClose` — prova o logout parando).

**Testabilidade — dois níveis, ambos ✅ FEITOS:**
- *Nível 1:* `RegistroSincronizacao` é **puro** → unit-testável com `FakeTelemetry` (como
  `RegistroCadastroTest`). Trava a taxonomia dos eventos.
- *Nível 2 (seam):* porta **`FonteSnapshots`** (`observar(colecao)` / `observarDocumento`) emitindo
  **`DocumentoBruto` neutro** (`id` + `Map`, sem tipos Firebase — decisão do usuário: porta pura). Impl
  `FonteSnapshotsFirestore` (`addSnapshotListener`+`callbackFlow`+`awaitClose`); fake
  `FakeFonteSnapshots` emite snapshots controlados. O `sincronizarColecao` depende da porta, não do
  `FirebaseFirestore` → `SincronizarColecaoTest` cobre o **ciclo de vida sem Firebase**: lote (1
  snapshot → 1 `salvarTodos`), erro (registra sem encerrar), parada (cancelar → `sync_parado`). A
  desserialização `Map→Documento` (que substituiu o `toObject`) virou funções puras testadas
  (`DocumentoBrutoMappersTest`). Trade-off aceito: perdeu-se a desserialização tipada do Firestore, em
  troca de mapeamento testável + porta livre de Firebase (fecha também a raiz da dívida de DIP).
