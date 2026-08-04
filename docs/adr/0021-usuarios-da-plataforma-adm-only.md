# ADR-0021: Usuários da plataforma — seção `ADM`-only, somente leitura

**Status:** Aceita (decisão do analista em 2026-08-04)

**Estudo que preparou:** [`docs/design/painel-administrativo.md`](../design/painel-administrativo.md)

---

## Contexto

O painel administrativo não responde *quem tem acesso ao aplicativo* — a pergunta só se responde abrindo
o console do Firebase. Isso passou a doer quando a esteira de distribuição ficou de pé (2026-08-04): cada
tester novo exige dois passos manuais no console, e não há de onde conferir o resultado.

A pergunta não é sobre **equipe**. Equipe é o coletivo dos `Funcionario` de uma agência — gente com cargo,
lotação e operação. Quem administra a plataforma não tem nada disso: é **usuário**, e os dois contextos já
estavam separados desde o ADR-0015 §8.1. Trazer "equipe" para a plataforma seria importar vocabulário de
agência para onde ele não vale.

Três fatos do código restringem o que é possível hoje:

1. a política tem **um predicado só** para plataforma (`ehPapelPlataforma()`), que funde `ADM` e `GESTOR`
   em toda decisão de menu e em toda regra de escrita;
2. `users/{uid}` é **legível por qualquer autenticado**, e escrevível **só pelo próprio dono**, com
   `papel` e `funcionarioId` imutáveis — o anti-escalonamento fechado na P2.2a′;
3. o `UsuarioRepository` virou **local puro** em 2026-08-04, quando o espelho pré-login da coleção foi
   removido por ser negado pelas regras.

## Decisão

### D1 — Nasce a seção `USUARIOS`, e ela é `ADM`-only

`ADM` é o único papel com acesso a esta seção. `GESTOR` **mantém o resto do painel** (Empresa, Navio,
Viagem), como hoje; `OPERADOR` não a vê porque não tem papel para ela — não porque esteja escondida.

Isso obriga um predicado novo, `ehAdm()`, ao lado do `ehPapelPlataforma()` existente. **É a primeira
decisão do sistema em que os dois papéis de plataforma divergem**, e por isso "papel de plataforma" deixa
de ser sinônimo de "pode tudo".

*Opção descartada:* estender `ehPapelPlataforma()` à seção. Custa a distinção que o analista pediu, e
tornaria impossível ter administração que o gestor não veja.

### D2 — Somente leitura, nesta rodada

A seção lista **todos** os usuários — não só os de plataforma —, com papel e uid visíveis. Não cadastra,
não promove, não desliga.

O motivo não é preguiça de escopo: **cada forma de escrita arrasta uma decisão de segurança própria**, e
nenhuma delas é obrigatória para responder a pergunta que originou a seção.

- **promover/rebaixar** exige afrouxar o anti-escalonamento e responder se um `ADM` pode rebaixar outro;
- **convidar** esbarra no Auth — o SDK cliente não cria conta de terceiro, e o primeiro acesso só se deduz
  para quem tem `Funcionario` de mesmo e-mail (o que **papel puro de plataforma nunca tem**);
- **desligar** não pode ser `delete`: apagar `users/{uid}` deixa a conta órfã no Auth, e a pessoa passa a
  autenticar sem entrar — a falha exata que travou o acesso do analista em 2026-08-03.

Ler primeiro também prova o caminho novo de dados (D4) antes de misturar risco de escrita.

### D3 — A leitura de `users` é apertada no servidor, no mesmo trabalho

`allow read` passa de `autenticado()` para **`ehAdm() || request.auth.uid == uid`**.

Fazer o gate só na UI repetiria o que o ADR-0010 já nomeou: *gate de UI é UX, não fronteira*. Uma seção
`ADM`-only cujo dado qualquer operador lê pelo SDK não é `ADM`-only.

**Verificado antes de decidir:** não quebra nada. Depois que o espelho pré-login saiu, o único leitor da
coleção é o `perfilAutenticado()`, que lê **o próprio documento** — coberto pela segunda metade da regra.
As leituras internas das regras (`get(users/$(uid))`) não passam por `allow read`.

A regra e os casos de emulador entram **junto** com a seção, não depois: o ADR-0016 §8 já avisa que regra
escrita depois é regra que passou um tempo aberta, e a sessão de 2026-08-04 mostrou o preço disso — um
servidor permissivo escondeu por semanas uma dependência que quebraria o login de todos.

### D4 — A fonte é um repositório Firestore novo, no molde da Empresa

Nasce um `UsuarioFirestoreRepository` com `FonteSnapshots` + `StateFlow`, **sem Room** (ADR-0017 D1). O
`UsuarioRepository` atual permanece **local e puro**: ele guarda quem entrou, e é o que a `SessaoUsuario`
lê.

Não é retrabalho do que foi removido em 2026-08-04. O que morreu foi a leitura **pré-login** da coleção
inteira, feita para achar um usuário; o que nasce é leitura **pós-login, autenticada e de uma seção que
existe para isso**.

## Consequências

- **O vocabulário muda.** "Papel de plataforma" deixa de significar "acesso total". KDocs que descrevem
  `ADM` e `GESTOR` como equivalentes ficam desatualizados e precisam de revisão.
- **A matriz de autorização cresce um eixo.** A paridade Kotlin ↔ `firestore.rules` (dever declarado no
  ADR-0011) passa a ter dois predicados de plataforma em vez de um; mudar papel exige tocar os dois lados.
- **A seção nasce incompleta de propósito.** Quem abrir esperando cadastrar usuário não vai conseguir, e
  o caminho continua sendo o console. É explícito, não acidental.
- **`users` deixa de ser legível pela operação.** Se algum caso futuro precisar que um operador leia
  perfis alheios (uma lista de responsáveis, por exemplo), a regra terá de abrir de novo — e o caso terá
  de justificar a abertura.
- **Não resolve o gargalo de provisionamento.** Continua exigindo dois passos manuais no console por
  pessoa. A seção torna o problema *visível*, não o elimina.

## Alternativas futuras

Revisitar quando:

- **o provisionamento entrar** — aí a seção ganha escrita, e a decisão entre link de e-mail e Cloud
  Function precisa ser tomada, com o caso do administrador tratado à parte do caso do operador
  (§4.2 do estudo);
- **existir mais de um `ADM`** — "um ADM pode rebaixar outro" deixa de ser hipótese;
- **o `Usuario` ganhar estado** (`ativo`) — o login passa a conferi-lo, e desligar deixa de ser um problema
  sem solução segura;
- **o recorte de leitura por papel virar alvo** — hoje só `users` fica restrito; as demais coleções
  continuam legíveis por qualquer autenticado, e "o ADM é o único com acesso a todas as informações"
  ainda é uma afirmação sobre o menu, não sobre o servidor.