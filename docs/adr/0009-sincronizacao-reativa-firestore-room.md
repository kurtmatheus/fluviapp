# ADR-0009: Sincronização reativa Firestore→Room — pipeline único, listener de sessão, observável e testável

**Status:** Aceita — implementada e verde. Execução faseada e aditiva (D1→D5 + observabilidade +
testabilidade N1/N2), cada fase compilando e com suíte passando. Commits: D1 `56eea5d`, D2+D3
`c2f5b7d`, observabilidade `ba9a817`, D4 `a7838ef`, D5 `d7b14b2`, testabilidade Nível 2 `52bfae9`.

> Formaliza o [estudo de sincronização](../design/sincronizacao-firestore-room.md) (mapeamento,
> opções e as decisões D1–D5 + §10). Conversa com o [ADR-0003](0003-modelo-de-memoria-do-dado.md)
> (Firestore = verdade, Room = espelho vivo), reusa a observabilidade dos
> [ADR-0004](0004-snapshot-e-observabilidade-emissao.md)/[ADR-0007](0007-observabilidade-cadastros.md)
> (porta `Telemetry`) e apoia-se nos mappers `suspend` do [ADR-0008](0008-relacionamentos-por-identidade.md).
> Ancorado no código em `2026-07`.

**Premissa (não em debate):** design **firestore-driven** — Firestore é a fonte de verdade e o motor
do fluxo (tempo real); Room é espelho. Herança consolidada (a origem carregava dados de planilha
Google, ineficiente; o Firebase resolveu). Nenhuma alternativa *pull* está em jogo.

**Contexto**

A sincronização da Main Screen tinha **dois mecanismos desconectados**: uma **leitura one-shot** do
Room (`obterTodas().first()` após um `delay(1000)`) e um **listener fire-and-forget**
(`addSnapshotListener` cujo `ListenerRegistration` era descartado, gravando por doc com `runBlocking`).
O listener escrevia no Room; a UI lia o Room **uma vez** — quando o listener gravava dados novos, a UI
**não reagia** (só no pull-to-refresh). Contradizia o próprio ADR-0003 (espelho *vivo*). Smells
concretos: leitura stale, `delay(1000)` mágico, **vazamento de listener** (re-anexo a cada refresh e
duplo-attach Login+Main), `runBlocking` na thread do listener, **falhas silenciosas** (só log), e um
`throw` dentro do callback do contador (derrubava o app).

**Opções consideradas**

1. **Status quo** — manter os dois mecanismos; simples, mas stale, vazando e cego a falhas.
2. **Pipeline reativo único dentro do modelo Room-espelho** (esta) — SSOT reativo, listener de sessão
   gerenciado, escrita em lote, observabilidade + testabilidade como critério de pronto.
3. **Trocar o eixo de storage** (Firestore-only / estratégia offline) — mudança estrutural maior;
   ortogonal e adiada para um ADR futuro.

**Decisão**

Opção 2. Uma direção de verdade: **Firestore →(1 listener gerenciado)→ Room →(Flow)→ UI reage
sozinha**, com o pull-to-refresh preservado como ação explícita. Decisões concretas:

- **D1 — SSOT reativo:** a UI observa o Room (`ViagemRepository.observarTodas(): Flow` → `StateFlow`
  no `MainScreenViewModel`), mapeando com o mapper `suspend` (ADR-0008). Elimina o `delay(1000)` e a
  leitura one-shot.
- **D2 — ciclo de vida do listener = sessão:** escopo `@SyncScope CoroutineScope(SupervisorJob()+IO)`;
  `sincronizarColecao` vira `callbackFlow` + `awaitClose` (remove a registration no cancelamento — fim
  do vazamento) e devolve `Job`; `sincronizar()` é **idempotente**; `SincronizacaoSessao.parar()`
  cancela no logout. (Alternativa `WhileSubscribed` registrada; VM-scoped rejeitada por vazar tipo
  Firebase na porta.)
- **D3 — escrita em lote sem `runBlocking`:** `salvarTodos` por snapshot no escopo de sessão.
- **D4 — falha visível offline-first:** `EstadoSincronizacao` (StateFlow) alimentado pela
  observabilidade; banner **não-bloqueante** na Main Screen **sobre** os dados do cache (campo no
  UiState, não um estado que troca a tela).
- **D5 — pull-to-refresh honesto:** `atualizarDoServidor()` = `get(Source.SERVER)` one-shot, grava no
  Room (o Flow reflete). Coexiste com o listener.
- **§10 — critério de pronto (observável + testável), pré-requisito desta aceitação:**
  - *Observabilidade:* `RegistroSincronizacao` (puro, sobre a porta `Telemetry`) emite o ciclo —
    `sync_iniciado` (único por coleção ⇒ idempotência **observável**), `sync_snapshot {origem=cache|servidor}`
    (de `SnapshotMetadata.isFromCache`), `sync_gravado`, `sync_parado`, `sync_erro`.
  - *Testabilidade:* porta **`FonteSnapshots`** emitindo `DocumentoBruto` **neutro** (sem tipos
    Firebase) — o ciclo de vida é testado sem Firebase (`FakeFonteSnapshots`); a desserialização
    `Map→Documento` virou funções puras testadas. `compile+suíte verde` prova que constrói; a §10 prova
    que se comporta.

**Consequências**

- **Reativo e sem gambiarra:** a Main Screen atualiza sozinha; sem `delay`, sem refresh obrigatório.
- **Sem vazamento / sem duplo-attach:** um único listener por coleção na sessão, encerrado no logout.
- **Falha deixa de ser silenciosa:** vira evento de telemetria + banner offline-first (cache segue útil).
- **Firebase fora das portas** (`FonteSnapshots` neutra) — fecha a raiz da dívida de DIP do sync e torna
  o comportamento verificável por telemetria, não por inspeção manual no device.
- **Custo:** trocou-se a desserialização tipada do Firestore (`toObject`) por mapeamento manual
  `Map→Documento` — mitigado por serem funções puras **testadas**. O escopo de sessão é uma peça de
  ciclo de vida nova a manter (parar no logout).

**Alternativas futuras**

- **Eixo de storage** (Firestore-only / offline avançado) — ADR próprio, com este pipeline já limpo.
- **`WhileSubscribed`** para o listener (economia de bateria quando nenhuma tela observa), se a
  premissa "Room sempre fresco na sessão" for revista.
- **Validação em runtime no device** — agora rastreável pelos eventos `sync_*`; falta um smoke-test
  explícito de campo.
