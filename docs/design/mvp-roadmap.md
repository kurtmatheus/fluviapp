# Roadmap para o MVP

**Status:** Pilares **1 e 2 completos** (2026-07-27); resta o **3**, que cresceu duas vezes — em 2026-07-28
ganhou um **domínio de plataforma** por baixo ([ADR-0016](../adr/0016-dominio-da-plataforma.md)), e em
2026-07-31/08-01 ganhou **dois eixos transversais** que não existiam quando este roadmap foi escrito: o
[ADR-0017](../adr/0017-eixo-de-storage-firestore-only.md) (o Room sai) e o
[ADR-0018](../adr/0018-agregado-passagem-participantes-modo-e-lancamentos.md) (o agregado Passagem
reescrito). **Revisado em 2026-08-01** contra os três. Não é código; é o sequenciamento dos passos até o MVP.

O MVP tem **três pilares** (na ordem proposta pelo analista):

1. **Contagem de Passagem** — melhorias no balanço como ele é hoje, **sem faturamento** ainda.
2. **Rework de Agente → Equipe** — agência/lotação viram capacidades do usuário; a agência vira capacidade
   transversal à emissão, com identidade visual por agência (multi-agência).
3. **Plataforma + CI/CD via Firebase** — o **painel administrativo** (que substitui o seed como porta de entrada
   do dado) e a distribuição de builds aos testers.

