# Roadmap para o MVP

**Status:** Pilares **1 e 2 completos** (2026-07-27); resta o **3** (CI/CD). Não é código; é o
sequenciamento dos passos até o MVP e as sementes de ADR que faltam.

O MVP tem **três pilares** (na ordem proposta pelo analista):

1. **Contagem de Passagem** — melhorias no balanço como ele é hoje, **sem faturamento** ainda.
2. **Rework de Agente → Equipe** — agência/lotação viram capacidades do usuário; a agência vira capacidade
   transversal à emissão, com identidade visual por agência (multi-agência).
3. **CI/CD via Firebase** — distribuição de builds aos usuários cadastrados.

---

## Estado atual (a base já pronta)

- Form de passagem **no molde ADR-0006**: validação pura, `runBlocking`→suspenso, UiState puro, UX
  fail-closed (banner). Emissão governada pelo modelo de preço tabelado do **ADR-0013**.
- **ADR-0013 fechado + emenda**: tipo tarifário (meia/gratuidade) **só na REDE**; suíte/camarote/veículo
  sempre inteira.
- **Contagem** (`BalancoPassagensMapper`) estudada (`balanco-passagens-mapper.md`): threading já limpo;
  decisões de contagem tomadas (§7).
- **Agente/agência hoje**: texto livre no bilhete; área de agência do form **comentada**; `onAgenciaChange`/
  `onAgenteChange` já existem no VM (não plugados); `atualizarListaAgente` com `runBlocking` (dívida).
  `funcionarioId = uid` do emissor já congela (ADR-0010). Logos `logo1/logo2.png` já no repo (branding por
  agência, ainda não usados).
- **Insight estrutural** (`viagem-vs-trecho.md`): a Viagem de hoje é o Trecho — rework grande, **fora do
  MVP** (não bloqueia nenhum pilar; anotado para depois).

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

**Pré-requisito de dado, ainda aberto:** o SEED não popula tarifas; para a contagem/emissão funcionar numa
viagem, a tarifa precisa estar cadastrada (viagem criada manual). Decidir se o MVP **semeia tarifas** ou
mantém manual. É a única pergunta viva do Pilar 1 — e vale notar que ela decide se o app **demonstra** sem
intervenção num projeto Firebase novo.

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

## Pilar 3 — CI/CD via Firebase (distribuição a usuários cadastrados)

Distribuir builds aos usuários via **Firebase App Distribution**. Passos prováveis (a confirmar):

- **P3.1 — Pipeline de build.** CI (provável GitHub Actions) que builda o app assinado (keystore em secrets).
- **P3.2 — Distribuição.** `firebase appdistribution:distribute` (service account) publicando o artefato a
  um **grupo de testers**. Gatilho: por tag/release ou push na branch.
- **P3.3 — Audiência = usuários cadastrados.** Decidir como o "grupo de testers" se relaciona aos usuários
  cadastrados no app (manual no console vs. sincronizado dos `users` do Firestore).

*Pilar largamente **independente** dos 1/2 — pode ser puxado para antes se você quiser começar a distribuir
cedo. É DevOps/release, não domínio.*

## Sequência e dependências (proposta)

```
Pilar 1 ✅ (P1.1 → P1.2 → P1.3 → P1.4)
Pilar 2 ✅ (ADR-0015: P2.0 → P2.1 → P2.2a′ → P2.2b → P2.2c → P2.3 → P2.4 → P2.6)
Pilar 3 (P3.1 → P3.2 → P3.3)                       ← independente, encaixa quando quiser
```

Ordem sugerida para o MVP: **P1 inteiro → semear ADR-0015 → P2 → P3** (ou P3 em paralelo assim que houver
build estável). P2.1 (rótulo Equipe) pode ir junto com o P1 por ser barato.

## Perguntas abertas (semear os ADRs)

**Pilar 1:** MVP **semeia tarifas** (para a contagem ter dado) ou mantém viagem/tarifa manual? (P1.4
entrou no MVP — feito.)

**Pilar 2:** nada aberto — todas as perguntas que estavam aqui (lotação, override de agência, capability,
logo por agência, isolamento, destino do `Agente`, recorte da contagem) foram respondidas pelo analista e
estão registradas no [ADR-0015](../adr/0015-rework-agente-equipe.md) (*Decisões resolvidas*, 1ª e 2ª rodada).

**Pilar 3:**
- Provedor de CI (GitHub Actions?) e onde ficam os secrets (keystore, service account)?
- Artefato: APK (App Distribution) — AAB fica para a Play depois?
- Grupo de testers manual no console ou sincronizado dos usuários cadastrados?

---

> O rework "Viagem vira Trecho" (`viagem-vs-trecho.md`) **não entra no MVP** — é ortogonal e maior; fica
> anotado para depois de o multi-agência (Pilar 2) assentar.