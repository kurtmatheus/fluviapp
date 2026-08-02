# Roadmap para o MVP

**Status:** Pilares **1 e 2 completos** (2026-07-27); resta o **3**, que **cresceu de escopo** em 2026-07-28:
deixou de ser só esteira de CI/CD e passou a ter um **domínio de plataforma** por baixo
([ADR-0016](../adr/0016-dominio-da-plataforma.md)). Não é código; é o sequenciamento dos passos até o MVP e as
sementes de ADR que faltam.

O MVP tem **três pilares** (na ordem proposta pelo analista):

1. **Contagem de Passagem** — melhorias no balanço como ele é hoje, **sem faturamento** ainda.
2. **Rework de Agente → Equipe** — agência/lotação viram capacidades do usuário; a agência vira capacidade
   transversal à emissão, com identidade visual por agência (multi-agência).
3. **Plataforma + CI/CD via Firebase** — o **painel administrativo** (que substitui o seed como porta de entrada
   do dado) e a distribuição de builds aos testers.

---

## Estado atual (a base já pronta)

- Form de passagem **no molde ADR-0006**: validação pura, `runBlocking`→suspenso, UiState puro, UX
  fail-closed (banner). Emissão governada pelo modelo de preço tabelado do **ADR-0013**.
- **ADR-0013 fechado + emenda**: tipo tarifário (meia/gratuidade) **só na REDE**; suíte/camarote/veículo
  sempre inteira.
- **Contagem** (`BalancoPassagensMapper`) estudada (`balanco-passagens-mapper.md`): threading já limpo;
  decisões de contagem tomadas (§7).
- ~~**Agente/agência hoje**: texto livre no bilhete; área de agência do form comentada; `atualizarListaAgente`
  com `runBlocking`; logos no repo mas não usados.~~ **Resolvido pelo Pilar 2** (ADR-0015, P2.3/P2.4): a agência
  do bilhete vem do emissor, `Passagem.agente` morreu, a área manual e o último `runBlocking` do form saíram, e
  a marca da agência entrou no bilhete digital. `funcionarioId = uid` congela desde o ADR-0010.
- **Insight estrutural** (`viagem-vs-trecho.md`): a Viagem de hoje é o Trecho. **Entrou no MVP** pelo
  [ADR-0016](../adr/0016-dominio-da-plataforma.md) (§7), e em dois níveis em vez de um — **Trecho** (par de
  cidades, compartilhado) × **Rota** (a viagem de verdade, da agência). A versão pesada da nota (ocorrências
  persistidas com contador por acomodação) segue fora.

---

## Pilar 1 — Contagem de Passagem (sem faturamento)

As fatias do `balanco-passagens-mapper.md §7`, **excluindo a fatia 4** (módulo Faturamento) — que é o
"por enquanto sem faturamento". Ordem:

- **P1.1 — FORM: tipo tarifário só-rede. ✅ FEITO (commit 4e62067).** `ValidacaoPassageiro` só exige tipo/
  gratuidade na rede (+2 testes); `ContentPassageiroAreaForm` só exibe os dropdowns na rede (filtro de MEIA
  removido); `atualizarAcomodacao` limpa tipo/gratuidade ao sair da rede + reabilita pagamento. Fora da rede
  = inteira (vazio → mapper/preview tratam como INTEIRA).
- **P1.2 — Contagem: suíte por bilhete + `associateBy`. ✅ FEITO (commit d118021).** 1 suíte por bilhete
  (`temPassageiro3`→3p, senão 2p; solo conta); `contador` extraído p/ função pura `contarOcupacaoNavio` +
  `associateBy` no lookup; +4 testes por caso. Breakdown segue no ramo REDE (coerente com P1.1).