…e **dois eixos que atravessam o Pilar 3**, decididos depois e sem pilar próprio porque não são etapa: são
mudança de regime. Estão em [§ Os dois eixos transversais](#os-dois-eixos-transversais).

---

## Estado atual (a base já pronta)

- Form de passagem **no molde ADR-0006**: validação pura, `runBlocking`→suspenso, UiState puro, UX
  fail-closed (banner). ~~Emissão governada pelo modelo de preço tabelado do **ADR-0013**.~~ **A tabela
  adormeceu** ([ADR-0016 §7.2](../adr/0016-dominio-da-plataforma.md)): o dado é o **valor informado**, e a
  base passa a ser **inferida por agregação**. O bloqueio `SemTarifa` morre com ela.
- **ADR-0013**: sobrevivem os tipos tarifários e as funções puras; a emenda continua valendo — meia e
  gratuidade **só na REDE** —, agora com a razão certa: **a unidade vendida é o espaço**, e só na rede ela
  coincide com uma pessoa ([ADR-0018 D7](../adr/0018-agregado-passagem-participantes-modo-e-lancamentos.md)).
- **Contagem** (`BalancoPassagensMapper`) estudada (`balanco-passagens-mapper.md`): threading já limpo;
  decisões de contagem tomadas (§7).
- ~~**Agente/agência hoje**: texto livre no bilhete; área de agência do form comentada; `atualizarListaAgente`
  com `runBlocking`; logos no repo mas não usados.~~ **Resolvido pelo Pilar 2** (ADR-0015, P2.3/P2.4): a agência
  do bilhete vem do emissor, `Passagem.agente` morreu, a área manual e o último `runBlocking` do form saíram, e
  a marca da agência entrou no bilhete digital. `funcionarioId = uid` congela desde o ADR-0010.
- **Insight estrutural** (`viagem-vs-trecho.md`): a Viagem de hoje é o Trecho. **Entrou no MVP** pelo
  [ADR-0016](../adr/0016-dominio-da-plataforma.md) §7 — e o desenho final tem **três** conceitos, não dois:
  **Rota** (o onde, sem dono), **Viagem** (o quando e em quê — `(rota, navio, diaSemana, hora)`, atômica) e a
  **ocorrência** `(viagemId, data)`. **O Trecho foi dissolvido** na 7ª rodada: o par de cidades é derivável
  dos portos. A versão pesada da nota (ocorrências persistidas com contador por acomodação) segue fora.

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

**Pré-requisito de dado — ✅ RESPONDIDO, e a resposta mudou duas vezes.** A pergunta era se o MVP **semeia
tarifas** (o seed não as populava, então a contagem/emissão dependia de viagem criada à mão). Primeiro o
[ADR-0016](../adr/0016-dominio-da-plataforma.md) §1 matou o seed e pôs a tarifa no cadastro da rota; depois a
9ª rodada (§7.2) **eliminou a pergunta**: não há tarifa a cadastrar nem a semear — o dado é o **valor
informado** na emissão. O que a instalação nova precisa é do universo compartilhado (rotas e viagens), que a
agência nova **encontra montado** no primeiro acesso. Em troca, todo dado tem autor.

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

`Catalogo` → matar o seed → painel → **capacidades da plataforma** (localidade/porto) → **parte e atuação**
(empresa + `atuacoes` + concessão de **portos e navios**) → funcionário multi-empresa → **rota e viagem** +
tipo de embarcação → regras e suíte. É a maior parte do pilar, e é **domínio**, não release.

> **Atualizado em 2026-08-01:** onde este roadmap dizia *trecho*, leia **localidade/porto** — o Trecho foi
> dissolvido na 7ª rodada. A concessão é por **navio** (`navioIds`), não por armador. E a "rota" desta linha
> virou **duas** entidades, `rotas` + `viagens` (§7.1), ambas capacidades compartilhadas **sem dono**.

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

## Os dois eixos transversais

Decididos depois dos três pilares, e sem pilar próprio porque **não são etapa — são mudança de regime**. Os
dois atravessam o Pilar 3 e se encontram nele.

### Eixo de storage — [ADR-0017](../adr/0017-eixo-de-storage-firestore-only.md) (F1–F6)

O Room deixa de ser datasource: a fonte reativa vira `StateFlow` por coleção, a escrita vai direto ao
documento e o offline passa a ser o cache do SDK, declarado. **F1 (piloto `Catalogo`) é a mesma fatia da F1
do ADR-0016** — foi assim que este eixo **destravou** o domínio da plataforma: dois pontos abertos de lá
deixaram de existir. Depois: F2 resíduo local (rascunho → DataStore, bilhete → galeria), F3 cadastros, F4
viagem, F5 passagem, F6 remover o Room.

### Eixo do agregado — [ADR-0018](0018-agregado-passagem-participantes-modo-e-lancamentos.md) (F1–F7)

A Passagem reescrita: participantes com identidade (pools `Cliente` e `Veiculo`), **modo** tipado, capacidade
vinda do navio, numeração por ocorrência, pagamento como **lançamentos**, cancelamento como **estado**.
Ordem: F1 tipos e regras puras · F1b cancelamento · F2/F3 os pools · F4 pagamento e carimbos · F5 ocorrência,
numeração e capacidade · F6 forma do documento · F7 a emissão por etapas.

**A dependência entre os dois é de ordem, não de conteúdo:** enquanto o Room existir, cada campo novo na
`Passagem` é DDL — então **F4 e F6 do ADR-0018 entram depois (ou junto) da F5 do ADR-0017**. As coleções
novas (F2/F3) não têm esse problema: nascem já no regime Firestore-only.

## Sequência e dependências (proposta)

```
Pilar 1 ✅ (P1.1 → P1.2 → P1.3 → P1.4)
Pilar 2 ✅ (ADR-0015: P2.0 → P2.1 → P2.2a′ → P2.2b → P2.2c → P2.3 → P2.4 → P2.6)
Pilar 3   P3.A (ADR-0016: F1 → F2 → F3 → F4 → F5 → F6 → F7, com F8 acompanhando)
          P3.B (P3.4 → P3.1/P3.2/P3.3 → P3.5)      ← P3.4 pode ir já; não depende de nada

eixos     storage  (ADR-0017: F1 = a F1 do ADR-0016 → F2 → F3 → F4 → F5 → F6)
          agregado (ADR-0018: F1 · F1b → F2 → F3 → F4* → F5 → F6* → F7)   * depois da F5 do storage
```

Ordem sugerida: **P3.4 primeiro** (gate das regras: valor alto, zero credencial, independente de tudo), depois
o **piloto do storage, que é a F1 do domínio** — uma fatia que paga dois eixos —, seguindo por **P3.A**, com
as fatias baratas do agregado (F1 e F1b) encaixadas quando convier: são puras, isoladas e independentes. A
distribuição por último — não faz sentido distribuir antes de haver painel que alimente o app.

> **F1b tem urgência própria:** enquanto o cancelamento continuar sendo *delete* físico, **cada cancelamento
> apaga histórico** que o ADR-0018 D17 declarou prioridade.

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

**Pilar 3 — domínio (P3.A):** ~~o que trava o começo é o rename `Constante`→`Catalogo` tocando o Room~~ —
**não trava mais**: sem espelho, não há entidade a renomear ([ADR-0017](../adr/0017-eixo-de-storage-firestore-only.md)).
Os dez pontos abertos do [ADR-0016](../adr/0016-dominio-da-plataforma.md#pontos-abertos-analista-decide)
**estão todos resolvidos** desde 2026-08-01 — o último foi o 6: **o `SUPERVISOR` cria rota e viagem** (criar
no pool comum afeta todas as agências) **e a concessão é editável** depois do cadastro. O domínio do Pilar 3
não tem mais pergunta pendente; o que falta é execução.

**Fora dos pilares, esperando decisão:** o **método da inferência tarifária** (janela, mínimo de bilhetes,
viagem sem histórico), situado no **módulo faturamento**; e **DTO por entidade × por caso de uso**, que saiu
dos pontos abertos do ADR-0016 para estudo e ADR próprios. O índice de vigência dos ADRs
([`docs/adr/README.md`](../adr/README.md)) mantém essa lista.

---

> O rework "Viagem vira Trecho" (`viagem-vs-trecho.md`) **entrou no MVP** pelo
> [ADR-0016](../adr/0016-dominio-da-plataforma.md) §7 — e chegou mais longe que a nota previa: **o Trecho foi
> dissolvido** e o que existe é Rota + Viagem + ocorrência (§7.1). A parte pesada (ocorrências persistidas +
> contador por acomodação) segue fora — mas a contagem deixou de ser só relatório: com a capacidade no navio
> ([ADR-0018 D8](../adr/0018-agregado-passagem-participantes-modo-e-lancamentos.md)), ela **barra a emissão**
> quando a ocorrência lota.