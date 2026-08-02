# Roadmap para o MVP

**Status:** Pilares **1 e 2 completos** (2026-07-27). **Reescrito em 2026-08-02** — o caminho até a entrega
deixou de ser "o Pilar 3" e virou **três frentes de tela**, decididas pelo analista, com um princípio único:
**mínimo para entrega, reaproveitando o que existe; o que não servir, descarta-se e revitaliza-se.**

O MVP teve **três pilares**:

1. ✅ **Contagem de Passagem** — melhorias no balanço, sem faturamento.
2. ⚠️ **Rework de Agente → Equipe** — as fatias do
   [ADR-0015](../adr/0015-rework-agente-equipe.md) foram entregues, mas **o pilar não está completo**: o
   ADR-0016 (8ª rodada) reabriu o modelo de identidade e há folga anotada no próprio código. O que falta
   está listado no [§ Pilar 2](#pilar-2--rework-de-agente--equipe-adr-0015) e **entra dentro das frentes
   E2/E3**, não como frente própria.
3. **Plataforma** — o painel que substitui o seed como porta de entrada do dado, e a distribuição. **É o que
   este roadmap agora detalha**, na forma que o analista definiu: três frentes de tela (E1, E2, E3), com o
   domínio já fechado por baixo.

**O método mudou junto, e é ele que dá a ordem** *(decisão de 2026-08-02)*: **do domínio nascem as
fronteiras e as camadas.** Ao definir uma tela, dela derivam mapper, ViewModel, camada de dados e
apresentação — não o contrário. O Firestore é a camada de dados que **reflete** o domínio; **a fronteira
passa a ser dinâmica (`Map`)** e os DTOs passam a ser **por caso de uso**
([estudo](dto-por-entidade-ou-caso-de-uso.md) §7). É por isso que o painel da plataforma vem **depois** do
domínio revisado: **o painel da plataforma é o molde do painel por agência**, e este é o molde dos painéis
dos outros segmentos.

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

> ### ⚠️ NÃO está completo *(revisto em 2026-08-02)*
>
> As fatias P2.0–P2.6 do ADR-0015 foram entregues, mas **o pilar não fechou** — parte por folga que o
> próprio ADR-0015 deixou anotada, parte porque o **ADR-0016 (8ª rodada) reabriu o modelo de identidade**.
> O que falta, verificado no código em `2026-08-02`:
>
> | Pendência | Estado no código | Origem |
> |---|---|---|
> | **Cargo por vínculo `(empresa, atuação)`** | não existe: `Funcionario.cargo` é **um** `String`; não há `vinculos[]` nem `Atuacao` no projeto | ADR-0016, 8ª rodada — **supera** o ADR-0015 |
> | **Agência por id** | `Agencia` ainda é **enum fixo** (`AUTONOMO`, `MATRIZ`) e `Funcionario.agencia` é **String livre** — o comentário em `Agencia.kt:10-14` admite: *"vira coleção cadastrável quando houver cadastro de agência… fechar essa folga é trabalho do P2.2b"* | folga do próprio ADR-0015 |
> | **Agência do bilhete por id** | `Passagem.agencia` continua sendo só o nome congelado | ADR-0018 D13 |
> | **Lotação** | `Funcionario.Lotacao` é enum de três valores fixos | ADR-0016 §5 (localidade é entidade) |
> | **Escolha do vínculo no login** | não existe — a splash resolve só `currentUser != null` | ADR-0016, 8ª rodada |
> | **`Funcionario` importa `FuncionarioDocumento`** | vazamento domínio → DTO dentro da entidade | ADR-0019 D2 |
>
> **Isto é um retrato, não um backlog.** A tabela mede **o domínio revisado contra o código de hoje** — e
> como a revitalização é **por etapas**, parte destes itens some sozinha quando a etapa chegar, e outros vão
> aparecer que ninguém listou. Não vale persegui-los um a um: **com o domínio e a camada de dados revisados,
> a apresentação vai se moldando**. Serve para saber que o Pilar 2 não fechou — não para virar fila.
>
> O ADR-0015 fixou *quem é a pessoa na operação*; o ADR-0016 disse *em que empresa e em que atuação* ela é
> isso. É por isso que nada aqui tem frente própria: cai dentro de E2/E3 e da P3.A, quando empresa, atuação
> e agência passarem a existir como cadastro.
>
> Segue valendo como decisão, não como pendência: **as strings visíveis não foram renomeadas** — na tela o
> coletivo é "Equipe" e o indivíduo é "Agente" (`btn_novo_agente`).
>
> O [ADR-0015](../adr/0015-rework-agente-equipe.md) é a fonte: ele passou por
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

### As três frentes de entrega *(definidas em 2026-08-02)*

O domínio está fechado — [ADR-0016](../adr/0016-dominio-da-plataforma.md) sem pontos abertos,
[ADR-0017](../adr/0017-eixo-de-storage-firestore-only.md) e
[ADR-0018](../adr/0018-agregado-passagem-participantes-modo-e-lancamentos.md) aceitos. **O que falta é
tela**, e ela vem em três frentes, nesta ordem. Todas com o mesmo crivo: *mínimo para entrega; reaproveitar
o que existe; o que não servir, descartar e revitalizar.*

> **O domínio do login está fechado e estável** — `Usuario` × `Funcionario`, papel × cargo, o elo
> `funcionarioId` (ADR-0015 §8). **A frente E1 não mexe nessa regra**: mexe em como o app **entra**.

#### E1 — O caminho de entrada: `MainActivity` → Splash → Login → Painel

Correções e melhorias **mínimas**, ancoradas no que o código mostra hoje:

| Achado | Onde | Por que corrigir agora |
|---|---|---|
| **`delay(Random.nextLong(300, 1000))`** antes de decidir a rota | `SplashScreenViewModel.kt:33` | espera **artificial** de até 1 s na abertura — é o primeiro contato com o produto |
| **Splash sem identidade** — só um `CircularProgressIndicator` centralizado | `SplashGraphNavigation.kt` | existe `FluviWordmark`; a marca deve aparecer onde o usuário espera |
| **Sem splash de sistema** (`core-splashscreen` / tema) | `MainActivity.kt`, `themes.xml` | há **duas esperas**: a janela em branco do sistema e depois o spinner |
| **`@RequiresApi(S)` com `minSdk 26`** | `MainActivity.kt:26` e no NavHost | herdado do Bluetooth dormente; contamina a Activity inteira |
| **`FluviApp` duplicado** — uma função chama a outra que só executa `content()` | `MainActivity.kt:47-64` | indireção sem função; some sem custo |
| **Permissões de Bluetooth pedidas na entrada do painel** | `MainScreenNavComposable.kt:57` | pede permissão para impressora **dormente**, antes de qualquer uso |
| **Splash resolve só `currentUser != null`** | `SplashScreenViewModel.kt:36` | o ADR-0016 (8ª rodada) põe a **escolha do vínculo no login** — quem entra precisa de contexto, não só de sessão |

**Reaproveita:** o grafo de navegação, o `LoginScreen`, o fluxo de recuperação/primeiro acesso e a sessão
(ADR-0005) — tudo isso serve. **Revitaliza:** a splash (visual e temporização) e a organização da Activity.

#### E2 — `MainScreen` vira **PainelPrincipal da Plataforma**

Correções baseadas no domínio + melhorias mínimas. O que o código pede:

- **A política de menu está dentro da navegação.** `MainScreenNavComposable` carrega 13 callbacks e a função
  `acoesDe(secao)` que decide o que cada seção oferece (`:66-93`) — decisão de domínio morando no grafo. O
  estudo da [camada de apresentação](camada-de-apresentacao.md) já apontava; agora tem consumidor.
- **`SecaoMenu` é um enum fixo de cinco seções** (`PASSAGEM, VIAGEM, EQUIPE, EMPRESA, NAVIO`). O ADR-0016 §2
  diz que as seções **derivam da atuação** — é isso que faz a plataforma ser multi-segmento sem tocar o
  modelo de permissão.
- **`DadosBotoesMenus` carrega `onClick`** — DTO com comportamento dentro de `screendata`, exatamente o que
  a decisão de DTO por caso de uso (§7 do [estudo](dto-por-entidade-ou-caso-de-uso.md)) desfaz.
- **O painel é o molde.** Este é o ponto do analista: *o painel da plataforma é como o painel por agência vai
  nascer*, e ele já deixa as bases para os painéis dos outros segmentos. Portanto **a estrutura importa mais
  que o conteúdo** desta frente.

#### E3 — Menu da Plataforma e as opções, a partir do domínio

Definir a **base estrutural** que as demais entidades vão reusar — e provar essa base numa entidade só:
**`Catalogo` primeiro, e como *última* opção do menu** (decisão do analista: é a base que sustenta as
outras, e a que o operador menos abre).

Isso encaixa com o que já estava decidido em outros ADRs, sem contradição:
- é a **F1 do ADR-0016** (`Constante` → `Catalogo`) e, ao mesmo tempo, a **F1 do ADR-0017** (piloto do
  Firestore-only) — uma fatia que paga dois eixos;
- é o primeiro **CRUD só do `ADM`**, com regra no servidor no mesmo incremento (ADR-0011);
- e é o primeiro caso de **DTO por caso de uso + fronteira `Map`**, no lugar mais barato do app.

### P3.A — Domínio e painel ([ADR-0016](../adr/0016-dominio-da-plataforma.md), fases F1–F8)

*O plano por fases do ADR-0016 continua válido como sequência de domínio; as frentes E1–E3 acima são a
**ordem de entrega em tela**, e a F1 de lá é a E3 daqui.*

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

### Eixo do agregado — [ADR-0018](../adr/0018-agregado-passagem-participantes-modo-e-lancamentos.md) (F1–F7)

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
Pilar 2 ⚠️ (ADR-0015 entregue; o que falta — vínculo, agência por id, lotação — entra em E2/E3+P3.A)

ENTREGA   E1 entrada  (Activity → splash → login → painel)
          E2 painel   (MainScreen vira PainelPrincipal; menu deriva da atuação)
          E3 menu + Catálogo  = F1 do ADR-0016 = F1 do ADR-0017 = 1º DTO por caso de uso

domínio   P3.A (ADR-0016: F2 → F3 → … → F8, seguindo a E3)
esteira   P3.B (P3.4 → P3.1/P3.2/P3.3 → P3.5)      ← P3.4 pode ir já; não depende de nada
eixos     storage  (ADR-0017: F1 = E3 → F2 → F3 → F4 → F5 → F6)
          agregado (ADR-0018: F1 · F1b → F2 → F3 → F4* → F5 → F6* → F7)   * depois da F5 do storage
```

**A ordem é E1 → E2 → E3**, e ela não é arbitrária: E1 é o que o usuário vê primeiro e é barato; E2 define a
**estrutura** que E3 vai instanciar; e E3 é a primeira entidade a nascer pelo método novo — domínio →
fronteiras → camadas. Fora da fila, dois itens que não dependem de nada e podem entrar quando convier:
**P3.4** (gate das regras no emulador: valor alto, zero credencial) e as fatias puras do agregado (**F1** e
**F1b**, esta com urgência própria — enquanto não existir, cada cancelamento apaga histórico).

A distribuição fica por último: não faz sentido distribuir antes de haver painel que alimente o app.

> **F1b tem urgência própria:** enquanto o cancelamento continuar sendo *delete* físico, **cada cancelamento
> apaga histórico** que o ADR-0018 D17 declarou prioridade.

## Perguntas abertas (semear os ADRs)

> **O que não conta como pergunta aberta:** diferença entre o **domínio revisado** e o **código de hoje**.
> Isso é delta de implementação, e a revitalização por etapas o consome — alguns itens somem, outros
> aparecem. Aqui ficam só as perguntas que **ninguém pode responder olhando o código**.

**Pilar 1:** nada aberto. A pergunta das tarifas foi respondida pelo [ADR-0016](../adr/0016-dominio-da-plataforma.md)
(seed morre; tarifa entra no cadastro da rota).

**Pilar 2:** nenhuma pergunta aberta — as que estavam aqui foram respondidas no
[ADR-0015](../adr/0015-rework-agente-equipe.md) e no ADR-0016. O que resta é **implementação**, e cai nas
etapas (§Pilar 2).

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

**Fora das frentes, esperando decisão:** o **método da inferência tarifária** (janela, mínimo de bilhetes,
viagem sem histórico), situado no **módulo faturamento**. O **DTO** deixou de estar aberto — foi decidido em
2026-08-02 (**por caso de uso**, com a fronteira de dados em `Map`) e **falta o ADR** que registre a mudança
de regime da camada de dados. O índice de vigência ([`docs/adr/README.md`](../adr/README.md)) mantém a lista.

---

> O rework "Viagem vira Trecho" (`viagem-vs-trecho.md`) **entrou no MVP** pelo
> [ADR-0016](../adr/0016-dominio-da-plataforma.md) §7 — e chegou mais longe que a nota previa: **o Trecho foi
> dissolvido** e o que existe é Rota + Viagem + ocorrência (§7.1). A parte pesada (ocorrências persistidas +
> contador por acomodação) segue fora — mas a contagem deixou de ser só relatório: com a capacidade no navio
> ([ADR-0018 D8](../adr/0018-agregado-passagem-participantes-modo-e-lancamentos.md)), ela **barra a emissão**
> quando a ocorrência lota.