- **P1.3 — Renomear módulo → "Contagem de Passagem". ✅ FEITO (commit 6014b5a).** As telas já diziam
  "Contagem de Passagem"; o **código** ainda dizia Balanço — e morava no pacote `faturamento`.
  `Balanco*` → `ContagemPassagem*`, pacote `faturamento` → `contagem`, rota `listaRelatorios` →
  `contagemPassagem`, chaves de string. Não foi só cosmético: o ADR-0015 §6 separa Contagem (ocupação,
  **cross-agência**, recurso compartilhado) de Faturamento (financeiro, **isolado por agência**) — a
  contagem morando dentro de `faturamento` convidava a confusão que o desenho evita. **Sem filtro por
  cargo**, como decidido: a Contagem vive na seção Passagem, visível a todos.
- **P1.4 — Threading. ✅ FEITO (commit 6014b5a).** `obterTodasPorDataStatus` gravava no Room com um
  `runBlocking { dao.salvar(it) }` **por documento**, dentro do listener de sucesso — N bloqueios de
  thread por consulta —, e a travessia acontecia **duas vezes** (o ViewModel remapeava o mesmo snapshot).
  Agora é `suspend`, devolve a lista mapeada e grava em **uma** transação (`salvarTodas`).

**Pilar 1 fechado** com P1.1–P1.4. A fatia 4 (Faturamento) segue fora por definição — é o "sem faturamento".

**Pré-requisito de dado — ✅ RESPONDIDO pelo [ADR-0016](../adr/0016-dominio-da-plataforma.md).** A pergunta era
se o MVP **semeia tarifas** (o seed não as populava, então a contagem/emissão dependia de viagem criada à mão).
A resposta veio por outro caminho: **o seed morre** (ADR-0016 §1) e a tarifa entra no **cadastro da rota**
(§7), pelo supervisor da agência. O app deixa de demonstrar sozinho num projeto novo por decisão, não por
falta — em troca, todo dado tem autor.

## Pilar 2 — Rework de Agente → Equipe ([ADR-0015](../adr/0015-rework-agente-equipe.md))

O maior pilar. É a rework de identidade/multi-agência que o §6 do estudo do form já apontava.

> **✅ COMPLETO** (2026-07-27). O [ADR-0015](../adr/0015-rework-agente-equipe.md) é a fonte: ele passou por
> uma **revisão estrutural** (dois contextos, sistema × negócio) que reescreveu boa parte do plano — o
> `Agente` não morreu, virou `Funcionario`; nasceram `Usuario.papel` × `Funcionario.cargo`, o elo
> `funcionarioId` e o regime de schema como DDL (§9). O resumo abaixo é o **plano original**, mantido como
> registro; onde ele diverge do ADR, vale o ADR.

- **P2.1 — Vocabulário: "Equipe" + cargos novos. ✅ FEITO** (ADR-0015 §4.2). Menu "Agentes" → "Equipe"
  (`SecaoMenu.AGENTE` → `EQUIPE`) **e** o rename dos cargos: `DIRETOR` → `GESTOR` (gestor **do sistema**),
  `COLABORADOR_MASTER` → `SUPERVISOR` (master **da agência**), `OPERADOR` → `AGENTE`; `ehGestor` →
  `ehCargoPlataforma`. Não foi cosmético: o cargo é String persistida — `firestore.rules`, a suíte de
  emulador (34 verdes) e os defaults de autocadastro foram no mesmo commit. Perfis já gravados com o cargo
  antigo caem em fail-closed até serem corrigidos no console (usuários não são semeados).
- **P2.2 — Agência e lotação como capacidades do usuário (migração).** Hoje agência é texto livre + entidade
  `Agente` à parte. Passa a ser **atributo do usuário** (`Usuario`/`Agente` ganha `agencia` + `lotacao`).
  Migração Room + espelho Firestore + form de cadastro do membro da Equipe. Consolida "agente = usuário
  logado" (o `funcionarioId`/uid já é a âncora — ADR-0010/0008).
- **P2.0 — Aposentar `podeSelecionarFormaPagamento` + o `valorPago` avulso. ✅ FEITO** (ADR-0015 §4a; supera o
  ADR-0002): a capability era resíduo da proposta antiga (bilhete de check-in) — o app hoje emite a passagem e
  o check-in é o QR; o "valor pago" avulso era o par dela. Duas migrações Room (17→18 e 18→19).
