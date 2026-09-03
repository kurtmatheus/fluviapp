# As telas antes da revitalização — login, painel e o form de passagem

> **Documento de histórico.** Não tem autoridade sobre nada: sintetiza **três estudos de apresentação** que
> descrevem telas que já não existem na forma descrita. Existe para preservar os achados sem manter em
> circulação texto que **documenta capacidades removidas** — que é pior que texto velho, porque parece
> instrução.
>
> **Absorve, e substitui:**
>
> | Estudo | Era | O que o tornou história |
> |---|---|---|
> | `fluxo-login.md` | 2026-07 | [ADR-0015](../adr/0015-rework-agente-equipe.md) §2.1 — o provisionamento fechou |
> | `fluxo-main-screen.md` | 2026-07 | a revitalização (ADR-0020) e o painel do [ADR-0022](../adr/0022-painel-da-empresa-e-fases.md) |
> | `form-passagem-validacao-exibicao.md` | 2026-07 | a **F9 inteira** ([ADR-0027](../adr/0027-faseamento-da-f9.md)) |
>
> Para a camada de apresentação **como ela está**, ver
> [`camada-de-apresentacao.md`](../design/camada-de-apresentacao.md).

---

## 1. O login que documentava o que hoje não existe

O estudo descrevia **quatro capacidades em produção**: login por e-mail/senha com gate de e-mail
verificado, **cadastro com auto-provisionamento de perfil**, recuperação de senha e **login com Google**
via Credential Manager.

**Três das quatro foram removidas por decisão** (ADR-0015 §2.1): o autocadastro, o login com Google e o
gate de e-mail verificado saíram quando o provisionamento fechou. Quem entra passou a entrar por
**convite** — um documento `convites/{email}` que só o `ADM` escreve, e contra o qual a regra do servidor
confere o `users/{uid}` no primeiro acesso.

É por isso que este estudo é o mais perigoso dos três: ele não estava *velho*, estava **errado sobre o
presente**, e num tom de documentação de fluxo implementado.

**O que sobrevive dele:** a arquitetura da porta de autenticação por DIP e a decisão de **não guardar senha
local** — as duas no [ADR-0005](../adr/0005-autenticacao-sessao-firebase-datastore.md), que segue vigente. E
a lição de método: *documentação de fluxo implementado envelhece pior que estudo de design*, porque ninguém
desconfia dela.

## 2. O painel que foi proposto, e o que de fato aconteceu

O estudo propunha uma remodelação: **bottom bar reduzida a Início + Menu** e um **drawer à direita,
arrastável**, concentrando as seções Passagem, Viagem, Agente e Empresa.

Quase nada disso é o painel de hoje:

| O que o estudo propunha | O que existe |
|---|---|
| drawer à **direita** | drawer à **esquerda** (modal no celular, permanente no tablet) |
| bottom bar com **Início + Menu** | bottom bar com **um item só, o embarque** — Início e Menu perderam o sentido em 2026-09-03 |
| seções Passagem, Viagem, **Agente**, Empresa | nove seções derivadas da **atuação**, e `Agente` virou `Funcionario` |
| gate por cargo "proposto" | política única com três coordenadas — `(papel, atuação, cargo)` |

**O que sobrevive:** a intuição que estava certa — *concentrar navegação no menu lateral e deixar a barra
inferior para a ação de rotina*. Ela levou dois meses e três ADRs para chegar à forma atual, e a forma atual
é mais radical que a proposta: a barra não navega, ela é o **pedestal do FAB de embarque**.

## 3. O form de passagem anterior ao molde

Este era o mais útil dos três, e o único que era **diagnóstico** e não documentação. Ele mediu o form de
emissão contra o [molde de cadastro](../adr/0006-molde-de-cadastro.md) e listou os desvios: validação
impura e monotônica, lambdas dentro do `UiState`, `runBlocking` no caminho da tela.

Junto, os achados de regra — bugs que ninguém tinha visto porque estavam escondidos na validação:

- a **agência era ignorada**;
- a **cilindrada não era validada**, e a mensagem de erro enganava sobre o motivo;
- havia validação **cruzada** de passageiro 3 contra passageiro 1;
- um *check* de cor era código morto;
- e a idade de gratuidade era um **6 mágico** no meio do validador.

**Todos foram resolvidos, mas não por refatoração deste form** — a F9 o demoliu e reconstruiu. A validação
virou pura; o `UiState` ficou sem lambdas; o `runBlocking` saiu; a idade mágica virou tipo
(`TipoGratuidade`); e a regra que o validador espalhava **subiu para o tipo** — quem sabe se exige modelo é
a classe, não o formulário (ADR-0023 D4).

**A direção que o estudo propôs — *validação pura primeiro* — foi seguida à risca**, e é a primeira fatia da
F9.1. Nesse sentido ele não foi superado por estar errado: foi superado por ter sido **executado até o fim**.

## 4. O que os três ensinam juntos

**Estudo de design envelhece com dignidade; documentação de tela, não.** Os três foram escritos no mesmo
mês. O que era diagnóstico (§3) virou plano e foi cumprido; o que era proposta (§2) foi superado por algo
melhor e ainda assim deixou a intuição certa; e o que era *documentação do que existe* (§1) virou a única
coisa perigosa do conjunto — porque descrevia, com segurança, três capacidades que foram deliberadamente
removidas.

A consequência prática, e que vale como regra: **documentar uma tela é assumir a manutenção dela**. Onde não
houver essa disposição, é melhor documentar a **decisão** — que não muda sem que alguém escreva outro ADR.
