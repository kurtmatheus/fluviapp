# ADR-0012: Ciclo de vida da passagem e confirmação de embarque por QR

**Status:** Aceita (direção) — decisões de revisão fechadas (ver *Decisões resolvidas*); implementação por
fase, nenhum código ainda. Claude rascunhou, analista revisou.

> Conversa com o [ADR-0010](0010-autorizacao-por-cargo.md) (política única por cargo, eixo de ação —
> ganha um eixo novo aqui), o [ADR-0011](0011-regras-firestore-por-cargo.md) (a fronteira no servidor,
> que passa a impor a transição de status), o [ADR-0008](0008-relacionamentos-por-identidade.md) (posse
> por `funcionarioId`, reusada para carimbar quem validou o embarque), o
> [ADR-0009](0009-sincronizacao-reativa-firestore-room.md) (SSOT Room←Firestore — a transição de status
> hoje vaza essa costura), o [ADR-0007](0007-observabilidade-cadastros.md) (auditar quem/quando) e o
> [ADR-0003](0003-modelo-de-memoria-do-dado.md) (normalização em trânsito, migração aditiva "stack
> dormente"). Materializa o `briefing-projeto-checkin-navio.md` (QR de embarque).

## Contexto

A feature pedida é **confirmar a passagem com QR Code**: um bilhete digital com QR que, lido no
embarque, confirma o uso. Mas o QR só "faz sentido" se o **ciclo de vida** da passagem tiver um estado
que a leitura consuma. Hoje ele não tem — e o status carrega três rachaduras estruturais.

- **Ciclo raso, tipo emprestado.** `Passagem.status` é `String` (`Passagem.kt`), e os valores vêm do
  enum genérico de catálogo `Constante.Descricao` (`Constante.kt:51-53`): só `A_EMITIR` e `EMITIDA`. O
  status do domínio está pendurado no mesmo enum que alimenta dropdowns — não é tipo próprio.
- **Grafia à deriva — o mesmo bug que o ADR-0010 matou no cargo.** Nasce `"A EMITIR"` (com espaço,
  `FormPassagemHelper.kt:431`), transiciona para `"EMITIDA"` (sem espaço, `ImpressaoHelper.kt:36`), e os
  testes usam `"A_EMITIR"` (underscore). Três grafias do mesmo conceito. Como a **busca filtra por
  status** (`PassagemFirestoreRepository.obterTodasPorDataStatus`), é bug latente de filtro.
- **Transição acoplada à impressão e vazando o SSOT.** A única mudança de status
  (`atualizarSituacao`, `PassagemFirestoreRepository.kt:174`) é disparada por *imprimir*
  (`ImpressaoHelper.kt:102`) e **só grava no Firestore, não no Room** — o espelho local diverge
  (ADR-0009). Não há máquina de estados: `atualizarSituacao(id, qualquerString)` aceita qualquer valor.
- **Status quase invisível.** Aparece só como texto puro nos Detalhes (`DetalhamentoPassagemContent.kt:334`),
  nunca no card da lista (`PassagemPreviewCard`) — não se distingue um bilhete emitido de um embarcado.

Dois habilitadores já existem e barateiam a feature:
- **O QR já é gerado e já codifica `Passagem.id`** no bilhete físico (`ImpressaoHelper.kt:119`, via
  `QRCodeGenerator`/ZXing). O bilhete **digital** (`ImpressaoDigitalDialog.kt`) ainda **não** desenha QR.
- **`funcionarioId` (uid)** na Passagem (ADR-0010 Fase 2) — dá para carimbar *quem validou* o embarque
  sem heurística de nome.

**Diagnóstico:** hoje "emitir" ≈ "imprimir". Não existe um estado que represente *o passageiro
embarcou*. Sem ele, escanear um QR não tem o que mudar. A feature exige, antes de tudo, endireitar o
ciclo de vida.

## Decisão

Introduzir o estado de embarque e tratar o status como **tipo de domínio com máquina de estados**,
espelhando as decisões que o ADR-0010 já tomou para o cargo. Ciclo alvo (decidido):

```
  criar                emitir/entregar          escanear QR no embarque
 ───────►  A_EMITIR ──────────────► EMITIDA ──────────────────────► EMBARCADA
 (rascunho)            (imprime/                (check-in: valida e        (bilhete usado)
                        compartilha;             consome o bilhete)
                        QR válido)
```
(Cancelar continua sendo **delete físico**, como hoje — `CANCELADA` como estado fica para o futuro.)

1. **Status como tipo, String na fronteira.** Criar `enum class StatusPassagem { A_EMITIR, EMITIDA,
   EMBARCADA }` com `de(valor: String?): StatusPassagem?` (fail-closed; **tolerante à grafia legada** —
   normaliza `"A EMITIR"`/`"A_EMITIR"` → `A_EMITIR`). Gravar sempre o `.name` canônico; formatar só na
   exibição. O catálogo `Constante(STATUS_PASSAGEM)` deixa de ser o *tipo* e fica só como fonte das
   **opções de filtro** na Pesquisa. Conserta a grafia à deriva **e** o filtro da busca por tabela.
2. **Transição como máquina de estados.** Uma função de domínio `transicionar(de, para)` que só permite
   arestas legais (`A_EMITIR→EMITIDA`, `EMITIDA→EMBARCADA`) e é *fail-closed*. É onde moram a
   **idempotência** (re-scan de `EMBARCADA` → recusa "bilhete já utilizado") e a guarda "não embarca
   bilhete não emitido" (`A_EMITIR→EMBARCADA` proibido). Substitui o `atualizarSituacao(anyString)`.
3. **QR é ponteiro, não credencial (firestore-driven).** O QR permanece `= Passagem.id` (reusa o que já
   existe no físico). O scanner **lê o doc ao vivo**, valida o status corrente e transiciona; a fronteira
   real é o servidor. O bilhete **digital ganha QR** (paridade com o físico) — ponto natural em
   `ImpressaoDigitalDialog.kt`.
4. **Autorização do embarque é um eixo NOVO.** Validar embarque ≠ editar conteúdo do bilhete. A política
   única (ADR-0010) ganha `podeConfirmarEmbarque(cargo)` = **qualquer cargo conhecido** (quem está na
   doca valida, mesmo sem ter vendido). *Não* colapsar em `podeEditarQualquerPassagem`.
5. **A transição passa a ser imposta nas regras (estende o ADR-0011).** `firestore.rules` só aceita
   avanço `A_EMITIR→EMITIDA→EMBARCADA`, **nunca retrocesso**, e só por quem `podeConfirmarEmbarque`; o
   `funcionarioId` segue imutável (ADR-0011). A suíte de emulador (`firestore-tests/`) ganha os casos.
6. **Corrigir o vazamento de SSOT.** A transição espelha no Room também (offline-first, ADR-0009), não só
   Firestore — hoje `atualizarSituacao` só toca o Firestore.
7. **Registro do operador que embarcou, gravado no doc (ADR-0007 + ADR-0008).** A confirmação carimba
   **quem** validou e **quando**, de forma aditiva (schemaless no Firestore, `ALTER TABLE` no Room):
   - `embarcadaPorId: String` — **uid** do operador que escaneou (chave estável, ADR-0008; vem de
     `usuarioLogado.id`, o mesmo `funcionarioId` que a emissão usa).
   - `embarcadaPor: String` — **nome** do operador como *snapshot* de exibição/impressão (mesmo par
     id+snapshot da emissão: `funcionarioId`/`funcionarioResponsavel`).
   - `embarcadaEm: String` — timestamp da confirmação.
   Espelham no DTO `PassagemDocumento` e na entidade `Passagem`/Room. O check-in é o evento mais
   auditável do ciclo; o par id+snapshot evita lookup e sobrevive a rename (ADR-0008).
8. **UI de status (badge/cor/label).** Criar (não existe) e exibir **também no card da lista**, não só
   nos Detalhes.

## Plano de migração (faseado, aditivo — "stack dormente" do ADR-0003)

- **Fase 1 — Tipo + FSM + grafia (sem UI nova). FEITA.** `StatusPassagem` (A_EMITIR/EMITIDA/EMBARCADA)
  + `de()` tolerante à grafia legada + `rotulo()` + FSM `podeTransicionarPara`/`ehTerminal`. Criação
  grava o `.name` canônico (`FormPassagemHelper`); `atualizarSituacao(anyString)` → `transicionar(
  StatusPassagem)` fail-closed, idempotente e espelhando o Room (`PassagemFirestoreRepository`);
  `ImpressaoHelper` usa a transição tipada; a busca canoniza o filtro (`obterTodasPorDataStatus`); a
  exibição formata via `rotulo()` (`PassagemDadosPassagemMapper`). *Sem backfill* (portfólio; docs
  legados leem via `de()`). Testes JVM `StatusPassagemTest` (verdes) + `testDebugUnitTest` compila o
  main OK.
- **Fase 2 — Bilhete digital com QR. FEITA.** QR (`= idPassagem`) desenhado no rodapé do
  `ImpressaoDigitalDialog` (composable `QrCodeEmbarque`), reusando `QRCodeGenerator` (ZXing) — paridade
  com o físico. Entra no bitmap capturado/compartilhado; fundo branco fixo; só renderiza com id presente.
  Rótulo `label_qr_embarque`. Compila (`compileDebugKotlin`).
- **Fase 3 — Tela de validação/embarque (scanner).** Leitor de QR por câmera com **CameraX + ML Kit
  Barcode Scanning** (offline, integra com Compose), resolve a passagem pelo id, mostra os dados e
  confirma `EMITIDA→EMBARCADA` carimbando `embarcadaPorId`/`embarcadaPor`/`embarcadaEm`. Consome o eixo
  `podeConfirmarEmbarque`. Arquitetura da tela (decidido):
  - **Scaffold próprio** — a tela de embarque não reaproveita o Scaffold da main; abre o seu (topbar/
    conteúdo/estado próprios). Fica **atrelada ao menu principal** como destino, mas isolada.
  - **ViewModel próprio** (`EmbarqueViewModel`/`ValidacaoEmbarqueViewModel`) injetando o
    `PassagemFirestoreRepository` (Hilt) — o mesmo repo da emissão; sem repo novo. Segue o molde de
    cadastro (VM dona do estado, UiState puro, evento one-shot).
  - **Permissão `CAMERA`** tratada explicitamente: declaração no manifesto + fluxo de *runtime
    permission* em Compose (pedir/negar/ir às configurações), com estado próprio no UiState (sem
    permissão → não abre o preview, mostra rationale).
  - **Acesso**: 3ª opção **no centro** da barra inferior (ver *UI e navegação*) — ação de rotina, a um
    toque.

  Sub-checkpoints: **3a — fundação de domínio. FEITA.** Campos `embarcadaPorId`/`embarcadaPor`/
  `embarcadaEm` em `Passagem`+`PassagemDocumento`+mappers; migração Room v12→v13 (aditiva); eixo
  `PermissoesUsuario.podeConfirmarEmbarque` (qualquer cargo conhecido) + testes; suíte JVM verde.
  **3b — câmera + validação. PENDENTE.** Deps CameraX + ML Kit; `confirmarEmbarque` no repositório
  (leitura ao vivo do Firestore pelo id + carimbo do operador + FSM); `EmbarqueViewModel` +
  `EmbarqueScreen` (Scaffold próprio) com permissão `CAMERA`. **Não verificável em runtime aqui.**
- **Fase 4 — Regras Firestore da transição** (estende ADR-0011). **FEITA.** `firestore.rules` passa a
  impor a FSM no `update` de `passagens`: `funcionarioId` imutável (ADR-0011) **e** `transicaoStatusLegal()`
  (só as arestas `A_EMITIR→EMITIDA`/`EMITIDA→EMBARCADA` ou status inalterado — bloqueia retrocesso e o
  pulo `A_EMITIR→EMBARCADA`), com dois eixos de autz: **confirmação de embarque** (`EMITIDA→EMBARCADA`
  carimbada) por `podeConfirmarEmbarque()` = qualquer cargo conhecido; qualquer outra edição pelo gate
  ADR-0011 (dono ∨ editar-qualquer). A confirmação é *endurecida*: `ehConfirmacaoEmbarque()` exige que o
  update toque **só** os 4 campos do embarque (`hasOnly`, sem contrabandear edição de conteúdo por
  não-dono) e que carimbe o **próprio uid** como `embarcadaPorId` + nome + timestamp (não dá forjar
  autoria, como o `funcionarioId==uid` da emissão). Grafia legada `"A EMITIR"` normalizada só na leitura
  (espelha `StatusPassagem.de`). Suíte de emulador (`firestore-tests/`) ganha 10 casos novos (embarque
  por não-dono OK, forjar autoria negado, sem carimbo negado, piggyback negado, retrocesso/pulo negados).
- **Fase 5 — UI de badge/cor de status** (Detalhes + card da lista).

## Consequências

- **O QR ganha semântica**: idempotência (antifraude de reuso), guarda de estado e auditoria de quem/
  quando embarcou. O que a UI mostra o servidor passa a impor.
- **Grafia unificada conserta o filtro de busca** (efeito colateral positivo, como no ADR-0010).
- **Dever de paridade cresce**: a transição vive agora na FSM Kotlin **e** nas regras — mesma disciplina
  do ADR-0011; a suíte de emulador trava. Mudou uma aresta? muda nos dois.
- **Rede no embarque** (firestore-driven): validar exige conectividade para ler o doc ao vivo. Offline
  ficaria para token assinado (futuro).
- **Novo eixo de autorização** — a política única cresce de forma coerente (não é regra dispersa).
- **Grafia legada**: `de()` tolerante na leitura; escrita sempre canônica. Docs antigos com `"A EMITIR"`
  continuam legíveis.
- **Nova dependência**: câmera/scanner + permissão `CAMERA` no manifesto.

## Alternativas consideradas

- **Reusar `Constante.Descricao` como tipo (status quo)** — rejeitado: é a origem da grafia à deriva.
- **`CANCELADA`/expiração como estados agora** — fora do escopo; delete físico permanece. Registrado
  como futuro (evita mexer no fluxo de deletar e em regra temporal/job neste incremento).
- **QR com token assinado (offline)** — rejeitado por ora (cripto + gestão de chave, sem backend).
- **QR com payload JSON completo** — rejeitado (grande, falsificável, desatualiza).

## Alternativas futuras

- **`CANCELADA`/`EXPIRADA` (no-show)** como estados, com auditoria em vez de delete físico.
- **Validação offline** por token assinado + sync posterior.
- **Número de poltrona/assento** (hoje só `acomodacao`, sem assento numerado).

## Decisões resolvidas na revisão (analista)

- **Lib do scanner**: **CameraX + ML Kit Barcode Scanning** (offline, integra com Compose).
- **`podeConfirmarEmbarque` = qualquer cargo conhecido** — embarque é ação de doca; quem está lá valida.
- **Embarque é irreversível** — a FSM não tem aresta de retrocesso (`EMBARCADA` é terminal); nem gestor
  desfaz nesta fase. "Desfazer embarque" fica como refino futuro. Corrigir scan errado é decisão de
  operação (fora do app por ora).
- **Acesso**: 3ª opção fixa no menu principal (barra inferior), ver *UI e navegação*.

## UI e navegação (Fase 5 — redesign da barra inferior)

**Estado atual** (`FluviBottomAppBar.kt`): `BottomAppBar` custom com **2 itens** — *Início* (`Home`, só reseta
o conteúdo da Main para HOME) e *Menu* (`Menu`, abre o `ModalNavigationDrawer` ancorado à direita). Fundo
`HeaderNavy` nos dois temas; realce só do *Início*, um retângulo translúcido a 12%; **sem FAB, sem
indicador de seleção Material 3**. Ponto crucial: **a barra não navega** — a navegação real mora no
NavHost (`FluviAppNavHost.kt`) e sai do drawer.

**Layout decidido** — `Início | [Embarque] | Menu`:
- **Início** à esquerda (mantém).
- **Embarque no centro**, como **FAB elevado** em `colorScheme.primary` (AquaAccent) com
  `Icons.Filled.QrCodeScanner` — a única cor de acento da barra, destaca a ação de rotina.
- **Menu à direita** (mantém posição e gesto — abre o drawer).
- **Seleção unificada**: trocar o retângulo a 12% por **pílula arredondada** (padrão Material 3) com
  ícone/label tingidos de acento, aplicável agora aos itens de rota.
- Barra segue **HeaderNavy** nos dois temas; o que muda é o conteúdo. (Mockup visual desta proposta
  produzido à parte para revisão.)

**Plugagem na navegação** (a parte nova — a barra passa a disparar rota):
- Nova rota-folha `EmbarqueNavComposable("embarque")` em `FluviAppNavComposableDestinations`.
- `NavHostController.navegaParaEmbarque()` usando `navegaDireto` (`popUpTo(start){saveState}` +
  `launchSingleTop` + `restoreState`) — o padrão certo para uma entrada fixa de barra.
- `NavGraphBuilder.embarqueNavComposable(...)` (espelha `BalancoNavComposable.kt`): `hiltViewModel<
  EmbarqueViewModel>()`, coleta o `uiState`, chama `EmbarqueScreen` (**Scaffold próprio**).
- Propagar `onClickEmbarque` pela cadeia `FluviBottomAppBar → CommonScaffold → CommonScreen → MainScreen
  → MainScreenNavComposable` até `navController.navegaParaEmbarque()`.
- Permissão `CAMERA`: reusar a infra `RequestPermission` (`ui/components/Permissions.kt`, hoje usada p/
  Bluetooth) + declaração no manifesto.
- Dependências novas: CameraX + ML Kit Barcode (o projeto hoje só **gera** QR via ZXing p/ impressão;
  não lê por câmera).