- **P2.3 — Agência transversal à emissão.** A emissão **deriva a agência do usuário logado** (não digita
  agente/agência à mão; a área comentada do form é aposentada). Remove a dívida do `runBlocking` de
  `atualizarListaAgente` e as validações órfãs de agência/agente.
- **P2.4 — Identidade visual por agência.** O bilhete/impressão usa o **logo da agência** emissora
  (`logo1/logo2.png` já no repo) — o branding por agência que motivou re-adicioná-los. Casa com o
  `FluviWordmark`/tema (identidade do app × identidade da agência emissora).
- **P2.5 — Aposentar `Agente`.** Remoção completa da entidade/DAO/repos/form/telas/testes (ADR-0015 §7),
  depois de P2.3. Diff quase todo deleção.
- **P2.6 — Escopo por agência na listagem.** Terceiro eixo em `PermissoesUsuario`: `ADM`/`GESTOR` são cargos
  **FluviApp** (atravessam agências); `SUPERVISOR`/`AGENTE` são de agência. Filtro por agência do
  logado nas consultas de passagem — **por UI**, sem regra Firestore no MVP. A Contagem fica fora do filtro
  (é cross-agência por definição).

**Dependência:** P2.3/P2.4 dependem de P2.2 (a agência precisa existir como capacidade do usuário antes de
ser transversal/visual). P2.1 é independente.

## Pilar 3 — Plataforma + CI/CD via Firebase

**Este pilar deixou de ser "só DevOps".** A análise de condições de setup (2026-07-28) mostrou que a esteira
distribuiria um app que **não se alimenta**: o `SeedFirestore` só roda em debug (`SeedFirestore.kt:40`), e num
projeto Firebase novo as regras do ADR-0011 negariam a escrita do seed de qualquer forma. Daí o
[ADR-0016](../adr/0016-dominio-da-plataforma.md): o **painel administrativo** vira a porta de entrada do dado, e
o app vira uma **plataforma multi-empresa e multi-segmento**.

### P3.A — Domínio e painel ([ADR-0016](../adr/0016-dominio-da-plataforma.md), fases F1–F8)

`Catalogo` → matar o seed → painel → **capacidades da plataforma** (porto/trecho) → **parte e atuação** (empresa +
`atuacoes` + concessão de trechos/portos/armadores) → funcionário multi-empresa → rota + tipo de embarcação →
regras e suíte. É a maior parte do pilar, e é **domínio**, não release.

O eixo do domínio é **parte × atuação × ativo** (ADR-0016 §4): agência não é entidade — é uma **atuação** de uma
empresa (`agenciaId` e `empresaId` são o mesmo id); navio é **ativo** e fica na raiz com `empresaId`, como já está.

Cada cadastro é um módulo com uma responsabilidade só (molde ADR-0006) — o que existe entre eles é uma **ordem de
dependência**, não um wizard: catálogo → porto/trecho e empresa → atuação/concessão → funcionário → rota.

### P3.B — Esteira (o que a análise verificou)

Baseline medido: `testDebugUnitTest assembleRelease` fecha **verde em ~3m30s**, com **238 testes JVM** em 41
classes e `lintVitalRelease` passando. O que falta não é qualidade de build — são condições de esteira:

- **P3.1 — Assinatura.** Hoje `assembleRelease` produz `app-release-unsigned.apk` (40,8 MB): não há
  `signingConfigs` no `app/build.gradle.kts`, e o App Distribution não instala APK sem assinatura. Decidir entre
  keystore em secret (base64) ou distribuir `assembleDebug`.
- **P3.2 — Secrets.** `google-services.json` é gitignored e o plugin `com.google.gms.google-services` **falha o
  build sem ele** — inclusive no job de testes, não só no de release. Não há remoto git configurado, e o repo é
  portfólio (repo público colide com o arquivo).
- **P3.3 — Versionamento.** `versionCode` é fixo (`9`): builds sucessivos ficam indistinguíveis na lista do
  tester, e nenhum crash no Crashlytics correlaciona com "qual build". Derivar de run number/tag.
