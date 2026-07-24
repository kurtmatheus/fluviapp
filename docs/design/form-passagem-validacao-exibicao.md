# Estudo de design — Form de passagem: validação e exibição

**Status:** Rascunho — Claude escreveu, revisão do analista pendente. Fotografia do estado corrente do form
de **emissão de passagem** (validação + exibição), com os desvios em relação ao molde e os achados
detectáveis no código. Semente para uma refatoração faseada.

> Conversa com o [ADR-0006](../adr/0006-molde-de-cadastro.md) (molde de cadastro: VM dona do estado,
> UiState puro, validação pura, evento one-shot — que o **form de Viagem** já cumpre e serve de prova), o
> [ADR-0013](0013-tabela-de-tarifa-e-tipo-tarifario.md) (tarifa/gratuidade/cilindrada, guardas de emissão)
> e o [ADR-0012](../adr/0012-ciclo-de-vida-passagem-e-embarque-qr.md).

---

## 1. Diagnóstico: o form de passagem é anterior ao molde

O form de Viagem foi refeito no molde; o de passagem carrega o padrão antigo. Três desvios estruturais,
todos verificáveis:

### 1a. Validação impura (efeito colateral)

`isFormulario…Valido()` chama um `validarFormulario(state)` que **muta o `uiState`** (seta flags
`isXError`) e depois **relê essas flags** para devolver o `Boolean`
(`ValidacaoFormPassagemHelper.kt:16-29`, `ValidacaoFormPassageiroHelper.kt:16-40`,
`ValidacaoFormVeiculoHelper.kt:11-20`). É o oposto do `validarViagem(state): ErrosViagem` **puro**.
Consequências:

- **Não é JVM-testável** como função pura (precisa de `MutableStateFlow`); a Viagem tem `ValidacaoViagemTest` puro.
- Mistura **calcular** erros com **aplicar** erros ao estado.
- **Monotônica**: os validadores só setam `true`, **nunca resetam para `false`** — dependem do `onChange`
  de cada campo para limpar. Um erro (sobretudo cruzado) pode "grudar" se o gatilho não for tocado.

### 1b. Lambdas no UiState

`FormPassagemUiState`/`Passageiro`/`Veiculo` carregam os `onXChange` **como campos de estado**, populados
via `uiState.update { it.copy(onValorPagoChange = {…}) }` nos helpers (`FormPassagemHelper.atualizaCampos`).
O molde pede UiState puro (só dados + flags) e eventos como **métodos do VM** — como a Viagem faz.

### 1c. `runBlocking` no `init` dos helpers

`atualizaCampos` carrega as listas com `runBlocking { constanteRepository… }` na **construção**, bloqueando
a thread; a Viagem usa `carregarFontes` suspenso em `viewModelScope`. Risco de jank/ANR + dívida de
threading.

> É a mesma "dívida: mappers passagem/balanço" do molde — aqui materializada no **form**.

## 2. Achados detectáveis no código (bugs / inconsistências)

Priorizados por risco:

1. **`agencia`/`agente`: erro setado mas ignorado.** `validarFormulario` seta `isAgenciaError`/`isAgenteError`
   (`ValidacaoFormPassagemHelper.kt:63-77`), mas o `return` do `isFormularioPassagemValido` tem essas
   checagens **comentadas** (`:21-22`). O campo pode ficar vermelho **e o form salvar mesmo assim**.
2. **Cilindrada da moto não é validada.** Moto sem `cc` passa na validação (`ValidacaoFormVeiculoHelper`
   não checa cilindrada), mas o save **bloqueia com "sem tarifa cadastrada"** (fail-closed:
   `resolverTarifaBase`→`tarifaMotoBase(cc null)`→null). A mensagem **engana** — a causa é cilindrada
   ausente, não tarifa da viagem.
3. **Cross-field suspeito p3→p1.** `ValidacaoFormPassageiroHelper.kt:67` e `:145`:
   `tipoDocumentoPassageiro3.isNotBlank() && documentoPassageiro1.isBlank()` gatilham erro — o tipo-doc do
   passageiro **3** condicionado ao doc do passageiro **1**. Cheira a copy-paste; confirmar a intenção.
