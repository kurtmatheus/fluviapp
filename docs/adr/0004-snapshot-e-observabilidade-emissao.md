# ADR-0004: Snapshot de rascunho + observabilidade/SRE na emissão de Passagem

**Status:** Aceita (direção); avaliação do flip completo — execução faseada

**Contexto**

A emissão de passagem é o fluxo mais rico do app e o laboratório natural do modelo de memória
([[ADR-0003]]). O mapeamento do fluxo real revelou que ele são **duas fases**:

- **SAVE (`btn_avancar`)** — `FormPassagemViewModel.salvarPassagem` → `FormPassagemHelper.montarPassagem`
  monta a `Passagem` com status `A_EMITIR`, **aloca número de bilhete** (`montarPassagem:413`,
  `contador.inc()`) e `PassagemFirestoreRepository.salvar` grava **Room + Firestore**.
- **EMIT (`btn_emitir`)** — `ImpressaoHelper.imprimir` imprime (QR encoda o `idPassagem` + térmica)
  e `atualizaSituacao` vira `A_EMITIR → EMITIDA` (`PassagemFirestoreRepository:139`).

Três problemas concretos que este ADR ataca:

1. **Não existe rascunho.** Crash no meio do formulário perde tudo (estado vive em 3
   `MutableStateFlow` voláteis; `grep rascunho|snapshot|draft` = só API do Firestore). E o
   registro só é "durável" quando já é sólida (`A_EMITIR`), o que já queimou número e já bateu
   no Firestore — o oposto da tese "verdade só na promoção".
2. **Transmissão cega.** `documento.set(...)` não é aguardado (`:62`) — a falha de rede do
   Firestore não é capturada pelo `catch` (que só pega erro síncrono de Room/mapper). Sucesso e
   falha de transmissão hoje são **invisíveis**.
3. **Sem observabilidade.** Só `Log.e` (16 pontos; um mis-tagged: `TAG="viagemFirestoreRepository"`
   no repo de passagem). Sem Crashlytics/Analytics/Timber.

**Modelo de memória aplicado à Passagem**

| Nível | Hoje | Alvo |
|---|---|---|
| **Volátil** | 3 states do form (`FormPassagemUiState`+passageiro+veículo) | igual |
| **Cacheada** | — (inexistente) | **snapshot de rascunho JSON** (crash-safe, não-autoritativo, não queima número) |
| **Sólida** | Firestore (Room=espelho); `A_EMITIR`/`EMITIDA` = ciclo de vida | igual; promoção explícita |

**Transições (mappers puros, batizados pela direção):** volátil→cacheada `montarRascunhoSnapshot()`;
cacheada→volátil `aplicarEm(states)`; cacheada/volátil→sólida = o gate de SAVE (`montarPassagem`+
`salvar`), que **descarta** o snapshot (invariante `snapshot existe ⇔ é rascunho`, importada do AS).

**Decisão — flip em duas profundidades**

- **Flip raso (recomendado 1º):** o form auto-grava um **snapshot de rascunho** (JSON) enquanto é
  preenchido → crash-safety/offline sem mexer na semântica de commit. SAVE continua promovendo pra
  sólida e passa a **descartar** o snapshot. Número e Firestore seguem alocados no SAVE.
  Comportamento de negócio inalterado; ganha-se só o rascunho.
- **Flip profundo (ambicioso, follow-up):** adiar número+Firestore para a promoção real, fazendo
  `A_EMITIR` deixar de ser sólida — a "verdade só na finalização" do AS. Maior risco (QR encoda
  `idPassagem`, número precisa existir no print), avaliado à parte.

**Observabilidade/SRE (padrão importado do AS — [[project_observabilidade_finalizacao_transmissao]])**

Porta + impl, tudo por interface (DIP; testável e swappable):

- `telemetry/EmissaoTelemetry` (interface): `chave(k,v)`, `rastro(msg)` (breadcrumb),
  `evento(nome, params)`, `naoFatal(e)`, `suspend fun <T> trace(nome, bloco)`.
- `FirebaseEmissaoTelemetry` (único ponto que toca Firebase) — **espelho local↔remoto**: `Log.*`
  (tag única) ALÉM do Firebase, para sucesso/trilha aparecerem no logcat E remoto.
- Exceptions: sealed `EmissaoException` (`FalhaAoPersistir`, `FalhaNaTransmissao`,
  `FalhaNaImpressao`, `NumeroIndisponivel`) — agrupa por motivo, dimensão navegável.

**Taxonomia sucesso / falha / warning** (o que você pediu):

| Sinal | Quando | Como |
|---|---|---|
| **Sucesso** | Room+Firestore ok; impressão ok; `EMITIDA` ok | `evento(passagem_salva)` / `passagem_emitida` (Analytics) + `rastro` |
| **Warning** | **Room ok mas Firestore set falhou** (offline-first: local tem, vai reconciliar) | `evento(passagem_pendente_sync{motivo})` + `naoFatal` non-fatal — degradado, NÃO erro |
| **Falha** | Room falhou / mapper falhou / impressão falhou / número indisponível | `naoFatal(EmissaoException.*)` + `evento(..._falha{motivo})` + key `fase` |

O achado #2 (set não-aguardado) vira o exemplo canônico: **aguardar o `Task` do Firestore**
converte o silêncio em Warning observável (offline) ou Sucesso confirmado. É a diferença entre
"achei que salvou" e "sei que está pendente de sync".

**Estratégia de teste (testável)**

- `EmissaoTelemetry` → **fake** que grava chamadas; `RascunhoStore` → fake in-memory.
- Testes de VM: (1) SAVE ok emite `passagem_salva` + descarta snapshot; (2) Firestore falha →
  `passagem_pendente_sync` + Room mantém o registro (não é falha); (3) restore repopula os 3
  states do snapshot após "crash"; (4) número indisponível → `NumeroIndisponivel` + não persiste.
- Sem device: o fake do `trace` executa o bloco (igual ao AS); asserção sobre chaves/eventos.

**Consequências**

- Ganha-se crash-safety/offline-first no fluxo de dinheiro + visibilidade SRE (erros/eventos/
  latência) sobre um fluxo hoje cego.
- A validação estrutural sai do "build tipado da entidade" (que hoje pressupõe validação) para a
  fronteira de promoção — o rascunho não é validado (lição do AS: `NumberFormatException` em
  `toInt("")`).
- Custo: um port + impl + deps de telemetria; disciplina de emitir nos pontos certos.

**Dependências / ordem**

- SRE **real** (dashboards) precisa do projeto Firebase recriado (camada 4 do rename, pendente —
  `google-services.json` ainda é `naveg-app-homol`). O **port** desacopla: telemetria é testável e
  o app compila/roda sem depender do projeto vivo. Analytics já é dependência; Crashlytics exige
  plugin/projeto — faseável. Impl Firebase completa fica atrás da recriação.

**Alternativas futuras**

- Flip profundo (número/Firestore só na promoção).
- Timber + CrashlyticsTree substituindo o espelho manual `Log.*` (roadmap fase 5 do AS).
- Generalizar `EmissaoTelemetry` para um `AppTelemetry` AS-wide quando outros fluxos precisarem.
