# Estudo — o painel administrativo e os usuários da plataforma

> **Estudo, não decisão.** Mapeia o código como ele está, expõe as tensões e prepara o ADR. Onde este
> documento e um ADR discordarem, o ADR vence.
>
> Origem: decisão do analista em 2026-08-04 — *"ADM significa o único com acesso ao painel administrativo
> e o único com acesso a todas as informações; nenhum gestor pode cadastrar ou ver usuários; nenhum
> operador vê essa opção no menu"*. E o recorte: **GESTOR mantém o resto do painel** (opção (i)).

## 1. O que se quer

Uma seção **Usuários**, visível só para `ADM`, que responda *quem tem acesso ao aplicativo*. Hoje essa
pergunta só é respondida abrindo o console do Firebase.

O nome importa e foi decidido: **não é "equipe"**. Equipe é o coletivo dos `Funcionario` de uma agência —
gente com cargo, lotação e operação. Quem administra a plataforma não tem nada disso: é **usuário**, e o
modelo já separava os dois desde o ADR-0015 §8.1. Chamar de equipe seria trazer vocabulário de agência
para dentro da plataforma.

## 2. O que existe hoje, e funciona

Esta seção existe porque o rework pedido atravessa o caminho de entrada do app — e o caminho de entrada
**está funcionando desde 2026-08-04**, depois de uma sessão inteira consertando-o. Nada aqui deve ser
refeito por gosto.

### 2.1 Os dois contextos (ADR-0015 §8.1)

| | `Usuario` | `Funcionario` |
|---|---|---|
| responde | o que compete **no aplicativo** | o que a pessoa faz **na operação** |
| eixo | SISTEMA (`papel`) | NEGÓCIO (`cargo`) |
| coleção | `users/{uid}` — o id **é** o uid do Auth | `funcionarios/{id}` |
| campos | `email`, `username`, `papel`, `funcionarioId` | nome, agência, lotação, cargo, e-mail |

`ContextoUsuario` (`domain/operacoes/ContextoUsuario.kt`) junta os dois e é a porta que responde *"quem
está operando"*. Papel puro de plataforma tem `funcionario == null`, e isso é **estado válido**, não falta
de dado: `ADM`/`GESTOR` não atuam num segmento.

### 2.2 O caminho de entrada, como está

```
LoginScreen → autenticar() → perfilAutenticado()  ─── Encontrado ──→ registrarLogin() → DataStore → Main
                                    │                                      (Room local)
                                    ├── Ausente ──→ deduzirPrimeiroAcesso(email)
                                    └── Indisponivel ──→ "sem conexão" (NÃO desloga)
```

Três pontos que custaram caro e não podem regredir:

- **o perfil vem do servidor** (`get(Source.SERVER)`), nunca do cache: decidir papel a partir do que
  sobrou da última sessão é decidir permissão por memória;
- **`ResultadoPerfil` tem três estados** — `Encontrado`/`Ausente`/`Indisponivel`. Fundir os dois últimos
  fazia o app acusar a pessoa de não ser da casa quando o problema era rede;
- **o login é a origem do espelho local**, não seu consumidor. O `UsuarioRepository` perdeu o Firestore e
  ficou local puro; quem lê depois é a `SessaoUsuario` (e a splash, por ela).

### 2.3 O que o servidor permite em `users/{uid}` (regras publicadas em 2026-08-04)

| Ação | Quem | Observação |
|---|---|---|
| ler | **qualquer autenticado** | ⚠️ ver §4.1 |
| criar | só o próprio dono, como `OPERADOR`, com `funcionarioId` apontando a um funcionário **de mesmo e-mail** | é o primeiro acesso |
| editar | só o próprio dono; `papel` e `funcionarioId` **imutáveis** | anti-escalonamento |
| apagar | ninguém (`allow delete: if false`) | — |

**O ADM não pode criar, promover nem remover ninguém pelo app.** Não é lacuna: é a proteção mais sensível
do sistema, e qualquer seção de usuários precisa conversar com ela em vez de contorná-la.

## 3. O que a seção exige do que existe

### 3.1 `ADM` e `GESTOR` nunca se separaram

A política tem um predicado só — `ehPapelPlataforma()` (`domain/operacoes/PermissoesUsuario.kt`) —, e ele
funde os dois papéis em **toda** decisão de menu e em toda regra de escrita do servidor. A seção Usuários
é o **primeiro lugar onde eles divergem**, o que exige um `ehAdm()` novo dos dois lados (Kotlin e
`firestore.rules`), sem afrouxar o que hoje é decidido por `ehPapelPlataforma`.

Consequência de vocabulário: "papel de plataforma" deixa de ser sinônimo de "pode tudo". Vale revisar o
KDoc que hoje descreve `ADM`/`GESTOR` como equivalentes.

### 3.2 O `UsuarioRepository` não lê o servidor

Ele virou **local puro** em 2026-08-04, e de propósito: era o espelho da coleção inteira, disparado antes
do login, que a regra `allow read: if autenticado()` nega. Listar todos os usuários exige uma fonte de
servidor nova — um `UsuarioFirestoreRepository` no molde da Empresa (`FonteSnapshots` + `StateFlow`, sem
Room, ADR-0017 D1). É trabalho previsto, não retrabalho: o que morreu foi a leitura **pré-login**.

### 3.3 O `Usuario` não sabe ser desligado