4. **`corVeiculo`: checada no `isValid`, nunca setada.** `isFormularioVeiculoValido` lê `isCorVeiculoError`,
   mas `validarFormulario` nunca o seta → check morto (cor sempre "válida"). Se cor é opcional, remover a
   checagem; se obrigatória, setá-la.
5. **Idade da criança = `minusYears(6)` (número mágico).** `ValidacaoFormPassageiroHelper.kt:102` codifica
   a faixa etária como `6` solto, **desacoplado** do `CRIANCA_ATE_5` do ADR-0013. Aparenta corresponder a
   "< 6 anos = até 5 inclusive" (correto), mas é frágil: mudou a regra, esse `6` não acompanha o tipo.
   Amarrar ao domínio.

## 3. UX de exibição

- **Fail-closed via toast transiente.** Os bloqueios de emissão (sem tarifa / cota de gratuidade,
  `FormPassagemViewModel.salvarPassagem`) são **toast** — some rápido, não aponta o campo/causa. Melhor:
  erro inline ou banner persistente com causa acionável ("cadastre a tarifa da acomodação X na viagem" /
  "informe a cilindrada").
- **`isFormaPagamentoEnabled` hardcoded `true`** no `FormPassagemUiState` (a capability do ADR-0002 que o
  `PassagemDadosPassagemMapper` **deriva** do agente) — no form é sempre true, então não gateia nada aqui.
  Verificar se é intencional (todos os agentes selecionam) ou verdade morta.
- **Desconto vestigial.** `FormPassagemUiState.desconto`/`onDescontoChange`/`isDescontoEnabled` sem input de
  UI (bloco removido no ADR-0013); lido só no fallback do mapper e serializado no rascunho (ADR-0004).

## 4. Direção proposta (a semear com o analista)

Trazer o form de passagem para o molde, começando pelo maior ganho: **validação pura**.

- **`validarPassagem(state…): ErrosPassagem`** puro (espelha `validarViagem`) — uma função (ou uma por
  sub-form) que **devolve** os erros em vez de mutar; o VM aplica os erros ao estado e decide o save. De
  quebra, torna os achados do §2 triviais de cobrir por teste JVM.
- **UiState puro** — mover os `onXChange` para métodos do VM (fatia à parte, maior; pode vir depois).
- **`runBlocking` → `carregarFontes` suspenso** (fatia à parte).
- **Corrigir os bugs do §2** junto da validação pura (agencia, cilindrada, cross-field, cor, idade amarrada
  ao `CRIANCA_ATE_5`).
- **UX**: fail-closed com causa acionável (inline/banner) no lugar do toast cego.

Fatiável e reversível — a validação pura primeiro (isolada, testável), depois estado/threading, por último
a UX do bloqueio.

## 7. Mapa do §1b — migração das lambdas do UiState para métodos do VM

Superfície (medida no código):

- **~40 lambdas** (`onXChange`/`onClick…`) como campos de estado em 3 UiStates —
  `FormPassagemUiState` (16), `FormPassageiroUiState` (20), `FormVeiculoUiState` (9).
- **~42 consumos** nas telas Compose (`state.onX`) em 4 `Content…AreaForm` — Passageiro (20), Pagamento
  (11), Veículo (9), Agência (2, hoje comentada).
- **3 helpers** (`FormPassagemHelper`/`FormPassageiroHelper`/`FormVeiculoHelper`) populam as lambdas em
  `atualizaCampos` via `uiState.update { it.copy(onX = { atualizarX(it) }) }`.

**Mecânica (baixo risco de lógica, alta superfície).** A lógica **já vive em métodos dos helpers**
(`atualizarValorPago`, `atualizarAcomodacao`, …) — o lambda no estado só **encaminha** para eles. Migrar
não reescreve lógica, só a **fiação**:

1. **VM expõe os eventos como métodos** (`fun onValorPagoChange(v) = formPassagemHelper.atualizarValorPago(v)`),
   tornando os `atualizar…` dos helpers acessíveis.