- **P3.4 — Gate das regras.** A suíte de `firestore-tests/` roda em `demo-fluviapp`, **sem credenciais** — de
  graça em qualquer runner. Ela deveria ser **gate do deploy de `firestore.rules`**, que hoje vai à mão. É o
  item de maior valor por menor custo do pilar.
- **P3.5 — Distribuição.** `firebase appdistribution:distribute` (service account) para um grupo de testers.
  Gatilho por tag. Falta `.firebaserc` (ausente).
- **P3.6 — Audiência: ✅ RESPONDIDO.** Grupo **manual no console** — e não por comodidade: a P2.2c removeu
  `cadastrar` do `AutenticacaoRepository`, o SDK cliente não cria conta de terceiro, e o ADR-0016 §10 fixou o
  **bootstrap do primeiro `ADM` como passo de ambiente** (console: Auth + `users/{uid}` com `papel: ADM`).
  A plataforma não tem caminho self-service, por decisão.

*P3.B é independente dos pilares 1/2 e pode ser puxado a qualquer momento; **P3.A é que segura o resto**, porque
sem painel a esteira entrega um app vazio.*

## Sequência e dependências (proposta)

```
Pilar 1 ✅ (P1.1 → P1.2 → P1.3 → P1.4)
Pilar 2 ✅ (ADR-0015: P2.0 → P2.1 → P2.2a′ → P2.2b → P2.2c → P2.3 → P2.4 → P2.6)
Pilar 3   P3.A (ADR-0016: F1 → F2 → F3 → F4 → F5 → F6 → F7, com F8 acompanhando)
          P3.B (P3.4 → P3.1/P3.2/P3.3 → P3.5)      ← P3.4 pode ir já; não depende de nada
```

Ordem sugerida: **P3.4 primeiro** (gate das regras: valor alto, zero credencial, independente de tudo), depois
**P3.A** (o domínio, que é o caminho crítico), e a distribuição por último — não faz sentido distribuir antes de
haver painel que alimente o app.

## Perguntas abertas (semear os ADRs)

**Pilar 1:** nada aberto. A pergunta das tarifas foi respondida pelo [ADR-0016](../adr/0016-dominio-da-plataforma.md)
(seed morre; tarifa entra no cadastro da rota).

**Pilar 2:** nada aberto — todas as perguntas que estavam aqui (lotação, override de agência, capability,
logo por agência, isolamento, destino do `Agente`, recorte da contagem) foram respondidas pelo analista e
estão registradas no [ADR-0015](../adr/0015-rework-agente-equipe.md) (*Decisões resolvidas*, 1ª e 2ª rodada).

**Pilar 3 — esteira (P3.B):**
- Provedor de CI (GitHub Actions?) e onde ficam os secrets (keystore, service account)? **Não há remoto git
  configurado** — é o primeiro pré-requisito.
- Artefato: APK (App Distribution) — AAB fica para a Play depois?
- Distribuir `release` assinado (keystore em secret) ou `debug` (assinado pela debug key)?
- ~~Grupo de testers manual ou sincronizado dos usuários?~~ **Respondido:** manual (ADR-0016 §10).

**Pilar 3 — domínio (P3.A):** os pontos abertos vivem no
[ADR-0016](../adr/0016-dominio-da-plataforma.md#pontos-abertos-analista-decide). O que trava o começo: o rename
`Constante`→`Catalogo` pode **regenerar** o schema do Room (ADR-0015 §9). Os demais são premissas a confirmar, não
bloqueios — a governança de trecho/porto ficou resolvida na 4ª rodada (são capacidades da plataforma, concedidas à
agência na abertura); só a **remoção** segue aberta.

---

> O rework "Viagem vira Trecho" (`viagem-vs-trecho.md`) **entrou no MVP** pelo
> [ADR-0016](../adr/0016-dominio-da-plataforma.md) §7 — na versão enxuta (dois níveis, ocorrências calculadas).
> A parte pesada (ocorrências persistidas + contador por acomodação) segue fora.