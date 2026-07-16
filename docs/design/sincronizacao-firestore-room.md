# Estudo de design — Sincronização Firestore↔Room (Main Screen)

> Estudo pré-ADR do pipeline de sincronização que alimenta a Main Screen (lista de viagens) e o
> espelho local em geral. Conversa com o [ADR-0003](../adr/0003-modelo-de-memoria-do-dado.md)
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

### D2 — Ciclo de vida do listener. **Decidido: sessão + (b) callbackFlow.**
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

### D3 — Matar o `runBlocking`. **Proposto.**
Trocar `runBlocking { salvar }` por escrita no escopo coletor + **`salvarTodas(snapshot)` em lote**
(1 transação por snapshot, não N bloqueantes). Cai naturalmente do D2(b).

### D4 — Falha visível. **Decidido: banner offline-first.**
Adicionar erro ao estado (campo de erro ou `MainScreenState.ERROR`) e ligar o `onErro` (hoje no-op).
→ **Decisão (ratificado): offline-first** — manter os cards do cache + **banner não-bloqueante** ("sem
conexão, mostrando dados salvos"), não tela de erro cheia. Combina com Room-espelho.

### D5 — Semântica do pull-to-refresh. **Decidido: (a) forçar busca no servidor.**
Com a lista reativa, o gesto precisa de significado honesto:
- **(a) Forçar busca no servidor** (ratificado): `get(Source.SERVER)` one-shot, grava no Room (o
  Flow reflete), spinner até concluir. Ação real, coexiste com o listener, atende o "necessário a
  nível de UX".
- **(b) Cosmético**: spinner curto e pronto (só devolve controle) — descartado.

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

## 9. Faseamento sugerido

1. **Reativo (D1 + D3)**: `observarTodas` Flow→UI, listener grava em lote sem `runBlocking`. Já
   entrega auto-update e mata `delay(1000)`. Menor risco, maior ganho.
2. **Ciclo de vida (D2)**: escopo de sessão + `callbackFlow`/idempotência + parada no logout. Corrige
   vazamento e duplo-attach.
3. **UX (D4 + D5)**: estado/banner de erro; refresh = busca no servidor.

Cada fase é aditiva e testável (o mapper reativo e a idempotência do listener são unit-testáveis com
os fakes existentes). Após validado, promover a um ADR de sincronização.