2. **UiState puro** — remover os campos-lambda dos 3 estados.
3. **Content…AreaForm recebe os `onX` como parâmetros** (padrão do `ContentViagemAreaForm`), no lugar de
   ler `state.onX`.
4. **`FormPassagemScreen` + o NavComposable threadam `viewModel::onX`** até os Content.

**Riscos:** superfície grande e **UI-não-testável** (só compila + verificação visual). Cada evento
perdido/mal-threadado = campo que não responde. Casos especiais: `onObservacaoChange: (String, Boolean)`,
`isAcomodacaoSelecionada: (String) -> Boolean` (é **predicado**, não evento — tratar à parte),
`onClickLimpar…`.

**Fatiamento proposto (por sub-form, do menor):** Veículo (9) → Passageiro (20) → Passagem (16). Cada fatia
= UiState puro + métodos no VM + Content por parâmetro + Screen threada; compila e é verificável no app por
sub-form. O `atualizarListaAgente` (runBlocking na mudança de agência) e a área de Agência ficam para a
rework do agente/Equipe.

## 6. Agente = usuário; plataforma multi-agência (direção do analista)

Decisão do analista que reenquadra os achados de `agencia`/`agente`/capability: **o agente É o usuário** da
plataforma. A plataforma vai **atender várias agências** e **exigir o cadastro** (do usuário e da agência)
na própria plataforma. Consequências:

- Hoje `agente`/`agencia` são **texto livre** no form (`FormPassagemUiState.agente`/`agencia`), e a
  capability de forma de pagamento é derivada por **casamento de nome** (best-effort,
  `PassagemDadosPassagemMapper`: `obterAgentesPorAgencia(...).firstOrNull { it.descricaoNome == agente }`).
  Frágil.
- **Direção:** o agente deixa de ser texto livre e passa a ser o **usuário logado** — o `funcionarioId`
  (uid) já é congelado na emissão (ADR-0010), então a identidade existe; falta a **agência como entidade de
  primeira classe** (cadastro obrigatório na plataforma) vinculada ao usuário. `agente`/`agencia` do
  bilhete derivam daí, não de digitação.
- **Reenquadramento dos achados:** o "erro de `agencia`/`agente` setado mas ignorado" (§2.1) e o
  "`isFormaPagamentoEnabled` hardcoded true" (§3) são **sintomas dessa lacuna** — resolver na **rework de
  identidade do agente/agência**, não como patch de validação agora. Merece ADR próprio (multi-tenant:
  usuário ↔ agência ↔ plataforma).

**Renomeação: "Agentes" → "Equipe" no menu (direção do analista).** A seção **Agentes** do menu lateral
(`SecaoMenu`) passa a se chamar **Equipe** — coerente com "agente = usuário": o que hoje é *cadastro de
agente* vira o **cadastro de membros da equipe** (os usuários operadores de uma agência). É a face de UI da
mesma rework de identidade: a agência (tenant) tem uma **Equipe** de usuários, e cada emissão carimba o
membro (o `funcionarioId`/uid já existente). **Não iniciar agora** — só registrado aqui como direção; a
renomeação + o cadastro de equipe entram com o ADR de identidade do agente (multi-tenant), não neste
incremento de validação.

## 5. Perguntas para o analista (seed)

1. **Intencional?** `corVeiculo` opcional (remover o check)?; `isFormaPagamentoEnabled` sempre true (capability
   aposentada no form)?; `agencia`/`agente` devem **voltar** a ser obrigatórios ou o comentário é proposital?
2. **Cross-field p3→p1** (`:67`/`:145`) — bug de copy-paste ou regra real?
3. **Ordem das fatias:** validação pura primeiro (recomendo), depois UiState puro, depois threading, por
   último a UX do fail-closed? Ou priorizar a UX do bloqueio (mais visível ao operador)?
4. **Escopo da 1ª fatia:** só `validarPassagem` pura + os fixes do §2, sem tocar lambdas/threading?