Não há campo de estado. "Remover acesso" hoje só poderia ser `delete`, e apagar `users/{uid}` deixa a
conta **órfã no Auth**: a pessoa autentica e não entra — exatamente a falha que travou o acesso do
analista em 2026-08-03, com uma mensagem que culpa o usuário. Se a seção tiver desligamento, o caminho
provável é um campo (`ativo`) que o login confira, e não a remoção do documento.

## 4. Achados

### 4.1 A leitura de `users` é ampla

`allow read: if autenticado()` deixa **qualquer operador ler o perfil de todo mundo** — e-mail, papel e
vínculo. Com a seção sendo `ADM`-only, fazer o gate só na UI repetiria o que o ADR-0010 já nomeou: *gate
de UI é UX, não fronteira*.

O aperto natural é `ehAdm() || request.auth.uid == uid`. **Verificado:** não quebra nada — depois que o
espelho da coleção saiu, o único leitor é o `perfilAutenticado()`, que lê o próprio documento. As regras
internas (`get(users/$(uid))` dentro de `firestore.rules`) não passam por `allow read`.

### 4.2 Não há caminho in-app para a primeira conta — nem para a segunda

O autocadastro saiu na P2.2c e o `SeedFirestore` morreu em 2026-08-03. Hoje **cada pessoa** exige dois
passos manuais no console: criar a conta no Authentication e criar o `users/{uid}` com o papel. É o que
impede o app de ter mais de um usuário, e é o gargalo real que a seção deveria atacar.

O SDK cliente não cria conta de terceiro. Três saídas:

| Saída | Custo | Observação |
|---|---|---|
| **link de e-mail** (`sendSignInLinkToEmail`) | nenhuma infra nova | a conta nasce quando a pessoa clica; **não** reabre o autocadastro, porque quem dispara é o painel |
| **Cloud Function** com Admin SDK | back-end | é o "back-end próprio" que o ADR-0017 previa para depois |
| **manual no console** | zero código | aceitável enquanto o tester for um |

> **A armadilha do primeiro acesso.** O `deduzirPrimeiroAcesso` só funciona para quem **tem funcionário**
> com o mesmo e-mail — é o pré-cadastro que autoriza o vínculo. Um `ADM`/`GESTOR` novo é papel puro, não
> tem funcionário, e portanto **não consegue nascer pelo app** por nenhuma das saídas acima sem regra
> nova. Provisionar operador e provisionar administrador são problemas diferentes.

### 4.3 O uid é por projeto

Um `users/{uid}` trazido de outro projeto Firebase não casa: o mesmo e-mail recriado ganha uid novo.
Custou a maior parte de uma sessão de diagnóstico. Qualquer tela que exiba ou edite usuários deve mostrar
o **uid**, porque é ele que liga o documento à conta.

### 4.4 O operador cai num painel vazio

Com o recorte da revitalização (ADR-0020), `secoesDoMenu(operador)` devolve lista vazia — a Empresa é
seção de plataforma. O analista mencionou que *"operadores já serão redirecionados direto para painel
empresa"*: isso é destino de navegação por papel, decidido na splash
(`ui/viewmodel/SplashScreenViewModel.kt`, que já resolve contexto antes de entrar). **Não faz parte desta
seção**, mas divide com ela a mesma pergunta — *para onde cada papel vai ao entrar*.

## 5. O que precisa ser decidido

1. **Nome e ordem da seção.** `USUARIOS` no `SecaoMenu` — "Usuários"? "Acessos"? E onde entra na ordem, que
   hoje é também a ordem de dependência do cadastro (Empresa primeiro, ADR-0020 D10).
2. **Até onde vai a escrita.** Só leitura (cabe hoje, sem tocar em regra) × promover/rebaixar (regra nova
   com anti-autoescalonamento: um ADM pode rebaixar outro ADM?) × convidar (§4.2) × desligar (§3.3).
3. **Apertar a leitura de `users`** (§4.1) — recomendado junto com a seção, para UI e servidor concordarem
   desde o primeiro commit. O ADR-0016 F8 já avisa: *regra escrita depois é regra que passou um tempo
   aberta*.
4. **O que "acesso a todas as informações" significa** além do menu. Hoje não há recorte de leitura por
   papel em nenhuma coleção — todo autenticado lê tudo. Se o ADM é o único com acesso total, isso implica
   recortes que ainda não existem, e vale saber se é alvo ou só ênfase.
5. **Provisionamento** (§4.2), com o caso do administrador separado do caso do operador.

## 6. Ordem sugerida

Pela régua da revitalização (ADR-0020) — domínio isolado e testado, depois dados, lógica e apresentação,
com teste em cada camada e a seção entrando em `SECOES_REVITALIZADAS`:

1. **domínio** — `ehAdm()`, `SecaoMenu.USUARIOS`, `AcaoMenu`; tudo JVM puro, sem Firestore;
2. **regras + suíte** — a leitura restrita, com os casos de emulador (§4.1);
3. **dados** — `UsuarioFirestoreRepository` no molde novo (§3.2);
4. **lógica** — a busca, observando o repositório;
5. **apresentação** — lista com papel e uid visíveis, teste instrumentado.

A escrita (item 2 do §5) fica para uma segunda rodada, depois que a leitura provar o caminho — e porque
cada forma de escrita arrasta uma decisão de segurança própria.